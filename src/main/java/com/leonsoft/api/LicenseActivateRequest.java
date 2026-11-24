package com.leonsoft.api;

import jakarta.validation.constraints.NotBlank;

public record LicenseActivateRequest(
        @NotBlank(message = "License key is required.")
        String licenseKey
) {
}
