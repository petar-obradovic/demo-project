package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void givenUnscaledAmount_whenConstructed_thenNormalizedToScale2HalfUp() {
        assertThat(Money.of("10.005").amount()).isEqualByComparingTo(new BigDecimal("10.01"));
        assertThat(Money.of("10").amount().scale()).isEqualTo(2);
    }

    @Test
    void givenTwoAmounts_whenAdded_thenSumsExactly() {
        assertThat(Money.of("19.99").add(Money.of("0.01"))).isEqualTo(Money.of("20.00"));
    }

    @Test
    void givenAmount_whenSubtracted_thenDifferenceExact() {
        assertThat(Money.of("20.00").subtract(Money.of("0.01"))).isEqualTo(Money.of("19.99"));
    }

    @Test
    void givenUnitPrice_whenMultipliedByQuantity_thenNoDrift() {
        assertThat(Money.of("19.99").multiply(3)).isEqualTo(Money.of("59.97"));
    }

    @Test
    void givenAmount_whenPercentageTaken_thenRoundedHalfUp() {
        assertThat(Money.of("59.97").percentage(new BigDecimal("0.10"))).isEqualTo(Money.of("6.00"));
    }

    @Test
    void givenNegativeAmount_whenIsNegative_thenTrue() {
        assertThat(Money.of("-0.01").isNegative()).isTrue();
        assertThat(Money.zero().isNegative()).isFalse();
    }

    @Test
    void givenDifferentCurrencies_whenAdded_thenThrows() {
        Money eur = Money.of("1.00");
        Money usd = new Money(new BigDecimal("1.00"), java.util.Currency.getInstance("USD"));
        assertThatThrownBy(() -> eur.add(usd)).isInstanceOf(IllegalArgumentException.class);
    }
}
