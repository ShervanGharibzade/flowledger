package org.fl.flowledger.user.controller;

import org.fl.flowledger.auth.service.AuthService;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.dto.UserRoles;
import org.fl.flowledger.user.dto.UserStatus;
import org.fl.flowledger.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController controller;

    @Test
    void me_returnsOnlyTheAuthenticatedCallersOwnProfile() {
        UserResponse expected = new UserResponse(
                UUID.randomUUID(), "jane@example.com", "Jane", "Doe",
                UserRoles.USER, UserStatus.ACTIVE
        );

        when(authService.getCurrentUserId()).thenReturn(42L);
        when(userService.getUserById(42L)).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.me();

        assertEquals(expected, response.getBody());
        verify(userService).getUserById(42L);
        verify(userService, never()).getUser(any());
    }

    @Test
    void changePassword_scopesToTheAuthenticatedCaller_notAClientSuppliedId() {
        ChangePasswordDto dto = new ChangePasswordDto("oldPassword1", "newPassword1");

        when(authService.getCurrentUserId()).thenReturn(42L);
        when(userService.changePassword(42L, dto)).thenReturn("Password changed successfully");

        ResponseEntity<String> response = controller.changePassword(dto);

        assertEquals("Password changed successfully", response.getBody());
        verify(userService).changePassword(42L, dto);
    }
}
