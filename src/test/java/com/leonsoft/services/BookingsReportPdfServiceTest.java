package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BookingsReportPdfServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingResourceService resourceService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setActive(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("admin", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportsOnlyTheSelectedModeAndArrivalDateRange() throws Exception {
        Booking roomBooking = booking("APT2", "Room Guest");
        Booking carBooking = booking("CAR1", "Car Guest");
        when(resourceService.getAll()).thenReturn(List.of(
              new BookingResource("APT2", "rooms", "APT 2"),
              new BookingResource("CAR1", "cars", "CAR 1")));
        when(resourceService.namesById()).thenReturn(Map.of("APT2", "APT 2", "CAR1", "CAR 1"));
        when(bookingRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(roomBooking, carBooking));

        MockHttpServletResponse response = new MockHttpServletResponse();
        BookingsService service = new BookingsService(
              bookingRepository, userRepository, resourceService, new BookingAvailabilityPolicy(true, true), new BookingOverlapChecker(bookingRepository));

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
