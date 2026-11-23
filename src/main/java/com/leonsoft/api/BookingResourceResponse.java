package com.leonsoft.api;

import com.leonsoft.models.BookingResource;

public record BookingResourceResponse(
        String id,
        String type,
        String name,
        boolean active) {

    public static BookingResourceResponse from(BookingResource resource) {
        return new BookingResourceResponse(
                resource.getId(),
                resource.getType(),
                resource.getName(),
                resource.isActive());
    }
}
