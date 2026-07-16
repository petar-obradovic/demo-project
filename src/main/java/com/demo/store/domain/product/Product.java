package com.demo.store.domain.product;

import com.demo.store.domain.shared.Money;

import java.util.Objects;

public class Product {

    private final ProductId id;
    private final String sku;
    private String name;
    private String description;
    private Money price;
    private int stockQuantity;
    private boolean active;

    public Product(ProductId id, String sku, String name, String description,
                   Money price, int stockQuantity, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.sku = requireNonBlank(sku, "sku");
        this.name = requireNonBlank(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.price = requireNonNegative(price);
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must be >= 0");
        }
        this.stockQuantity = stockQuantity;
        this.active = active;
    }

    public static Product create(String sku, String name, String description,
                                 Money price, int initialStock) {
        return new Product(ProductId.newId(), sku, name, description, price, initialStock, true);
    }

    public void changePrice(Money newPrice) {
        this.price = requireNonNegative(newPrice);
    }

    public void decreaseStock(int quantity) {
        requirePositive(quantity);
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(id, quantity, stockQuantity);
        }
        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        requirePositive(quantity);
        stockQuantity += quantity;
    }

    public boolean canFulfill(int quantity) {
        return quantity <= stockQuantity;
    }

    public void deactivate() {
        this.active = false;
    }

    public ProductId id() { return id; }
    public String sku() { return sku; }
    public String name() { return name; }
    public String description() { return description; }
    public Money price() { return price; }
    public int stockQuantity() { return stockQuantity; }
    public boolean active() { return active; }

    private static Money requireNonNegative(Money money) {
        Objects.requireNonNull(money, "price");
        if (money.isNegative()) {
            throw new IllegalArgumentException("price must not be negative");
        }
        return money;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
