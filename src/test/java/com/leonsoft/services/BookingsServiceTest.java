package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookingsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingResourceService resourceService;

    @InjectMocks
    private BookingsService bookingsService;

    @Test
    void createNormalizesDatesAndRecalculatesNightsAndBalance() {
        Booking booking = booking("20/06/2026", "23/06/2026");
        booking.setNumOfNights(99);
        booking.setBalance(999.0);
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            assertEquals("2026-06-20", saved.getStartDate());
            assertEquals("2026-06-23", saved.getEndDate());
            assertEquals(3, saved.getNumOfNights());
            assertEquals(70.0, saved.getBalance());
            return saved;
        });

        Booking result = bookingsService.create(booking);

        assertNotNull(result.getId());
        assertEquals("20/06/2026", result.getStartDate());
        assertEquals("23/06/2026", result.getEndDate());
        assertEquals(3, result.getNumOfNights());
        assertEquals(70.0, result.getBalance());
        assertEquals("APT 2", result.getRoomName());
    }

    @Test
    void updateKeepsOriginalRequestTimestamp() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Booking existing = new Booking();
        existing.setRequestDateTime(createdAt);
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking update = booking("20/06/2026", "22/06/2026");
        update.setId("booking-1");
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));

        Booking result = bookingsService.update(update);

        assertEquals(createdAt, result.getRequestDateTime());
        assertEquals("APT 2", result.getRoomName());
        verify(bookingRepository).save(update);
    }

    @Test
    void updateReturnsNotFoundWhenBookingDoesNotExist() {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());
        Booking update = new Booking();
        update.setId("missing");

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.update(update));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private static Booking booking(String startDate, String endDate) {
        Booking booking = new Booking();
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setCharge(100.0);
        booking.setCommission(10.0);
        booking.setReceived(20.0);
        booking.setRoom("APT2");
        return booking;
    }
}
