package com.demo.store.domain.order;

public class IllegalOrderStateException extends RuntimeException {

    public IllegalOrderStateException(OrderStatus current, String attempted) {
        super("Cannot %s an order in status %s".formatted(attempted, current));
    }
}
