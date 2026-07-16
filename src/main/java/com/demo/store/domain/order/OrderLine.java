package com.demo.store.domain.order;

import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.Objects;

/** Immutable snapshot of one cart line at checkout time. */
public record OrderLine(ProductId productId, String name, Money unitPrice,
                        int quantity, Money lineTotal) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(lineTotal, "lineTotal");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
    }
}
