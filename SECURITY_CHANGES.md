# Security & auth fixes applied

This is a summary of everything changed from the original upload, in order of
severity. File paths are relative to `src/main/java/org/fl/flowledger/`.

## Critical correctness bugs

1. **`getCurrentUserId()` always threw `UnauthorizedException`**
   (`auth/service/AuthServiceImpl.java`). The JWT `sub` claim holds the
   user's numeric id, but `getCurrentUserId()` was treating the principal
   name as an email and looking it up with `findByEmail(...)`, which could
   never match. This broke every wallet/transfer/ledger endpoint that
   depends on it. Fixed to parse the id directly.

2. **Double `ROLE_` prefix on login tokens**
   (`auth/service/AuthServiceImpl.java`). Login derived the role claim from
   `authentication.getAuthorities()`, which already carried a `ROLE_`
   prefix; `JwtAuthenticationConverter` then added another `ROLE_`,
   producing authorities like `ROLE_ROLE_USER`. Every `hasRole`/
   `hasAnyRole` check failed for freshly-issued login tokens (refreshed
   tokens were unaffected, which made this inconsistent and hard to spot).
   Fixed by always storing the bare role name (`user.getRole().name()`) in
   the claim, on both login and refresh.

3. **IDOR: `cancelTransfer` had no ownership check**
   (`transfer/service/TransferServiceImpl.java`). Any authenticated user
   could cancel any pending transfer by UUID. Added the same
   sender/receiver ownership check `findTransaction` already used.

4. **IDOR: `GET /api/v1/users/me/{uuid}`**
   (`user/controller/UserController.java`). Returned whatever user the
   supplied UUID pointed to, not the caller. Replaced with a parameterless
   `GET /api/v1/users/me` that derives the user from the authenticated
   principal.

## High-severity gaps

5. **Unhandled auth exceptions returned raw 500s**
   (`common/exception/GlobalExceptionHandler.java`). Added handlers for
   Spring Security's `AuthenticationException` (bad login → clean 401 with
   a generic message, so login failures don't reveal whether the email
   exists), `EmailAlreadyUsedException` (409), `UserNotFoundedException`
   (404), the app's own `BadCredentialsException` (401, used by
   change-password), a missing-cookie case for `/logout` and `/refresh`,
   and a catch-all `Exception` handler that logs server-side and returns a
   generic message instead of leaking internals.

6. **Credentials logged in plaintext.** Removed the `System.out.println`
   calls in `AuthController.login` (raw password) and
   `CustomUserDetailsService` (email + a manual stack trace on every
   login).

7. **JWT signing key regenerated on every restart**
   (`common/config/JwtKeyConfig.java`). Now loads an RSA key pair from
   `app.jwt.private-key` / `app.jwt.public-key` (PEM, via `JWT_PRIVATE_KEY`
   / `JWT_PUBLIC_KEY` env vars) when configured. Falls back to an
   in-memory generated key only when unset, with a loud startup warning —
   that fallback is dev-only and will not work correctly with multiple
   instances or across restarts.

## Medium

8. **Missing `@Valid`** on `/login`, `/register`, and the new
   change-password endpoint so the existing bean-validation annotations
   actually run.
9. **`changePassword` was a stub returning `null`** with no endpoint. Now
   implemented properly (`UserService.changePassword(userId, dto)`,
   wired up at `PATCH /api/v1/users/me/password`), derives the target user
   from the authenticated principal (not a client-supplied email), and
   revokes all of that user's refresh tokens on success so other
   sessions/devices are forced to re-authenticate.
10. **No refresh-token rotation.** `RefreshTokenService` now rotates the
    token on every `/refresh` (old one revoked, new one issued) and
    supports `revokeAll(userId)` for the password-change case above.
11. **Cookie inconsistencies** — login/refresh/logout now share one
    `setRefreshCookie`/`clearRefreshCookie` helper in `AuthController`, and
    `secure` is driven by `app.cookie.secure` (`true` by default, `false`
    only in the `dev` profile) instead of being hardcoded differently in
    two places. Removed the duplicated `Set-Cookie` header on login.
12. **No CORS configuration** — added a `CorsConfigurationSource` bean in
    `SecurityConfig`, driven by `app.cors.allowed-origins`.
13. **Password strength** — `CreateUserDto.password` and
    `ChangePasswordDto.newPassword` now require a minimum length (8).
14. **No issuer validation / no clock-skew tolerance**
    (`common/security/JwtService.java`, `common/config/JwtKeyConfig.java`).
    Tokens now carry an `iss` claim (`JwtService.ISSUER`), and
    `jwtDecoder()` validates it via `JwtIssuerValidator`, chained with a
    `JwtTimestampValidator` allowing 30s of clock skew. Note: the issuer is
    intentionally a well-formed URL string (`https://flow-ledger.internal`)
    rather than a bare word — Spring Security parses `iss` as a
    `java.net.URL` internally, and a non-URL string there breaks every
    token decode.
