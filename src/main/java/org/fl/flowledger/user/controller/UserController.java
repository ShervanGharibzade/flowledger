package org.fl.flowledger.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.service.AuthService;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    // Deliberately takes no path/query parameter: "me" always means the
    // caller's own account, derived from the authenticated token. Do not
    // change this to accept an id/uuid from the client — that would let any
    // authenticated user read any other user's profile by guessing an id.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = authService.getCurrentUserId();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordDto dto
    ) {
        Long userId = authService.getCurrentUserId();
        return ResponseEntity.ok(userService.changePassword(userId, dto));
    }
}
