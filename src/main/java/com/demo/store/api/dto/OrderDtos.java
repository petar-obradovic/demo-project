package com.demo.store.api.dto;

import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderLineResponse(String productId, String name, BigDecimal unitPrice,
                                    int quantity, BigDecimal lineTotal, String currency) {

        public static OrderLineResponse from(OrderLine line) {
            return new OrderLineResponse(
                    line.productId().value(),
                    line.name(),
                    line.unitPrice().amount(),
                    line.quantity(),
                    line.lineTotal().amount(),
                    line.lineTotal().currency().getCurrencyCode());
        }
    }

    public record OrderResponse(String id, String customerId, List<OrderLineResponse> lines,
                                BigDecimal total, String currency, String status,
                                Instant placedAt) {

        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.id().value(),
                    order.customerId().value(),
                    order.lines().stream().map(OrderLineResponse::from).toList(),
                    order.total().amount(),
                    order.total().currency().getCurrencyCode(),
                    order.status().name(),
                    order.placedAt());
        }
    }
}
