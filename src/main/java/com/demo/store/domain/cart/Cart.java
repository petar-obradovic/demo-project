package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cart {

    private final CartId id;
    private final CustomerId customerId;
    private final List<CartItem> items;

    public Cart(CartId id, CustomerId customerId, List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
    }

    public static Cart createFor(CustomerId customerId) {
        return new Cart(CartId.newId(), customerId, new ArrayList<>());
    }

    public void addItem(ProductId productId, String name, Money unitPrice, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        int existing = indexOf(productId);
        if (existing >= 0) {
            CartItem item = items.get(existing);
            items.set(existing, item.withQuantity(item.quantity() + quantity));
        } else {
            items.add(new CartItem(productId, name, unitPrice, quantity));
        }
    }

    public void changeQuantity(ProductId productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        int index = requireIndex(productId);
        items.set(index, items.get(index).withQuantity(quantity));
    }

    public void removeItem(ProductId productId) {
        items.remove(requireIndex(productId));
    }

    public Money total() {
        return items.stream()
                .map(item -> item.unitPrice().multiply(item.quantity()))
                .reduce(Money.zero(), Money::add);
    }

    // legacy: pre-Money convenience kept for old integrations — used by the cart API response
    public double getTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.unitPrice().amount().doubleValue() * item.quantity();
        }
        return total;
    }

    public void clear() {
        items.clear();
    }

    public CartId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public List<CartItem> items() { return List.copyOf(items); }

    private int indexOf(ProductId productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    private int requireIndex(ProductId productId) {
        int index = indexOf(productId);
        if (index < 0) {
            throw new ItemNotInCartException(productId);
        }
        return index;
    }
}
