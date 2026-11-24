package com.leonsoft.services;

import com.leonsoft.api.CustomerResponse;
import com.leonsoft.api.LicenseActivateRequest;
import com.leonsoft.api.LicenseStatusResponse;
import com.leonsoft.models.AppInstallation;
import com.leonsoft.models.AppLicense;
import com.leonsoft.models.Customer;
import com.leonsoft.repositories.AppInstallationRepository;
import com.leonsoft.repositories.AppLicenseRepository;
import com.leonsoft.services.licensing.LicensePayload;
import com.leonsoft.services.licensing.LicenseVerifier;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LicenseService {

    private final AppInstallationRepository installationRepository;
    private final AppLicenseRepository licenseRepository;
    private final CustomerService customerService;
    private final LicenseVerifier licenseVerifier;
    private final CurrentUserProvider currentUserProvider;
    private volatile Boolean activatedCache;

    public LicenseService(
            AppInstallationRepository installationRepository,
            AppLicenseRepository licenseRepository,
            CustomerService customerService,
            LicenseVerifier licenseVerifier,
            CurrentUserProvider currentUserProvider) {
        this.installationRepository = installationRepository;
        this.licenseRepository = licenseRepository;
        this.customerService = customerService;
        this.licenseVerifier = licenseVerifier;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public String getOrCreateInstallId() {
        return installationRepository.findById(1)
                .map(AppInstallation::getInstallId)
                .orElseGet(() -> {
                    AppInstallation installation = new AppInstallation();
                    installation.setId(1);
                    installation.setInstallId(UUID.randomUUID().toString());
                    installation.setCreatedAt(LocalDateTime.now());
                    return installationRepository.save(installation).getInstallId();
                });
    }

    @Transactional
    public LicenseStatusResponse getStatus() {
        String installId = getOrCreateInstallId();
        AppLicense license = licenseRepository.findById(1).orElse(null);
        boolean activated = license != null && "ACTIVE".equalsIgnoreCase(license.getStatus());
        CustomerResponse customer = activated && license.getCustomer() != null
                ? CustomerResponse.from(license.getCustomer())
                : null;
        return new LicenseStatusResponse(
                activated,
                installId,
                customer,
                activated && license.getActivatedAt() != null ? license.getActivatedAt().toString() : null,
                activated ? license.getActivatedBy() : null
        );
    }

    @Transactional(readOnly = true)
    public boolean isActivated() {
        Boolean cached = activatedCache;
        if (cached != null) {
            return cached;
        }
        boolean activated = licenseRepository.findById(1)
                .map(license -> "ACTIVE".equalsIgnoreCase(license.getStatus()))
                .orElse(false);
        activatedCache = activated;
        return activated;
    }

    @Transactional
    public LicenseStatusResponse activate(LicenseActivateRequest request) {
        String installId = getOrCreateInstallId();
        LicensePayload payload = licenseVerifier.verify(request.licenseKey(), installId);

        Customer customer = customerService.upsertFromLicense(
                payload.customerName(),
                payload.customerTelephone(),
                payload.customerEmail());

        String activatedBy = currentUserProvider.getCurrentUser()
                .map(user -> user.getUsername())
                .orElse("unknown");

        AppLicense license = licenseRepository.findById(1).orElseGet(AppLicense::new);
        license.setId(1);
        license.setInstallId(installId);
        license.setCustomer(customer);
        license.setSignedLicense(request.licenseKey().trim());
        license.setActivatedAt(LocalDateTime.now());
        license.setActivatedBy(activatedBy);
        license.setStatus("ACTIVE");
        licenseRepository.save(license);
        activatedCache = true;

        return getStatus();
    }
}
