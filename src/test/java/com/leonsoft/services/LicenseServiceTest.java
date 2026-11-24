package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.leonsoft.api.LicenseActivateRequest;
import com.leonsoft.api.LicenseStatusResponse;
import com.leonsoft.models.AppInstallation;
import com.leonsoft.models.AppLicense;
import com.leonsoft.models.Customer;
import com.leonsoft.models.User;
import com.leonsoft.repositories.AppInstallationRepository;
import com.leonsoft.repositories.AppLicenseRepository;
import com.leonsoft.services.licensing.LicensePayload;
import com.leonsoft.services.licensing.LicenseVerifier;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @Mock private AppInstallationRepository installationRepository;
    @Mock private AppLicenseRepository licenseRepository;
    @Mock private CustomerService customerService;
    @Mock private LicenseVerifier licenseVerifier;
    @Mock private CurrentUserProvider currentUserProvider;

    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        licenseService = new LicenseService(installationRepository, licenseRepository, customerService, licenseVerifier, currentUserProvider);
    }

    @Test
    void getOrCreateInstallIdCreatesWhenMissing() {
        when(installationRepository.findById(1)).thenReturn(Optional.empty());
        when(installationRepository.save(any(AppInstallation.class))).thenAnswer(inv -> inv.getArgument(0));

        String installId = licenseService.getOrCreateInstallId();

        assertNotNull(installId);
        verify(installationRepository).save(argThat(i -> i.getId() == 1 && installId.equals(i.getInstallId())));
    }

    @Test
    void getStatusReturnsInactiveWhenNoLicense() {
        AppInstallation inst = new AppInstallation();
        inst.setId(1);
        inst.setInstallId("install-1");
        when(installationRepository.findById(1)).thenReturn(Optional.of(inst));
        when(licenseRepository.findById(1)).thenReturn(Optional.empty());

        LicenseStatusResponse status = licenseService.getStatus();

        assertFalse(status.activated());
        assertEquals("install-1", status.installId());
        assertNull(status.customer());
    }

    @Test
    void activateVerifiesStoresAndCaches() {
        AppInstallation inst = new AppInstallation();
        inst.setId(1);
        inst.setInstallId("install-1");
        when(installationRepository.findById(1)).thenReturn(Optional.of(inst));
        when(licenseVerifier.verify("key", "install-1")).thenReturn(new LicensePayload("leonbookinsKarpathos", "lic-1", "install-1", "Acme", "+30", "a@b.com", "2026-01-01T00:00:00Z"));
        Customer customer = new Customer();
        customer.setId(7);
        customer.setName("Acme");
        when(customerService.upsertFromLicense("Acme", "+30", "a@b.com")).thenReturn(customer);
        User admin = new User();
        admin.setUsername("admin");
        when(currentUserProvider.getCurrentUser()).thenReturn(Optional.of(admin));
        when(licenseRepository.findById(1)).thenReturn(Optional.empty(), Optional.of(activatedLicense(customer)));
        when(licenseRepository.save(any(AppLicense.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenseStatusResponse status = licenseService.activate(new LicenseActivateRequest("key"));

        assertTrue(status.activated());
        assertEquals("Acme", status.customer().name());
        verify(licenseRepository).save(argThat(l -> "ACTIVE".equals(l.getStatus()) && "admin".equals(l.getActivatedBy())));
        assertTrue(licenseService.isActivated());
    }

    private static AppLicense activatedLicense(Customer customer) {
        AppLicense license = new AppLicense();
        license.setId(1);
        license.setStatus("ACTIVE");
        license.setCustomer(customer);
        license.setActivatedAt(LocalDateTime.now());
        license.setActivatedBy("admin");
        return license;
    }
}
