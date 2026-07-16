package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.InsufficientStockException;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    private CheckoutService checkoutService;

    private final CustomerId customerId = CustomerId.newId();
    private Product laptop;
    private Product mouse;
    private Cart cart;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                cartRepository, productRepository, orderRepository, new PricingCalculator());
        laptop = Product.create("SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);
        mouse = Product.create("SKU-2", "Mouse", "wireless", Money.of("19.99"), 5);
        cart = Cart.createFor(customerId);
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenFilledCart_whenCheckout_thenOrderPlacedStockReducedCartCleared() {
        cart.addItem(laptop.id(), laptop.name(), laptop.price(), 2);
        cart.addItem(mouse.id(), mouse.name(), mouse.price(), 3);
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));
        when(productRepository.findById(mouse.id())).thenReturn(Optional.of(mouse));

        Order order = checkoutService.checkout(customerId);

        assertThat(order.total()).isEqualTo(Money.of("2059.77"));
        assertThat(order.lines()).hasSize(2);
        assertThat(laptop.stockQuantity()).isEqualTo(8);
        assertThat(mouse.stockQuantity()).isEqualTo(2);
        assertThat(cart.items()).isEmpty();
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void givenNoCart_whenCheckout_thenNotFound() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void givenEmptyCart_whenCheckout_thenEmptyCartException() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void givenShortStock_whenCheckout_thenNothingPersistedAndNoStockTouched() {
        cart.addItem(laptop.id(), laptop.name(), laptop.price(), 2);
        cart.addItem(mouse.id(), mouse.name(), mouse.price(), 6); // only 5 in stock
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));
        when(productRepository.findById(mouse.id())).thenReturn(Optional.of(mouse));

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(laptop.stockQuantity()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }
}
