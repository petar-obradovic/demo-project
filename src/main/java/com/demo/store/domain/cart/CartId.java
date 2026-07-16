package com.demo.store.domain.cart;

import java.util.Objects;
import java.util.UUID;

public record CartId(String value) {

    public CartId {
        Objects.requireNonNull(value, "value");
    }

    public static CartId newId() {
        return new CartId(UUID.randomUUID().toString());
    }
}
