package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private final CustomerId customerId = CustomerId.newId();
    private Product laptop;

    @BeforeEach
    void setUp() {
        laptop = Product.create("SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);
        lenient().when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenNoCart_whenGetOrCreate_thenNewCartSaved() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        Cart cart = cartService.getOrCreateCart(customerId);

        assertThat(cart.customerId()).isEqualTo(customerId);
        assertThat(cart.items()).isEmpty();
    }

    @Test
    void givenActiveProduct_whenAddItem_thenSnapshotStored() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));

        Cart cart = cartService.addItem(customerId, laptop.id(), 2);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).name()).isEqualTo("Laptop");
        assertThat(cart.items().get(0).unitPrice()).isEqualTo(Money.of("999.90"));
    }

    @Test
    void givenUnknownProduct_whenAddItem_thenNotFound() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(customerId, laptop.id(), 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void givenInactiveProduct_whenAddItem_thenRejected() {
        laptop.deactivate();
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));

        assertThatThrownBy(() -> cartService.addItem(customerId, laptop.id(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
