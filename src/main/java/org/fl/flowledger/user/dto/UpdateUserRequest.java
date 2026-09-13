package org.fl.flowledger.user.dto;

import java.util.UUID;

public record UpdateUserRequest(
        UUID uuid,
        String firstName,
        String LastName
) {
}
