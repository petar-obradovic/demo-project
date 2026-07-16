package com.demo.store.application;

import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void givenUnitPriceAndQuantity_whenLineTotal_thenExactMoney() {
        assertThat(calculator.lineTotal(Money.of("19.99"), 3)).isEqualTo(Money.of("59.97"));
    }

    @Test
    void givenLines_whenOrderTotal_thenSumOfLineTotals() {
        OrderLine a = new OrderLine(ProductId.newId(), "A", Money.of("999.90"), 2, Money.of("1999.80"));
        OrderLine b = new OrderLine(ProductId.newId(), "B", Money.of("19.99"), 3, Money.of("59.97"));
        assertThat(calculator.orderTotal(List.of(a, b))).isEqualTo(Money.of("2059.77"));
    }

    @Test
    void givenNoLines_whenOrderTotal_thenZero() {
        assertThat(calculator.orderTotal(List.of())).isEqualTo(Money.zero());
    }
}
