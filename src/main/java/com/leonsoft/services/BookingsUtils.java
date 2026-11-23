package com.leonsoft.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class BookingsUtils {

    private BookingsUtils() {
    }

    public static final DateTimeFormatter formatterEURO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter formatterISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate parseStoredDate(String date) {
        try {
            return LocalDate.parse(date, formatterISO);
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(date, formatterEURO);
        }
    }
}
