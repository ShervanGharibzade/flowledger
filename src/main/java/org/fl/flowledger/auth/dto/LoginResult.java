package org.fl.flowledger.auth.dto;

import java.util.UUID;

public record LoginResult(
        String email,
        UUID uuid,
        String accessToken,
        String refreshToken
) {
}
