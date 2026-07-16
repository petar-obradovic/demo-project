package com.demo.store.domain.cart;

import com.demo.store.domain.product.ProductId;

public class ItemNotInCartException extends RuntimeException {

    public ItemNotInCartException(ProductId productId) {
        super("Product %s is not in the cart".formatted(productId.value()));
    }
}
