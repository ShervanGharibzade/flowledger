package org.fl.flowledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserDto(
        @NotBlank
        @Size(max = 255)
        String email,
        @NotBlank
        @Size(min = 8, max = 255)
        String password,
        @NotBlank
        @Size(max = 100)
        String firstName,
        @NotBlank
        @Size(max = 100)
        String LastName
) {
}
