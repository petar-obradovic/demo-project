package com.demo.store.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** House representation of money: BigDecimal, scale 2, HALF_UP, EUR by default. */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, EUR);
    }

    public static Money zero() {
        return of(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    public Money percentage(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
