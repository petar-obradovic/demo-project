package com.demo.store.domain.customer;

import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;

import java.util.Objects;

public class Customer {

    private final CustomerId id;
    private final String name;
    private final Email email;
    private final Address shippingAddress;

    public Customer(CustomerId id, String name, Email email, Address shippingAddress) {
        this.id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;
        this.email = Objects.requireNonNull(email, "email");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
    }

    public static Customer register(String name, Email email, Address shippingAddress) {
        return new Customer(CustomerId.newId(), name, email, shippingAddress);
    }

    public CustomerId id() { return id; }
    public String name() { return name; }
    public Email email() { return email; }
    public Address shippingAddress() { return shippingAddress; }
}
