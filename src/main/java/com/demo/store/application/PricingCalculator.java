package com.demo.store.application;

import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computes line and order totals. Kept as a named component: pricing is the
 * seam future features (discounts, taxes) will extend.
 */
@Component
public class PricingCalculator {

    public Money lineTotal(Money unitPrice, int quantity) {
        return unitPrice.multiply(quantity);
    }

    public Money orderTotal(List<OrderLine> lines) {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.zero(), Money::add);
    }
}
