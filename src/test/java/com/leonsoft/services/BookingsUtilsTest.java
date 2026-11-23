package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BookingsUtilsTest {

    @Test
    void normalizesMoneyAndCalculatesBalanceWithTwoDecimalRounding() {
        assertEquals(new BigDecimal("0.00"), BookingsUtils.normalizeMoney(null));
        assertEquals(new BigDecimal("10.12"), BookingsUtils.normalizeMoney(new BigDecimal("10.123")));
        assertEquals(new BigDecimal("10.13"), BookingsUtils.normalizeMoney(new BigDecimal("10.125")));
        assertEquals(new BigDecimal("90.01"), BookingsUtils.calculateBalance(
                new BigDecimal("100.01"), new BigDecimal("5.00"), new BigDecimal("5.00")));
    }
}
