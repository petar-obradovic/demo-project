package com.demo.store.domain.customer;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
    }

    public static CustomerId newId() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
