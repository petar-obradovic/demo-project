package com.demo.store.domain.cart;

import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.Objects;

/** Snapshot of a product at the moment it entered the cart. */
public record CartItem(ProductId productId, String name, Money unitPrice, int quantity) {

    public CartItem {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
    }

    CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, name, unitPrice, newQuantity);
    }
}
