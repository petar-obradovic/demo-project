package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newOrder() {
        OrderLine line = new OrderLine(ProductId.newId(), "Laptop",
                Money.of("999.90"), 2, Money.of("1999.80"));
        return Order.place(CustomerId.newId(), List.of(line), Money.of("1999.80"));
    }

    @Test
    void givenPlacedOrder_thenStatusNewWithTimestampAndTotal() {
        Order o = newOrder();
        assertThat(o.status()).isEqualTo(OrderStatus.NEW);
        assertThat(o.placedAt()).isNotNull();
        assertThat(o.total()).isEqualTo(Money.of("1999.80"));
    }

    @Test
    void givenNoLines_whenPlaced_thenThrows() {
        assertThatThrownBy(() -> Order.place(CustomerId.newId(), List.of(), Money.zero()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNewOrder_whenPaidShippedDelivered_thenWalksTheHappyPath() {
        Order o = newOrder();
        o.markPaid();
        assertThat(o.status()).isEqualTo(OrderStatus.PAID);
        o.markShipped();
        assertThat(o.status()).isEqualTo(OrderStatus.SHIPPED);
        o.markDelivered();
        assertThat(o.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void givenNewOrder_whenShippedOrDelivered_thenThrows() {
        assertThatThrownBy(() -> newOrder().markShipped())
                .isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(() -> newOrder().markDelivered())
                .isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void givenPaidOrder_whenPaidAgainOrDelivered_thenThrows() {
        Order paid = newOrder();
        paid.markPaid();
        assertThatThrownBy(paid::markPaid).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(paid::markDelivered).isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void givenDeliveredOrder_whenAnyTransition_thenThrows() {
        Order o = newOrder();
        o.markPaid();
        o.markShipped();
        o.markDelivered();
        assertThatThrownBy(o::markPaid).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(o::markShipped).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(o::markDelivered).isInstanceOf(IllegalOrderStateException.class);
    }
}
