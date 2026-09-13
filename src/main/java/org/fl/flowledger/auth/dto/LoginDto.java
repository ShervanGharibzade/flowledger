package org.fl.flowledger.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDto(
        @Email
        @NotBlank
        @Size(max=255)
        String email,


        @NotBlank
        @Size(max=255)
        String password
) {
}
