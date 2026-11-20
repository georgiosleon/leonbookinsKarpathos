package com.leonsoft.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leonsoft.config.SecurityConfig;
import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.services.BookingResourceService;
import com.leonsoft.services.BookingsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
      controllers = {
            BookingsRestController.class, BookingResourceController.class, CsrfController.class
      },
      properties = {
            "APP_ADMIN_USERNAME=test-admin",
            "APP_ADMIN_PASSWORD=test-only-password-123"
      })
@Import(SecurityConfig.class)
class BookingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingsService bookingsService;

    @MockBean
    private BookingResourceService bookingResourceService;

    @Test
    void bookingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/booking/getAll"))
              .andExpect(status().isUnauthorized());
    }

    @Test
    void resourceEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
              .andExpect(status().isUnauthorized());
    }

    @Test
    void versionedCsrfEndpointRemainsAvailableToAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/api/v1/csrf").with(user("test-admin").roles("ADMIN")))
              .andExpect(status().isOk());
    }

    @Test
    void onlyAdminRoleCanReadBookings() throws Exception {
        when(bookingsService.getAll()).thenReturn(List.<Booking>of());

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("operator").roles("USER")))
              .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("test-admin").roles("ADMIN")))
              .andExpect(status().isOk());

        mockMvc.perform(get("/booking/getAll").with(user("test-admin").roles("ADMIN")))
              .andExpect(status().isOk());
    }

    @Test
    void resourceManagementRequiresAdminRole() throws Exception {
        when(bookingResourceService.getAll()).thenReturn(List.<BookingResource>of());

        mockMvc.perform(get("/api/v1/resources").with(user("operator").roles("USER")))
              .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resources").with(user("test-admin").roles("ADMIN")))
              .andExpect(status().isOk());
    }

    @Test
    void bookingWritesRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/booking/delete")
                    .with(user("test-admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":\"booking-1\"}"))
              .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/booking/delete")
                    .with(user("test-admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":\"booking-1\"}"))
              .andExpect(status().isOk());
    }

    @Test
    void resourceWritesRequireCsrfToken() throws Exception {
        when(bookingResourceService.create(any(BookingResourceRequest.class)))
              .thenReturn(new BookingResource("resource-1", "rooms", "Suite 1"));

        mockMvc.perform(post("/api/v1/resources")
                    .with(user("test-admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"rooms\",\"name\":\"Suite 1\"}"))
              .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/resources")
                    .with(user("test-admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"rooms\",\"name\":\"Suite 1\"}"))
              .andExpect(status().isCreated());
    }

    @Test
    void logoutWithCsrfReturnsNoContent() throws Exception {
        mockMvc.perform(post("/logout")
                    .with(user("test-admin").roles("ADMIN"))
                    .with(csrf()))
              .andExpect(status().isNoContent());
    }
}
