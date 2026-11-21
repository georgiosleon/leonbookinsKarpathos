package com.leonsoft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.leonsoft.config.CustomErrorHandler;
import com.leonsoft.services.BookingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Locale;

class BookingsRestControllerTest {
    private MockMvc mockMvc;
    private BookingsService bookingsService;

    @BeforeEach
    void setUp() {
        bookingsService = org.mockito.Mockito.mock(BookingsService.class);
        mockMvc = MockMvcBuilders
              .standaloneSetup(new BookingsRestController(bookingsService))
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

        verify(bookingsService).exportReportToPDF(
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
}
