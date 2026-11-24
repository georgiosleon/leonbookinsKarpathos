package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.leonsoft.api.CustomerRequest;
import com.leonsoft.models.AppLicense;
import com.leonsoft.models.Customer;
import com.leonsoft.repositories.AppLicenseRepository;
import com.leonsoft.repositories.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AppLicenseRepository appLicenseRepository;
    @Mock private AuditService auditService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, appLicenseRepository, auditService);
    }

    @Test
    void createRejectsSecondCustomer() {
        when(customerRepository.count()).thenReturn(1L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> customerService.create(new CustomerRequest("Acme", null, null)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void deleteRejectsCustomerLinkedToLicense() {
        Customer customer = new Customer();
        customer.setId(5);
        when(customerRepository.findById(5)).thenReturn(Optional.of(customer));
        AppLicense license = new AppLicense();
        license.setCustomer(customer);
        when(appLicenseRepository.findById(1)).thenReturn(Optional.of(license));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> customerService.delete(5));
        assertEquals(409, ex.getStatusCode().value());
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void upsertFromLicenseCreatesOrUpdatesSingleRecord() {
        when(customerRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        Customer result = customerService.upsertFromLicense(" Acme ", " +30 ", " a@b.com ");

        assertEquals("Acme", result.getName());
        assertEquals("+30", result.getTelephone());
        assertEquals("a@b.com", result.getEmail());
        assertNotNull(result.getCreatedAt());
    }
}
