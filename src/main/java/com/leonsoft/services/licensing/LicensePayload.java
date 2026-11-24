package com.leonsoft.services.licensing;

public record LicensePayload(
        String product,
        String licenseId,
        String installId,
        String customerName,
        String customerTelephone,
        String customerEmail,
        String issuedAt
) {
}
