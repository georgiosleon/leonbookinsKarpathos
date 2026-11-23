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

    public static double normalizeMoney(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateBalance(Double charge, Double received, Double commission) {
        BigDecimal result = BigDecimal.valueOf(normalizeMoney(charge))
                .subtract(BigDecimal.valueOf(normalizeMoney(received)))
                .subtract(BigDecimal.valueOf(normalizeMoney(commission)));
        return result.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
