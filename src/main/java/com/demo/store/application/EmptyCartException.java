package com.demo.store.application;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException(String customerId) {
        super("Cart for customer %s is empty".formatted(customerId));
    }
}
