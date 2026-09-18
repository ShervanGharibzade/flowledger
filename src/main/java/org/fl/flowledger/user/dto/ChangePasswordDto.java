package org.fl.flowledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @NotBlank
        @Size(max = 255)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 255)
        String newPassword
) {
}
