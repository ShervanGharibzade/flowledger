package org.fl.flowledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChangePasswordDto(
        String email,
        @NotBlank
        @Size(max=255)
        String currentPassword,
        @NotBlank
        @Size(max=255)
        String newPassword
) {
}
