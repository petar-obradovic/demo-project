package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.order.OrderStatus;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order newOrder() {
        OrderLine line = new OrderLine(ProductId.newId(), "Laptop",
                Money.of("999.90"), 1, Money.of("999.90"));
        return Order.place(CustomerId.newId(), List.of(line), Money.of("999.90"));
    }

    @Test
    void givenNewOrder_whenPay_thenPaidAndSaved() {
        Order order = newOrder();
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order paid = orderService.pay(order.id());

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void givenUnknownOrder_whenPay_thenNotFound() {
        OrderId id = OrderId.newId();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(id))
                .isInstanceOf(NotFoundException.class);
    }
}