15. **Wallet creation was unusable and, as written, let a user create/delete
    a wallet for someone else's account**
    (`wallet/service/WalletServiceImpl.java`). `CreateWalletDto`/
    `DeleteWalletDto` took a separate "target user" UUID from the client,
    and the check `if (Objects.equals(requesterId, targetId)) throw ...`
    was inverted: it rejected the normal case (creating your own wallet,
    requester == target) and silently allowed the dangerous case
    (requester != target, i.e. acting on someone else's account). Fixed by
    removing the client-supplied user id entirely — wallet create/delete is
    now always scoped to the authenticated caller, matching the pattern
    used for `/users/me` and transfer ownership checks.
16. **`existsByUserIdAndCurrency` had a parameter type mismatch**
    (`wallet/repository/WalletRepository.java`). The method name derives a
    query on `user.id`, which is a `Long`, but the parameter was declared
    as `UUID` — this would fail at query execution. Fixed the parameter
    type to `Long` and updated the call site to pass `user.getId()`.

## Config

- `application.yml` split into `dev`/`prod` Spring profiles (multi-document
  YAML). `dev` keeps the previous hardcoded defaults matching
  `docker-compose.yml`; `prod` requires `DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`, `REDIS_HOST`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` to be set
  as environment variables, with no fallback.
- Actuator now explicitly exposes only `health` and `info`.
- Security/web logging dropped from `TRACE`/`DEBUG` to `WARN` in `prod`.
- Added `.env.example` documenting every environment variable the app now
  reads.

## Not done (flagged, not fixed)

These were called out in review as good next steps but are larger/riskier
changes intentionally left out of this pass so as not to bundle unrelated
behavioral changes into a security-fix pass:

- Login brute-force rate limiting (e.g. Bucket4j + Redis).
- Full refresh-token reuse *detection* (current fix rotates on every use,
  which already meaningfully shrinks the exploit window, but doesn't yet
  revoke an entire token family on detected reuse).
- Email verification / password-reset flows.
- Integration tests around the auth flow (login → role-gated endpoint,
  refresh → role-gated endpoint, cross-user access attempts on
  wallets/transfers). Given how easily the two critical bugs above hid in
  this codebase, this is the highest-value next investment.

## Tests added

All new tests are plain JUnit 5 + Mockito unit tests (`src/test/java/...`),
deliberately avoiding `@SpringBootTest`/`@WebMvcTest` so they run without a
database or Redis and don't depend on test-slice APIs that couldn't be
verified against this Spring Boot version in this environment. Each one maps
to a fix above:

- `AuthServiceImplTest` — register/login/refresh/logout/getCurrentUserId,
  including a direct regression test that login never issues a
  `ROLE_`-prefixed role claim, and that `getCurrentUserId()` parses the
  numeric principal name rather than treating it as an email.
- `RefreshTokenServiceTest` — create/rotate/revoke/revokeAll against a
  mocked Redis template.
- `JwtAuthenticationConverterTest` — the most direct regression test for the
  double `ROLE_ROLE_USER` bug: asserts exactly one `ROLE_` prefix is ever
  produced, for both `USER` and `ADMIN`.
- `GlobalExceptionHandlerTest` — every handler maps to the intended HTTP
  status, including that an unexpected exception's real message never
  leaks into the response body.
- `WalletServiceImplTest` — wallet creation is always scoped to the caller,
  duplicate-currency rejection, and delete ownership enforcement (regression
  test for the inverted requester/target check).
- `TransferServiceImplTest` — `cancelTransfer` regression tests: sender can
  cancel, receiver can cancel, a third party is rejected (the exact IDOR
  that was fixed), and only `PENDING` transfers can be cancelled.
- `AuthControllerTest` — cookie attributes (`HttpOnly`, `Secure`,
  `Path=/api/v1/auth`) on login/refresh, cookie rotation on refresh, and
  idempotent no-cookie logout.
- `UserControllerTest` — `/me` and change-password are scoped to
  `authService.getCurrentUserId()`, never a client-supplied id (regression
  test for the old `/users/me/{uuid}` IDOR).

**Note:** `FlowLedgerApplicationTests` (pre-existing, `@SpringBootTest`)
loads the full application context and therefore needs Postgres + Redis
reachable (`docker compose up -d`) to pass — the new unit tests above do
not need that and will run standalone via `./mvnw test`.

**Same build-verification caveat as everywhere else in this project:** I
could not run `mvn test` in this environment (no network access, no local
Maven cache), so these are carefully hand-written and reviewed against the
exact method signatures in this codebase, but not build-verified. Run
`./mvnw test` and send me any compile errors or failures.
