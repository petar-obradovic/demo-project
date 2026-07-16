package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final ProductId laptop = ProductId.newId();
    private final ProductId mouse = ProductId.newId();

    private Cart cart() {
        return Cart.createFor(CustomerId.newId());
    }

    @Test
    void givenEmptyCart_whenItemAdded_thenContainsItem() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        assertThat(c.items()).hasSize(1);
        assertThat(c.items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void givenItemInCart_whenSameProductAdded_thenQuantitiesMerge() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.addItem(laptop, "Laptop", Money.of("999.90"), 2);
        assertThat(c.items()).hasSize(1);
        assertThat(c.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void givenZeroQuantity_whenAdded_thenThrows() {
        assertThatThrownBy(() -> cart().addItem(laptop, "Laptop", Money.of("1.00"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenItemInCart_whenQuantityChanged_thenUpdated() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.changeQuantity(laptop, 5);
        assertThat(c.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void givenProductNotInCart_whenQuantityChanged_thenThrows() {
        assertThatThrownBy(() -> cart().changeQuantity(mouse, 2))
                .isInstanceOf(ItemNotInCartException.class);
    }

    @Test
    void givenItemInCart_whenRemoved_thenGone() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.removeItem(laptop);
        assertThat(c.items()).isEmpty();
    }

    @Test
    void givenProductNotInCart_whenRemoved_thenThrows() {
        assertThatThrownBy(() -> cart().removeItem(mouse))
                .isInstanceOf(ItemNotInCartException.class);
    }

    @Test
    void givenItems_whenTotal_thenSumInMoney() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 2);
        c.addItem(mouse, "Mouse", Money.of("19.99"), 3);
        assertThat(c.total()).isEqualTo(Money.of("2059.77"));
    }

    @Test
    void givenItems_whenCleared_thenEmpty() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.clear();
        assertThat(c.items()).isEmpty();
    }
}
