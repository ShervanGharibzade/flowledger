package org.fl.flowledger.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.AuthMapper.AuthMapper;
import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResponse;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.TokenResponse;
import org.fl.flowledger.auth.service.AuthServiceImpl;
import org.fl.flowledger.user.dto.CreateUserDto;
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


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginDto body,
            HttpServletResponse response) {

            LoginResult data = authService.login(body);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", data.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        return ResponseEntity.ok(authMapper.toResponse(data));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) throws UnknownHostException {
        InetAddress ipAddress =
                InetAddress.getByName(request.getRemoteAddr());

        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return ResponseEntity.ok(
                authService.logout(refreshToken, ipAddress)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody CreateUserDto body
    ){
        String data = authService.register(body);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @CookieValue("refresh_token") String refreshToken
    ) {
        return authService.refresh(refreshToken);
    }
}
