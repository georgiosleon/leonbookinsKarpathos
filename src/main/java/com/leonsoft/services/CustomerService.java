package com.leonsoft.services;

import com.leonsoft.api.CustomerRequest;
import com.leonsoft.models.Customer;
import com.leonsoft.repositories.AppLicenseRepository;
import com.leonsoft.repositories.CustomerRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AppLicenseRepository appLicenseRepository;

    public CustomerService(CustomerRepository customerRepository, AppLicenseRepository appLicenseRepository) {
        this.customerRepository = customerRepository;
        this.appLicenseRepository = appLicenseRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAll() {
        return customerRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Customer getById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        if (customerRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only one customer is supported for this installation.");
        }
        Customer customer = new Customer();
        apply(customer, request);
        LocalDateTime now = LocalDateTime.now();
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Integer id, CustomerRequest request) {
        Customer customer = getById(id);
        apply(customer, request);
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Integer id) {
        Customer customer = getById(id);
        boolean licenseReferencesCustomer = appLicenseRepository.findById(1)
                .map(license -> license.getCustomer() != null && id.equals(license.getCustomer().getId()))
                .orElse(false);
        if (licenseReferencesCustomer) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the customer linked to the active license.");
        }
        customerRepository.delete(customer);
    }

    @Transactional
    public Customer upsertFromLicense(String name, String telephone, String email) {
        Customer customer = customerRepository.findAllByOrderByIdAsc().stream().findFirst().orElseGet(Customer::new);
        LocalDateTime now = LocalDateTime.now();
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(now);
        }
        customer.setName(name == null ? "" : name.trim());
        customer.setTelephone(trimToNull(telephone));
        customer.setEmail(trimToNull(email));
        customer.setUpdatedAt(now);
        return customerRepository.save(customer);
    }

    private static void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setTelephone(trimToNull(request.telephone()));
        customer.setEmail(trimToNull(request.email()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
