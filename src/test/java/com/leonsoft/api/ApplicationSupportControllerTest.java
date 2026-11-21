package com.leonsoft.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leonsoft.services.ApplicationSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApplicationSupportControllerTest {
    private MockMvc mockMvc;
    private ApplicationSupportService supportService;

    @BeforeEach
    void setUp() {
        supportService = org.mockito.Mockito.mock(ApplicationSupportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
              new ApplicationSupportController(supportService)).build();
    }

    @Test
    void returnsVersionAndDeploymentInformation() throws Exception {
        when(supportService.appInfo())
              .thenReturn(new ApplicationSupportService.AppInfo("1.2.3", "desktop"));

        mockMvc.perform(get("/api/v1/app-info"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.version").value("1.2.3"))
              .andExpect(jsonPath("$.deploymentType").value("desktop"));
    }

    @Test
    void downloadsUtf8DiagnosticsAsAnAttachment() throws Exception {
        when(supportService.diagnostics()).thenReturn("Version: 1.2.3\nΈκδοση");

        mockMvc.perform(get("/api/v1/diagnostics"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(
                    new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8)))
              .andExpect(header().string(
                    "Content-Disposition", "attachment; filename=leon-bookings-diagnostics.txt"))
              .andExpect(content().string("Version: 1.2.3\nΈκδοση"));
    }
}
