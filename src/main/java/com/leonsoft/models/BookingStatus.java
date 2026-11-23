package com.leonsoft.models;

import java.util.List;

public final class BookingStatus {

    public static final String ACTIVE = "active";
    public static final String PAID = "paid";
    public static final String CANCELLED = "cancelled";
    public static final List<String> BOOKABLE = List.of(ACTIVE, PAID);

    private BookingStatus() {
    }

    public static boolean isBookable(String status) {
        return ACTIVE.equals(status) || PAID.equals(status);
    }

    public static boolean isCancelled(String status) {
        return CANCELLED.equals(status);
    }
}
