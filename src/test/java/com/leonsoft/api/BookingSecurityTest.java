package com.leonsoft.api;

import com.leonsoft.config.LicenseEnforcementFilter;
import com.leonsoft.config.SecurityConfig;
import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.services.ApplicationSupportService;
import com.leonsoft.services.BookingAvailabilityPolicy;
import com.leonsoft.services.BookingReportService;
import com.leonsoft.services.BookingResourceService;
import com.leonsoft.repositories.UserRepository;
import com.leonsoft.services.BookingsService;
import com.leonsoft.services.CustomerService;
import com.leonsoft.services.AuditService;
import com.leonsoft.services.LicenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
                BookingsRestController.class,
                BookingResourceController.class,
                AvailabilitySettingsRestController.class,
                ApplicationSupportController.class,
                CsrfController.class,
                UserRestController.class,
                LicenseController.class,
                CustomerController.class
        })
@Import({SecurityConfig.class, LicenseEnforcementFilter.class})
class BookingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingsService bookingsService;

    @MockitoBean
    private BookingReportService bookingReportService;

    @MockitoBean
    private BookingResourceService bookingResourceService;

    @MockitoBean
    private BookingAvailabilityPolicy bookingAvailabilityPolicy;

    @MockitoBean
    private ApplicationSupportService applicationSupportService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private LicenseService licenseService;

    @MockitoBean
    private CustomerService customerService;

    @org.junit.jupiter.api.BeforeEach
    void setUpLicense() {
        when(licenseService.isActivated()).thenReturn(true);
    }

    @Test
    void bookingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/booking/getAll"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required."))
                .andExpect(jsonPath("$.path").value("/api/v1/booking/getAll"));
    }

    @Test
    void forbiddenApiRequestsReturnConsistentErrorBody() throws Exception {
        mockMvc.perform(post("/api/v1/booking/save")
                    .with(user("guest-1").roles("GUEST"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/booking/save"));
    }

    @Test
    void resourceEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void licenseGateBlocksNonLicenseApisWhenNotActivated() throws Exception {
        when(licenseService.isActivated()).thenReturn(false);
        when(licenseService.getStatus()).thenReturn(
                new com.leonsoft.api.LicenseStatusResponse(false, "install-1", null, null, null));

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("License activation required. Please contact an administrator."));

        mockMvc.perform(get("/api/v1/license/status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/license/status").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void applicationSupportEndpointsAreAvailableToAuthenticatedUsers() throws Exception {
        when(applicationSupportService.appInfo())
                .thenReturn(new ApplicationSupportService.AppInfo("1.0.0", "standalone", true));
        when(applicationSupportService.diagnostics()).thenReturn("diagnostics");
        when(applicationSupportService.supportBundle()).thenReturn(new byte[] {80, 75, 3, 4});

        mockMvc.perform(get("/api/v1/app-info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/app-info").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));

        mockMvc.perform(get("/api/v1/app-info").with(user("guest").roles("GUEST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));

        mockMvc.perform(get("/api/v1/app-info").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));

        mockMvc.perform(get("/api/v1/diagnostics").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/diagnostics").with(user("guest").roles("GUEST")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/support-bundle").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void availabilitySettingsAreReturnedToAuthenticatedUsers() throws Exception {
        when(bookingAvailabilityPolicy.settings())
                .thenReturn(new BookingAvailabilityPolicy.AvailabilitySettings(true, false));

        mockMvc.perform(get("/api/v1/settings/availability")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomsPreventOverlap").value(true))
                .andExpect(jsonPath("$.carsPreventOverlap").value(false));
    }

    @Test
    void versionedCsrfEndpointRemainsAvailableToAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/api/v1/csrf").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void bookingsAreReadableByEveryAuthenticatedRole() throws Exception {
        when(bookingsService.getAll()).thenReturn(List.<Booking>of());

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("guest-1").roles("GUEST")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk());

    }

    @Test
    void resourceManagementRequiresAdminRole() throws Exception {
        when(bookingResourceService.getAll()).thenReturn(List.<BookingResource>of());

        // The resource catalog is readable by every authenticated role.
        mockMvc.perform(get("/api/v1/resources").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resources").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk());

        // Resource creation stays admin-only.
        mockMvc.perform(post("/api/v1/resources").with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"rooms\",\"name\":\"Suite 1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userAdministrationRequiresAdminRole() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(com.leonsoft.models.User.class))).thenAnswer(inv -> {
            com.leonsoft.models.User user = inv.getArgument(0);
            user.setId(42);
            return user;
        });

        // User management is admin-only.
        mockMvc.perform(get("/api/v1/users").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users").with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(post("/api/v1/users").with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"role\":\"USER\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void bookingWritesRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/booking/delete")
                        .with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"booking-1\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/booking/delete")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"booking-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelledBookingDeletionRequiresPostAndCsrfToken() throws Exception {
        mockMvc.perform(get("/api/v1/booking/del")
                        .with(user("operator").roles("OPERATOR"))
                        .param("bid", "booking-1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/booking/del")
                        .with(user("operator").roles("OPERATOR"))
                        .param("bid", "booking-1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/booking/del")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .param("bid", "booking-1"))
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
    void adminCanUpdateResourceButOperatorCannot() throws Exception {
        BookingResource updated = new BookingResource("r1", "rooms", "Suite 2");
        updated.setRentalTaxPerNight(new java.math.BigDecimal("2.00"));
        updated.setActive(true);
        when(bookingResourceService.update(any(String.class), any(com.leonsoft.api.BookingResourceUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/resources/r1")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Suite 2\",\"rentalTaxPerNight\":2.00,\"active\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/resources/r1")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Suite 2\",\"rentalTaxPerNight\":2.00,\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithCsrfReturnsNoContent() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void userResponsesDoNotExposePasswordHash() throws Exception {
        com.leonsoft.models.User existing = new com.leonsoft.models.User();
        existing.setId(7);
        existing.setUsername("alice");
        existing.setEmail("alice@test.com");
        existing.setRole("OPERATOR");
        existing.setActive(true);
        existing.setPasswordHash("$2a$10$secret");
        when(userRepository.findById(7)).thenReturn(java.util.Optional.of(existing));

        mockMvc.perform(get("/api/v1/users/7").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminCannotDeleteOwnAccount() throws Exception {
        com.leonsoft.models.User admin = new com.leonsoft.models.User();
        admin.setId(1);
        admin.setUsername("test-admin");
        admin.setRole("ADMIN");
        admin.setActive(true);
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(admin));

        mockMvc.perform(delete("/api/v1/users/1")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDeleteLastActiveAdmin() throws Exception {
        com.leonsoft.models.User admin = new com.leonsoft.models.User();
        admin.setId(2);
        admin.setUsername("other-admin");
        admin.setRole("ADMIN");
        admin.setActive(true);
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue("ADMIN")).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/users/2")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRejectsDuplicateUsername() throws Exception {
        com.leonsoft.models.User existing = new com.leonsoft.models.User();
        existing.setId(5);
        existing.setUsername("alice");
        existing.setRole("OPERATOR");
        existing.setActive(true);
        when(userRepository.findById(5)).thenReturn(java.util.Optional.of(existing));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        mockMvc.perform(put("/api/v1/users/5")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"role\":\"USER\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDemoteLastActiveAdmin() throws Exception {
        com.leonsoft.models.User admin = new com.leonsoft.models.User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setActive(true);
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue("ADMIN")).thenReturn(1L);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"role\":\"USER\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userUpdateRequiresAdminRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/5")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"role\":\"USER\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingMutationsAreAllowedForAdminAndOperatorButNotGuest() throws Exception {
        String bookingJson = """
                {"name":"Alice","numOfGuests":2,"agency":"direct","resourceId":"APT2","startDate":"20/06/2026","endDate":"23/06/2026","charge":100,"commission":0,"received":0,"status":"active"}
                """;
        when(bookingsService.create(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/booking/save")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking/save")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking/save")
                        .with(user("guest-1").roles("GUEST"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsAreAvailableToAdminAndOperatorButNotGuest() throws Exception {
        when(bookingReportService.report(any(), any(), any())).thenReturn("report");

        mockMvc.perform(get("/api/v1/booking/report")
                        .with(user("test-admin").roles("ADMIN"))
                        .param("fromDate", "01/06/2026")
                        .param("toDate", "30/06/2026"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking/report")
                        .with(user("operator").roles("OPERATOR"))
                        .param("fromDate", "01/06/2026")
                        .param("toDate", "30/06/2026"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/booking/report")
                        .with(user("guest-1").roles("GUEST"))
                        .param("fromDate", "01/06/2026")
                        .param("toDate", "30/06/2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentUserEndpointIsAvailableToAuthenticatedRoles() throws Exception {
        com.leonsoft.models.User me = new com.leonsoft.models.User();
        me.setId(9);
        me.setUsername("operator");
        me.setRole("OPERATOR");
        me.setActive(true);
        when(userRepository.findByUsername("operator")).thenReturn(java.util.Optional.of(me));

        mockMvc.perform(get("/api/v1/users/me").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operator"))
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    void licenseActivationIsAdminOnly() throws Exception {
        when(licenseService.activate(any())).thenReturn(new LicenseStatusResponse(true, "install-1", null, "2026-01-01T00:00:00", "admin"));

        mockMvc.perform(post("/api/v1/license/activate").with(csrf()).with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenseKey\":\"key\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/license/activate").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenseKey\":\"key\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void customerEndpointsAreAdminOnly() throws Exception {
        when(customerService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/customers").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/customers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/customers").with(csrf()).with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"telephone\":\"\",\"email\":\"\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void imagesArePublicStaticAssets() throws Exception {
        mockMvc.perform(get("/images/login-robot-greek-island.webp"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals(
                      "image/webp", result.getResponse().getContentType()));
    }

    @Test
    void activationMakesBlockedApisAvailable() throws Exception {
        when(licenseService.isActivated()).thenReturn(false);
        when(licenseService.getStatus()).thenReturn(new LicenseStatusResponse(false, "install-1", null, null, null));
        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        when(licenseService.isActivated()).thenReturn(true);
        when(bookingsService.getAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
