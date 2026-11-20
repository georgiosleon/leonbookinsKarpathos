package com.leonsoft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leonsoft.config.CustomErrorHandler;
import com.leonsoft.services.BookingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookingsRestControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
              .standaloneSetup(new BookingsRestController(org.mockito.Mockito.mock(BookingsService.class)))
              .setControllerAdvice(new CustomErrorHandler())
              .build();
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
