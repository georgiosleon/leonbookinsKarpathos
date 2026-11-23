package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookingsUtilsTest {

    @Test
    void normalizesMoneyAndCalculatesBalanceWithTwoDecimalRounding() {
        assertEquals(0.0, BookingsUtils.normalizeMoney(null));
        assertEquals(10.12, BookingsUtils.normalizeMoney(10.123));
        assertEquals(10.13, BookingsUtils.normalizeMoney(10.125));
        assertEquals(90.01, BookingsUtils.calculateBalance(100.01, 5.0, 5.0));
    }
}
