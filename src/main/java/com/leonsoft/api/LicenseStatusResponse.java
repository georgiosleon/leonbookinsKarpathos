package com.leonsoft.api;

public record LicenseStatusResponse(
        boolean activated,
        String installId,
        CustomerResponse customer,
        String activatedAt,
        String activatedBy
) {
}
