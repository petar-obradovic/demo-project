package com.demo.store.domain.product;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(ProductId productId, int requested, int available) {
        super("Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId.value(), requested, available));
    }
}
