package com.demo.store.api.dto;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotBlank String productId, @Min(1) int quantity) {
    }

    public record ChangeQuantityRequest(@Min(1) int quantity) {
    }

    public record CartItemResponse(String productId, String name, BigDecimal unitPrice,
                                   String currency, int quantity) {

        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.productId().value(),
                    item.name(),
                    item.unitPrice().amount(),
                    item.unitPrice().currency().getCurrencyCode(),
                    item.quantity());
        }
    }

    // legacy: total is a double via Cart.getTotal() — kept for old integrations
    public record CartResponse(String id, String customerId,
                               List<CartItemResponse> items, double total) {

        public static CartResponse from(Cart cart) {
            return new CartResponse(
                    cart.id().value(),
                    cart.customerId().value(),
                    cart.items().stream().map(CartItemResponse::from).toList(),
                    cart.getTotal());
        }
    }
}
