package com.leonsoft.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public static LocalDate parseEuroDate(String date) {
        return LocalDate.parse(date, formatterEURO);
    }

    public static BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateBalance(BigDecimal charge, BigDecimal received, BigDecimal commission, BigDecimal rentalTax) {
        return normalizeMoney(charge)
                .subtract(normalizeMoney(received))
                .subtract(normalizeMoney(commission))
                .subtract(normalizeMoney(rentalTax))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
