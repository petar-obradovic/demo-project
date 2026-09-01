package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.shared.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Money total;
    private OrderStatus status;
    private final Instant placedAt;

    public Order(OrderId id, CustomerId customerId, List<OrderLine> lines,
                 Money total, OrderStatus status, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must have at least one line");
        }
        this.lines = List.copyOf(lines);
        this.total = Objects.requireNonNull(total, "total");
        this.status = Objects.requireNonNull(status, "status");
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    public static Order place(CustomerId customerId, List<OrderLine> lines, Money total) {
        return new Order(OrderId.newId(), customerId, lines, total,
                OrderStatus.NEW, Instant.now());
    }

    public void markPaid() {
        requireStatus(OrderStatus.NEW, "pay");
        status = OrderStatus.PAID;
    }

    public void markShipped() {
        requireStatus(OrderStatus.PAID, "ship");
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        requireStatus(OrderStatus.SHIPPED, "deliver");
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status != OrderStatus.NEW && status != OrderStatus.PAID) {
            throw new IllegalOrderStateException(status, "cancel");
        }
        status = OrderStatus.CANCELLED;
    }

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public List<OrderLine> lines() { return lines; }
    public Money total() { return total; }
    public OrderStatus status() { return status; }
    public Instant placedAt() { return placedAt; }

    private void requireStatus(OrderStatus expected, String attempted) {
        if (status != expected) {
            throw new IllegalOrderStateException(status, attempted);
        }
    }
}
