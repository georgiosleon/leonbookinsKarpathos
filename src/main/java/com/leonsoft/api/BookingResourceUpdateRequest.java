package com.leonsoft.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookingResourceUpdateRequest(
        @NotBlank @Size(max = 80) String name,
        @DecimalMin("0.0") BigDecimal rentalTaxPerNight,
        @NotNull Boolean active,
        @Size(max = 120) String registrationNumber,
        @Size(max = 255) String propertyAddress,
        @Size(max = 120) String taxCategory) {
}
