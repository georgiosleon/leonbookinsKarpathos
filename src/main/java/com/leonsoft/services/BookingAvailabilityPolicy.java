package com.leonsoft.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookingAvailabilityPolicy {

    private final boolean roomsPreventOverlap;
    private final boolean carsPreventOverlap;

    public BookingAvailabilityPolicy(
          @Value("${booking.availability.rooms.prevent-overlap:true}") boolean roomsPreventOverlap,
          @Value("${booking.availability.cars.prevent-overlap:true}") boolean carsPreventOverlap) {
        this.roomsPreventOverlap = roomsPreventOverlap;
        this.carsPreventOverlap = carsPreventOverlap;
    }

    public boolean preventsOverlap(String resourceType) {
        return switch (resourceType) {
            case com.leonsoft.models.BookingResourceType.ROOMS -> roomsPreventOverlap;
            case com.leonsoft.models.BookingResourceType.CARS -> carsPreventOverlap;
            default -> true;
        };
    }

    public AvailabilitySettings settings() {
        return new AvailabilitySettings(roomsPreventOverlap, carsPreventOverlap);
    }

    public record AvailabilitySettings(
          boolean roomsPreventOverlap,
          boolean carsPreventOverlap) {
    }
}
