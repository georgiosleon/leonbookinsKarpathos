package com.leonsoft.api;

import com.leonsoft.models.Customer;

public record CustomerResponse(
        Integer id,
        String name,
        String telephone,
        String email
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getTelephone(), customer.getEmail());
    }
}
