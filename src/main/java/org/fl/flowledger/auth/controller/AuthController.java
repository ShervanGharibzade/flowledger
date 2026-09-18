package org.fl.flowledger.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.AuthMapper.AuthMapper;
import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResponse;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.RefreshResult;
import org.fl.flowledger.auth.dto.TokenResponse;
import org.fl.flowledger.auth.service.AuthServiceImpl;
import org.fl.flowledger.user.dto.CreateUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final AuthMapper authMapper;

    // Defaults to true (cookie only sent over HTTPS). Override with
    // app.cookie.secure=false only for local HTTP development.
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginDto body,
            HttpServletResponse response) {

        LoginResult data = authService.login(body);

        setRefreshCookie(response, data.refreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok(authMapper.toResponse(data));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) throws UnknownHostException {

        clearRefreshCookie(response);

        if (refreshToken == null || refreshToken.isBlank()) {
            // No session to invalidate; treat as a successful, idempotent no-op
            // rather than erroring out.
            return ResponseEntity.ok("Already logged out");
        }

        InetAddress ipAddress = InetAddress.getByName(request.getRemoteAddr());

        return ResponseEntity.ok(
                authService.logout(refreshToken, ipAddress)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody CreateUserDto body
    ) {
        String data = authService.register(body);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse response
    ) {
        RefreshResult result = authService.refresh(refreshToken);

        setRefreshCookie(response, result.refreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok(new TokenResponse(result.accessToken()));
    }

    private void setRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", Duration.ZERO);
    }
}
