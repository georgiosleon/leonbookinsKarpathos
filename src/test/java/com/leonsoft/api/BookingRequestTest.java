package com.leonsoft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
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
              null, null, "ada@example.com", null, null, null, null, null, "PLAT-123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidDatesAndUnsupportedStatus() {
        BookingRequest request = new BookingRequest(
              null, "Ada", 2, "agency", "room-1", "Greece",
              "31/02/2026", "22/06/2026", 2,
              null, null, null, null, "unknown",
              null, null, null, null, null, null, null, null, null);

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
              violation.getPropertyPath().toString().equals("dateRangeValid")));
        assertTrue(violations.stream().anyMatch(violation ->
              violation.getPropertyPath().toString().equals("status")));
    }

    @Test
    void rejectsMissingRequiredFieldsInvalidEmailAndNegativeMoney() {
        BookingRequest request = new BookingRequest(
              null, " ", 0, "", "", "Greece",
              "20/06/2026", "22/06/2026", 2,
              new BigDecimal("-1"), new BigDecimal("-1"), new BigDecimal("-1"), null, "active",
              null, null, "not-an-email", null, null, null, null, null, null);

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("numOfGuests")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("agency")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("resourceId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("charge")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("commission")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("received")));
    }

    @Test
    void toBookingMapsContractFields() {
        BookingRequest request = new BookingRequest(
              "id-1", "Ada", 2, "agency", "room-1", "Greece",
              "20/06/2026", "22/06/2026", 2,
              new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("40.00"), "active",
              "id", "afm", "ada@example.com", "tel", "voucher", "tax", "inv", "extra", "PLAT-123");

        var booking = request.toBooking();

        assertEquals("id-1", booking.getId());
        assertEquals("room-1", booking.getResourceId());
        assertEquals(java.time.LocalDate.of(2026, 6, 20), booking.getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 6, 22), booking.getEndDate());
        assertEquals(new BigDecimal("100.00"), booking.getCharge());
        assertEquals(com.leonsoft.models.BookingStatus.ACTIVE, booking.getStatus());
        assertEquals("PLAT-123", booking.getPlatformReservationId());
    }
}
