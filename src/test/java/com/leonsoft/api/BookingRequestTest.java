package com.leonsoft.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidBookingRequest() {
        BookingRequest request = new BookingRequest(
              null, "Ada", 2, "agency", "room-1", "Greece",
              "20/06/2026", "22/06/2026", 2,
              null, null, null, null, "active",
              null, null, "ada@example.com", null, null, null, null, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidDatesAndUnsupportedStatus() {
        BookingRequest request = new BookingRequest(
              null, "Ada", 2, "agency", "room-1", "Greece",
              "31/02/2026", "22/06/2026", 2,
              null, null, null, null, "unknown",
              null, null, null, null, null, null, null, null);

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
              violation.getPropertyPath().toString().equals("dateRangeValid")));
        assertTrue(violations.stream().anyMatch(violation ->
              violation.getPropertyPath().toString().equals("status")));
    }
}
