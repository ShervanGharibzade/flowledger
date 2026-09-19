package org.fl.flowledger.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fl.flowledger.auth.AuthMapper.AuthMapper;
import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResponse;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.RefreshResult;
import org.fl.flowledger.auth.dto.TokenResponse;
import org.fl.flowledger.auth.service.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthServiceImpl authService;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, authMapper);
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
    }

    @Test
    void login_setsHttpOnlySecureRefreshCookieScopedToAuthPath() {
        LoginDto dto = new LoginDto("jane@example.com", "password123");
        LoginResult result = new LoginResult(
                "jane@example.com", UUID.randomUUID(), "access-token", "refresh-token"
        );
        LoginResponse expectedBody = new LoginResponse(
                "jane@example.com", result.uuid(), "access-token"
        );

        when(authService.login(dto)).thenReturn(result);
        when(authMapper.toResponse(result)).thenReturn(expectedBody);

        ResponseEntity<LoginResponse> response1 = controller.login(dto, response);

        assertEquals(expectedBody, response1.getBody());

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());

        String cookie = cookieCaptor.getValue();
        assertTrue(cookie.contains("refresh_token=refresh-token"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("Path=/api/v1/auth"));
    }

    @Test
    void refresh_rotatesCookieAndReturnsOnlyTheAccessTokenInBody() {
        RefreshResult result = new RefreshResult("new-access", "new-refresh");
        when(authService.refresh("old-refresh")).thenReturn(result);

        ResponseEntity<TokenResponse> response1 = controller.refresh("old-refresh", response);

        assertEquals("new-access", response1.getBody().accessToken());

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        assertTrue(cookieCaptor.getValue().contains("refresh_token=new-refresh"));
    }

    @Test
    void logout_withNoCookie_isAnIdempotentNoOp() throws Exception {
        ResponseEntity<String> response1 = controller.logout(request, null, response);

        assertEquals("Already logged out", response1.getBody());
        verify(authService, never()).logout(any(), any());
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), any());
    }

    @Test
    void logout_withCookie_revokesSessionAndClearsCookie() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.logout(eq("refresh-token"), any())).thenReturn("User logout successfully");

        ResponseEntity<String> response1 = controller.logout(request, "refresh-token", response);

        assertEquals("User logout successfully", response1.getBody());
        verify(authService).logout(eq("refresh-token"), any());

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        assertTrue(cookieCaptor.getValue().contains("refresh_token="));
        assertTrue(cookieCaptor.getValue().contains("Max-Age=0"));
    }

    @Test
    void register_delegatesToAuthService() {
        var dto = new org.fl.flowledger.user.dto.CreateUserDto(
                "new@example.com", "password123", "New", "User"
        );
        when(authService.register(dto)).thenReturn("User registered successfully now you can login.");

        ResponseEntity<String> response1 = controller.register(dto);

        assertEquals("User registered successfully now you can login.", response1.getBody());
    }
}
