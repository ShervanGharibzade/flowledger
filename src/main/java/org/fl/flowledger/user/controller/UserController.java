package org.fl.flowledger.user.controller;

import io.lettuce.core.dynamic.annotation.Param;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me/{uuid}")
    public ResponseEntity<UserResponse> me(@PathVariable UUID uuid) {
        return userService.getUser(uuid);
    }
}
