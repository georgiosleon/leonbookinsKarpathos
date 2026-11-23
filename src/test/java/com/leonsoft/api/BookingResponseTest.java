package com.leonsoft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.leonsoft.models.Booking;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BookingResponseTest {

    @Test
    void fromFormatsIsoDatesToEuropeanDisplayFormat() {
        Booking booking = booking();
        booking.setStartDate("2026-06-20");
        booking.setEndDate("2026-06-23");

        BookingResponse response = BookingResponse.from(booking);

        assertEquals("20/06/2026", response.startDate());
        assertEquals("23/06/2026", response.endDate());
    }

    @Test
    void fromPreservesLegacyEuropeanDatesAsEuropeanDisplayFormat() {
        Booking booking = booking();
        booking.setStartDate("20/06/2026");
        booking.setEndDate("23/06/2026");

        BookingResponse response = BookingResponse.from(booking);

        assertEquals("20/06/2026", response.startDate());
        assertEquals("23/06/2026", response.endDate());
    }

    @Test
    void fromHandlesNullOrBlankDatesAndMapsFields() {
        LocalDateTime requestDateTime = LocalDateTime.of(2026, 6, 1, 12, 30);
        Booking booking = booking();
        booking.setStartDate(null);
        booking.setEndDate(" ");
        booking.setRequestDateTime(requestDateTime);

        BookingResponse response = BookingResponse.from(booking);

        assertNull(response.startDate());
        assertEquals(" ", response.endDate());
        assertEquals("booking-1", response.id());
        assertEquals("Ada", response.name());
        assertEquals("APT 2", response.roomName());
        assertEquals("active", response.status());
        assertEquals(requestDateTime, response.requestDateTime());
    }

    private static Booking booking() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setName("Ada");
        booking.setAgency("Agency");
        booking.setRoom("room-1");
        booking.setRoomName("APT 2");
        booking.setNumOfGuests(2);
        booking.setNumOfNights(3);
        booking.setCharge(100.0);
        booking.setReceived(20.0);
        booking.setCommission(10.0);
        booking.setBalance(70.0);
        booking.setStatus(com.leonsoft.models.BookingStatus.ACTIVE);
        return booking;
    }
}
