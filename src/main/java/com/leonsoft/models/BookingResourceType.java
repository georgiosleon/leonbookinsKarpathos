package com.leonsoft.models;

public final class BookingResourceType {

    public static final String ROOMS = "rooms";
    public static final String CARS = "cars";

    private BookingResourceType() {
    }

    public static boolean isSupported(String type) {
        return ROOMS.equals(type) || CARS.equals(type);
    }
}
