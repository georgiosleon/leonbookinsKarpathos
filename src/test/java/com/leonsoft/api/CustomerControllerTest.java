package com.leonsoft.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.leonsoft.config.LicenseEnforcementFilter;
import com.leonsoft.config.SecurityConfig;
import com.leonsoft.models.Customer;
import com.leonsoft.repositories.UserRepository;
import com.leonsoft.services.CustomerService;
import com.leonsoft.services.LicenseService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerController.class)
@Import({SecurityConfig.class, LicenseEnforcementFilter.class})
class CustomerControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerService customerService;
    @MockitoBean LicenseService licenseService;
    @MockitoBean UserRepository userRepository;

    @BeforeEach
    void setUp() { when(licenseService.isActivated()).thenReturn(true); }

    @Test
    void createValidatesName() throws Exception {
        mockMvc.perform(post("/api/v1/customers").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"telephone\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanCreateAndList() throws Exception {
        Customer c = new Customer(); c.setId(1); c.setName("Acme");
        when(customerService.getAll()).thenReturn(List.of(c));
        when(customerService.create(any())).thenReturn(c);

        mockMvc.perform(get("/api/v1/customers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Acme"));

        mockMvc.perform(post("/api/v1/customers").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"telephone\":\"+30\",\"email\":\"a@b.com\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void adminCanUpdateAndDelete() throws Exception {
        Customer c = new Customer(); c.setId(2); c.setName("New");
        when(customerService.update(eq(2), any())).thenReturn(c);

        mockMvc.perform(put("/api/v1/customers/2").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"telephone\":\"\",\"email\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New"));

        mockMvc.perform(delete("/api/v1/customers/2").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }
}
