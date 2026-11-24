package com.leonsoft.models;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum BookingStatus {
    ACTIVE("active"),
    PAID("paid"),
    CANCELLED("cancelled");

    public static final List<BookingStatus> BOOKABLE = List.of(ACTIVE, PAID);

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BookingStatus from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(status -> status.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown booking status: " + value));
    }

    public static boolean isBookable(BookingStatus status) {
        return status == ACTIVE || status == PAID;
    }

    public static boolean isBookable(String status) {
        if (status == null) {
            return false;
        }
        try {
            return isBookable(from(status));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isCancelled(BookingStatus status) {
        return status == CANCELLED;
    }

    public static boolean isCancelled(String status) {
        if (status == null) {
            return false;
        }
        try {
            return isCancelled(from(status));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
