package com.leonsoft.api;

import com.leonsoft.models.BookingResource;
import java.math.BigDecimal;

public record BookingResourceResponse(
        String id,
        String type,
        String name,
        boolean active,
        BigDecimal rentalTaxPerNight) {

    public static BookingResourceResponse from(BookingResource resource) {
        return new BookingResourceResponse(
                resource.getId(),
                resource.getType(),
                resource.getName(),
                resource.isActive(),
                resource.getRentalTaxPerNight());
    }
}
