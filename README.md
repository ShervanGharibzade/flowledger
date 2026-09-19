# Flow Ledger

A Spring Boot ledger/wallet service: users register and log in, hold one
wallet per currency, and transfer money between wallets. Every transfer
posts matching debit/credit entries to an append-only ledger.

## Tech stack

- **Java 21**, **Spring Boot 4.1**
- **Spring Security** with **JWT** (RSA-signed, OAuth2 resource server) for
  stateless auth + **Redis**-backed refresh tokens
- **PostgreSQL** (via Spring Data JPA) with **Flyway** migrations
- **MapStruct** for entity ↔ DTO mapping
- **Maven** (wrapper included, no local Maven install needed)

## Features

- Email/password registration and login, short-lived (15 min) JWT access
  tokens + rotating refresh tokens in an `HttpOnly` cookie
- Self-service password change that revokes all other active sessions
- Per-user, per-currency wallets (`IRR`, `TOMAN`, `USD`)
- Idempotent wallet-to-wallet transfers with cancellation (owner-only)
- An append-only ledger of debit/credit entries per wallet and per transfer
- Audit log of security-relevant events (e.g. logout)
- Role-based access (`USER`, `ADMIN`) enforced via Spring Security + JWT
  claims

## Project structure

Code is organized by feature/domain, each with its own `controller`,
`service`, `dto`, `entity`, `repository` (and `mapper` where relevant):

```
src/main/java/org/fl/flowledger/
├── auth/       # login, register, refresh, logout
├── user/       # user profile, change password
├── wallet/     # wallet create/delete/list
├── transfer/   # transfer create/list/cancel
├── ledger/     # read-only ledger entries
├── audit/      # audit logging
└── common/     # security config, JWT, exceptions, base entity
```

## Prerequisites

- **Java 21**
- **Docker** (for Postgres + Redis) — or Postgres 17 and Redis 8 installed
  locally instead

Maven itself isn't required — the project ships `mvnw` / `mvnw.cmd`.

## Running locally

1. **Start Postgres and Redis:**
   ```bash
   docker compose up -d
   ```
   This starts Postgres 17 (`localhost:5432`, db/user/password all
   `flowledger`) and Redis 8 (`localhost:6379`), matching the defaults the
   `dev` Spring profile expects — no extra config needed.

2. **(Optional) set a persistent JWT signing key.** If you skip this, the
   app generates a random RSA key pair on every startup (with a warning in
   the logs) — fine for a one-off run, but every previously-issued token
   becomes invalid on restart. To avoid that during regular dev work:
   ```bash
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
   openssl pkey -in private.pem -pubout -out public.pem
   openssl pkcs8 -topk8 -nocrypt -in private.pem -out private_pkcs8.pem

   export JWT_PRIVATE_KEY="$(cat private_pkcs8.pem)"
   export JWT_PUBLIC_KEY="$(cat public.pem)"
   ```
   (PowerShell: `$env:JWT_PRIVATE_KEY = Get-Content private_pkcs8.pem -Raw`, etc.)

3. **Run the app:**
   ```bash
   ./mvnw spring-boot:run
   ```
   Activates the `dev` Spring profile by default, runs Flyway migrations
   against Postgres automatically, and starts on **port 8080**.

4. **Verify it's up:**
   ```bash
   curl http://localhost:8080/actuator/health
   # {"status":"UP"}
   ```

## Configuration

Config lives in `src/main/resources/application.yml`, split into `dev` and
`prod` Spring profiles (`SPRING_PROFILES_ACTIVE`, defaults to `dev`). `dev`
ships with working defaults matching `docker-compose.yml`; `prod` requires
everything below to be set explicitly, with no fallback. See `.env.example`
for a copyable template — note this project has no `.env` auto-loading, so
these need to be real exported environment variables (or set in your
IDE's run configuration / process manager / secret store).

| Variable | Required in `prod` | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | – | `dev` or `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | Postgres connection |
| `REDIS_HOST`, `REDIS_PORT` | ✅ (`REDIS_PORT` defaults to 6379) | Redis connection (refresh tokens) |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | ✅ | PEM-encoded RSA key pair for signing/verifying access tokens |
| `COOKIE_SECURE` | – (defaults `true`; `dev` profile forces `false`) | Whether the refresh-token cookie requires HTTPS |
| `CORS_ALLOWED_ORIGINS` | – (defaults `http://localhost:3000`) | Comma-separated list of allowed frontend origins |

## API reference

Base path: `/api/v1`. Protected endpoints require `Authorization: Bearer
<accessToken>`; the refresh token travels only as an `HttpOnly` cookie set
by `/auth/login` and `/auth/refresh`.

### Auth (`/auth`) — public

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{ email, password, firstName, LastName }` | Password min 8 chars |
| POST | `/auth/login` | `{ email, password }` | Returns `{ email, uuid, accessToken }`; sets `refresh_token` cookie |
| POST | `/auth/refresh` | *(cookie only)* | Rotates the refresh token; returns a new `{ accessToken }` |
| POST | `/auth/logout` | *(cookie only)* | Idempotent — succeeds even with no active session |

### Users (`/users`) — authenticated

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/users/me` | – | Always the caller's own profile — no id/uuid parameter |
| PATCH | `/users/me/password` | `{ currentPassword, newPassword }` | Revokes all other sessions on success |

### Wallets (`/wallet`) — authenticated

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/wallet` | `{ currency }` | Creates a wallet for the caller; one wallet per currency |
| DELETE | `/wallet` | `{ walletUuId }` | Caller must own the wallet and its balance must be zero |
| GET | `/wallet` | – | Lists the caller's own wallets |
| GET | `/wallet/Wallets/{id}` | – | A single wallet by id (must belong to the caller) |

### Transfers (`/transfer`) — authenticated

| Method | Path | Body / Headers | Notes |
|---|---|---|---|
| POST | `/transfer` | `{ senderWalletId, receiverWalletId, amount, currency }` + `Idempotency-Key` header | `Idempotency-Key` is required |
| GET | `/transfer` | – | Lists the caller's own transfers |
| GET | `/transfer/{id}` | – | Caller must be the sender or receiver |
| POST | `/transfer/{id}/cancel` | – | Only `PENDING` transfers; caller must be the sender or receiver |

### Ledger (`/ledger`) — authenticated

| Method | Path | Notes |
|---|---|---|
| GET | `/ledger/wallet/{walletId}` | Entries for a wallet |
| GET | `/ledger/transfer/{transferId}` | Entries for a transfer |

## Data model

Flyway migration `V1__init_schema.sql` creates: `users`, `wallets`,
`transfers`, `ledger_entries`, `audit_logs`.

## Testing

```bash
./mvnw test
```

The test suite is plain JUnit 5 + Mockito unit tests (no database/Redis
required) covering the auth flow, wallet/transfer ownership checks, and the
exception-handling layer. The one exception is the pre-existing
`FlowLedgerApplicationTests` (`@SpringBootTest`, full context load), which
does need Postgres + Redis reachable — run `docker compose up -d` first if
you're running the full suite including that test.

## Security notes

This codebase went through a security review and hardening pass; see
[`SECURITY_CHANGES.md`](./SECURITY_CHANGES.md) for the full list of fixes
(auth/JWT correctness, IDOR fixes, exception handling, refresh-token
rotation, etc.) and the items intentionally left for follow-up (rate
limiting, email verification, full refresh-token reuse detection).
