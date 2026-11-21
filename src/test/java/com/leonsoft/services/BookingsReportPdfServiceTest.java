package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class BookingsReportPdfServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingResourceService resourceService;

    @Test
    void exportsOnlyTheSelectedModeAndArrivalDateRange() throws Exception {
        Booking roomBooking = booking("APT2", "Room Guest");
        Booking carBooking = booking("CAR1", "Car Guest");
        when(resourceService.getAll()).thenReturn(List.of(
              new BookingResource("APT2", "rooms", "APT 2"),
              new BookingResource("CAR1", "cars", "CAR 1")));
        when(resourceService.namesById()).thenReturn(Map.of("APT2", "APT 2", "CAR1", "CAR 1"));
        when(bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              "2026-06-01", "2026-06-30")).thenReturn(List.of(roomBooking, carBooking));

        MockHttpServletResponse response = new MockHttpServletResponse();
        BookingsService service = new BookingsService(
              bookingRepository, resourceService, new BookingAvailabilityPolicy(true, true));

        service.exportReportToPDF(response, "01/06/2026", "30/06/2026", "rooms", Locale.UK);

        assertTrue(response.getContentType().startsWith("application/pdf"));
        assertTrue(response.getHeader("Content-Disposition").contains("booking-report-rooms-"));
        PdfReader reader = new PdfReader(response.getContentAsByteArray());
        try {
            String pageText = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(pageText.contains("Room Guest"));
            assertFalse(pageText.contains("Car Guest"));
            assertTrue(pageText.contains("APT 2"));
        } finally {
            reader.close();
        }
    }

    private static Booking booking(String resourceId, String name) {
        Booking booking = new Booking();
        booking.setId(name);
        booking.setStartDate("2026-06-12");
        booking.setEndDate("2026-06-15");
        booking.setStatus("active");
        booking.setRoom(resourceId);
        booking.setName(name);
        booking.setAgency("abnb");
        booking.setNumOfGuests(2);
        booking.setNumOfNights(3);
        booking.setCharge(300.0);
        booking.setReceived(100.0);
        booking.setBalance(200.0);
        return booking;
    }
}
