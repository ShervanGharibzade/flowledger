package org.fl.flowledger.user.dto;

import java.util.UUID;

public record UserResponse(
        UUID uuid,
        String email,
        String firstName,
        String lastName,
        UserRoles role,
        UserStatus status

) {}
