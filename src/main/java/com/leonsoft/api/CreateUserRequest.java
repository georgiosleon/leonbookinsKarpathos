package com.leonsoft.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "ADMIN|OPERATOR|GUEST|USER") String role,
        Boolean active) {
}
