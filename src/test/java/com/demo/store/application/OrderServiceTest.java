package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.order.OrderStatus;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

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

    private Product product(ProductId id, int stock) {
        return new Product(id, "SKU-" + id.value(), "Product", "", Money.of("10.00"), stock, true);
    }

    @Test
    void givenNewOrder_whenCancel_thenCancelledAndStockRestored() {
        Order order = newOrder();
        ProductId pid = order.lines().get(0).productId();
        Product product = product(pid, 5);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(pid)).thenReturn(Optional.of(product));

        Order cancelled = orderService.cancel(order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.stockQuantity()).isEqualTo(6);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void givenPaidOrder_whenCancel_thenCancelledAndStockRestored() {
        Order order = newOrder();
        order.markPaid();
        ProductId pid = order.lines().get(0).productId();
        Product product = product(pid, 3);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(pid)).thenReturn(Optional.of(product));

        Order cancelled = orderService.cancel(order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.stockQuantity()).isEqualTo(4);
    }

    @Test
    void givenOrderWithMultipleLines_whenCancel_thenStockRestoredForAllLines() {
        ProductId pid1 = ProductId.newId();
        ProductId pid2 = ProductId.newId();
        OrderLine line1 = new OrderLine(pid1, "Widget", Money.of("5.00"), 2, Money.of("10.00"));
        OrderLine line2 = new OrderLine(pid2, "Gadget", Money.of("20.00"), 3, Money.of("60.00"));
        Order order = Order.place(CustomerId.newId(), List.of(line1, line2), Money.of("70.00"));
        Product p1 = product(pid1, 10);
        Product p2 = product(pid2, 7);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(pid1)).thenReturn(Optional.of(p1));
        when(productRepository.findById(pid2)).thenReturn(Optional.of(p2));

        orderService.cancel(order.id());

        assertThat(p1.stockQuantity()).isEqualTo(12);
        assertThat(p2.stockQuantity()).isEqualTo(10);
        verify(productRepository, times(2)).save(any());
    }

    @Test
    void givenUnknownOrder_whenCancel_thenNotFound() {
        OrderId id = OrderId.newId();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void givenShippedOrder_whenCancel_thenThrowsIllegalOrderState() {
        Order order = newOrder();
        order.markPaid();
        order.markShipped();
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(order.id()))
                .isInstanceOf(com.demo.store.domain.order.IllegalOrderStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenCancelledOrder_whenCancel_thenThrowsIllegalOrderState() {
        Order order = newOrder();
        order.cancel();
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(order.id()))
                .isInstanceOf(com.demo.store.domain.order.IllegalOrderStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenOrderWithMissingProduct_whenCancel_thenCancelSucceedsAndOtherStockRestored() {
        ProductId missingId = ProductId.newId();
        ProductId presentId = ProductId.newId();
        OrderLine line1 = new OrderLine(missingId, "Gone", Money.of("1.00"), 1, Money.of("1.00"));
        OrderLine line2 = new OrderLine(presentId, "Here", Money.of("2.00"), 4, Money.of("8.00"));
        Order order = Order.place(CustomerId.newId(), List.of(line1, line2), Money.of("9.00"));
        Product present = product(presentId, 0);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());
        when(productRepository.findById(presentId)).thenReturn(Optional.of(present));

        Order cancelled = orderService.cancel(order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(present.stockQuantity()).isEqualTo(4);
        verify(productRepository, times(1)).save(present);
    }
}
