package com.leonsoft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.leonsoft.config.CustomErrorHandler;
import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingReportService;
import com.leonsoft.services.BookingsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookingsRestControllerTest {
    private MockMvc mockMvc;
    private BookingsService bookingsService;
    private BookingReportService bookingReportService;

    @BeforeEach
    void setUp() {
        bookingsService = org.mockito.Mockito.mock(BookingsService.class);
        bookingReportService = org.mockito.Mockito.mock(BookingReportService.class);
        mockMvc = MockMvcBuilders
              .standaloneSetup(new BookingsRestController(bookingsService, bookingReportService))
              .setControllerAdvice(new CustomErrorHandler())
              .build();
    }

    @Test
    void pdfExportPassesTheSelectedModeDatesAndLanguage() throws Exception {
        mockMvc.perform(get("/api/v1/booking/export/pdf")
                    .param("fromDate", "01/06/2026")
                    .param("toDate", "30/06/2026")
                    .param("type", "cars")
                    .param("lang", "fr"))
              .andExpect(status().isOk());

        verify(bookingReportService).exportReportToPDF(
              any(), eq("01/06/2026"), eq("30/06/2026"), eq("cars"), eq(Locale.FRANCE));
    }

    @Test
    void cancelledBookingDeletionRequiresPost() throws Exception {
        mockMvc.perform(get("/api/v1/booking/del").param("bid", "booking-1"))
              .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/api/v1/booking/del").param("bid", "booking-1"))
              .andExpect(status().isOk());

        verify(bookingsService).delCancelledBooking("booking-1");
    }

    @Test
    void rejectsMalformedBookingWithBadRequest() throws Exception {
        mockMvc.perform(post("/booking/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                          {
                            "name": "Ada",
                            "numOfGuests": 2,
                            "agency": "agency",
                            "room": "room-1",
                            "startDate": "31/02/2026",
                            "endDate": "01/03/2026",
                            "status": "active"
                          }
                          """))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").value("Invalid request."));
    }

    @Test
    void keepsLegacyBookingRoutesAvailable() throws Exception {
        mockMvc.perform(post("/booking/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").value("Invalid request."));
    }

    @Test
    void returnsLocalizedValidationErrorForGreekRequests() throws Exception {
        mockMvc.perform(post("/api/v1/booking/save")
                    .header("Accept-Language", "el")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").value("Τα δεδομένα του αιτήματος δεν είναι έγκυρα."));
    }

    @Test
    void getAllReturnsBookingResponseWithEuropeanDates() throws Exception {
        Booking booking = booking("2026-06-20", "2026-06-23");
        org.mockito.Mockito.when(bookingsService.getAll()).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/v1/booking/getAll"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$[0].id").value("booking-1"))
              .andExpect(jsonPath("$[0].startDate").value("20/06/2026"))
              .andExpect(jsonPath("$[0].endDate").value("23/06/2026"))
              .andExpect(jsonPath("$[0].roomName").value("APT 2"))
              .andExpect(jsonPath("$[0].status").value("active"));
    }

    @Test
    void saveReturnsBookingResponseDto() throws Exception {
        Booking booking = booking("2026-06-20", "2026-06-23");
        org.mockito.Mockito.when(bookingsService.create(any())).thenReturn(booking);

        mockMvc.perform(post("/api/v1/booking/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                          {
                            "name": "Ada",
                            "numOfGuests": 2,
                            "agency": "agency",
                            "room": "room-1",
                            "startDate": "20/06/2026",
                            "endDate": "23/06/2026",
                            "status": "active"
                          }
                          """))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.id").value("booking-1"))
              .andExpect(jsonPath("$.startDate").value("20/06/2026"))
              .andExpect(jsonPath("$.endDate").value("23/06/2026"))
              .andExpect(jsonPath("$.roomName").value("APT 2"));
    }

    private static Booking booking(String startDate, String endDate) {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setName("Ada");
        booking.setAgency("agency");
        booking.setRoom("room-1");
        booking.setRoomName("APT 2");
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setNumOfGuests(2);
        booking.setNumOfNights(3);
        booking.setStatus("active");
        booking.setRequestDateTime(LocalDateTime.of(2026, 6, 1, 12, 0));
        return booking;
    }
}
