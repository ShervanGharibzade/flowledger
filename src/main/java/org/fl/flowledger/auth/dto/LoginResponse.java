package org.fl.flowledger.auth.dto;

import java.util.UUID;

public record LoginResponse(
        String email,
        UUID uuid,
        String accessToken
) {}
