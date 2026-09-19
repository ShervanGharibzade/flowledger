package org.fl.flowledger.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getMethod()).thenReturn("POST");
    }

    @Test
    void resourceNotFound_mapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResourceNotFound(
                        new ResourceNotFoundException("User", 1L), request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("/api/v1/test", response.getBody().path());
    }

    @Test
    void transferNotFound_mapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(
                        new TransferNotFoundException("Transfer not found"), request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void unauthorizedWalletAccess_mapsTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleForbidden(
                        new UnauthorizedWalletAccessException("no access"), request
                );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void walletAccessDenied_mapsTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleForbidden(new WalletAccessDeniedException(), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void domainUnauthorized_mapsTo401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorized(new UnauthorizedException(), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void domainBadCredentials_mapsTo401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(
                        new org.fl.flowledger.common.exception.BadCredentialsException(),
                        request
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void springAuthenticationFailure_mapsTo401WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthenticationFailure(
                        new BadCredentialsException("Bad credentials"), request
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email or password is wrong.", response.getBody().message());
    }

    @Test
    void emailAlreadyUsed_mapsTo409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleEmailAlreadyUsed(new EmailAlreadyUsedException(), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void illegalArgument_mapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(
                        new IllegalArgumentException("bad input"), request
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().message());
    }

    @Test
    void illegalState_mapsTo409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleConflict(
                        new IllegalStateException("already cancelled"), request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void unexpectedException_mapsTo500WithGenericMessage_neverLeakingDetails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(
                        new RuntimeException("db password is hunter2"), request
                );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred.", response.getBody().message());
        assertFalse(response.getBody().message().contains("hunter2"));
    }
}
