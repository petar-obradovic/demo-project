package com.demo.store.domain.order;

import java.util.Objects;
import java.util.UUID;

public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID().toString());
    }
}
