package com.demo.store.domain.product;

import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product laptop() {
        return Product.create("SKU-1", "Laptop", "13-inch laptop", Money.of("999.90"), 10);
    }

    @Test
    void givenNewProduct_whenCreated_thenActiveWithIdAndStock() {
        Product p = laptop();
        assertThat(p.id()).isNotNull();
        assertThat(p.active()).isTrue();
        assertThat(p.stockQuantity()).isEqualTo(10);
    }

    @Test
    void givenNegativePrice_whenCreated_thenThrows() {
        assertThatThrownBy(() -> Product.create("SKU-2", "X", "d", Money.of("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNegativeStock_whenCreated_thenThrows() {
        assertThatThrownBy(() -> Product.create("SKU-2", "X", "d", Money.of("1.00"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenEnoughStock_whenDecreased_thenReduced() {
        Product p = laptop();
        p.decreaseStock(4);
        assertThat(p.stockQuantity()).isEqualTo(6);
    }

    @Test
    void givenTooLittleStock_whenDecreased_thenThrowsAndUnchanged() {
        Product p = laptop();
        assertThatThrownBy(() -> p.decreaseStock(11))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(p.stockQuantity()).isEqualTo(10);
    }

    @Test
    void givenProduct_whenStockIncreased_thenAdded() {
        Product p = laptop();
        p.increaseStock(5);
        assertThat(p.stockQuantity()).isEqualTo(15);
    }

    @Test
    void givenNegativePrice_whenPriceChanged_thenThrows() {
        Product p = laptop();
        assertThatThrownBy(() -> p.changePrice(Money.of("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenProduct_whenDeactivated_thenInactive() {
        Product p = laptop();
        p.deactivate();
        assertThat(p.active()).isFalse();
    }

    @Test
    void givenNonPositiveQuantity_whenStockChanged_thenThrows() {
        Product p = laptop();
        assertThatThrownBy(() -> p.decreaseStock(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.decreaseStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.increaseStock(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenExactStock_whenDecreased_thenZero() {
        Product p = laptop();
        p.decreaseStock(10);
        assertThat(p.stockQuantity()).isEqualTo(0);
    }

    @Test
    void givenProduct_whenCanFulfillChecked_thenReflectsStock() {
        Product p = laptop();
        assertThat(p.canFulfill(10)).isTrue();
        assertThat(p.canFulfill(11)).isFalse();
    }

    @Test
    void givenNonNegativePrice_whenPriceChanged_thenUpdated() {
        Product p = laptop();
        p.changePrice(Money.of("1234.56"));
        assertThat(p.price()).isEqualTo(Money.of("1234.56"));
    }
}
