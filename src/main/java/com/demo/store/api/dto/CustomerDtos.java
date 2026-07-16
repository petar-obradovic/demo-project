package com.demo.store.api.dto;

import com.demo.store.domain.customer.Customer;
import jakarta.validation.constraints.NotBlank;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record RegisterCustomerRequest(
            @NotBlank String name,
            @NotBlank String email,
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String zip,
            @NotBlank String country) {
    }

    public record CustomerResponse(String id, String name, String email,
                                   String street, String city, String zip, String country) {

        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(
                    customer.id().value(),
                    customer.name(),
                    customer.email().value(),
                    customer.shippingAddress().street(),
                    customer.shippingAddress().city(),
                    customer.shippingAddress().zip(),
                    customer.shippingAddress().country());
        }
    }
}
