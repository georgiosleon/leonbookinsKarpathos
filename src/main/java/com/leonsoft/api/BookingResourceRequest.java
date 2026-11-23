package com.leonsoft.api;

import com.leonsoft.models.BookingResource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookingResourceRequest(
      @NotBlank @Pattern(regexp = "rooms|cars") String type,
      @NotBlank @Size(max = 80) String name,
      @DecimalMin("0.0") BigDecimal rentalTaxPerNight) {

    public BookingResource toResource(String id) {
        BookingResource resource = new BookingResource(id, type, name.trim());
        resource.setRentalTaxPerNight(rentalTaxPerNight == null ? BigDecimal.ZERO : rentalTaxPerNight);
        return resource;
    }
}
