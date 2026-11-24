package com.leonsoft.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.leonsoft.config.LicenseEnforcementFilter;
import com.leonsoft.config.SecurityConfig;
import com.leonsoft.repositories.UserRepository;
import com.leonsoft.services.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LicenseController.class)
@Import({SecurityConfig.class, LicenseEnforcementFilter.class})
class LicenseControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LicenseService licenseService;
    @MockitoBean UserRepository userRepository;

    @BeforeEach
    void setUp() { when(licenseService.isActivated()).thenReturn(true); }

    @Test
    void statusRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/license/status")).andExpect(status().isUnauthorized());
    }

    @Test
    void activateValidatesPayload() throws Exception {
        mockMvc.perform(post("/api/v1/license/activate").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateReturnsStatusForAdmin() throws Exception {
        when(licenseService.activate(any())).thenReturn(new LicenseStatusResponse(true, "i1", null, "now", "admin"));
        mockMvc.perform(post("/api/v1/license/activate").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"licenseKey\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activated").value(true));
    }
}
