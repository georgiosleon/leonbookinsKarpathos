package com.leonsoft.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Customer name is required.")
        @Size(max = 255, message = "Customer name is too long.")
        String name,
        @Size(max = 100, message = "Telephone is too long.")
        String telephone,
        @Email(message = "Email must be valid.")
        @Size(max = 255, message = "Email is too long.")
        String email
) {
}
