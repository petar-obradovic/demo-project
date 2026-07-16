package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Cart getOrCreateCart(CustomerId customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.createFor(customerId)));
    }

    public Cart addItem(CustomerId customerId, ProductId productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId.value()));
        if (!product.active()) {
            throw new IllegalArgumentException(
                    "Product %s is not available".formatted(productId.value()));
        }
        cart.addItem(product.id(), product.name(), product.price(), quantity);
        return cartRepository.save(cart);
    }

    public Cart changeQuantity(CustomerId customerId, ProductId productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        cart.changeQuantity(productId, quantity);
        return cartRepository.save(cart);
    }

    public Cart removeItem(CustomerId customerId, ProductId productId) {
        Cart cart = getOrCreateCart(customerId);
        cart.removeItem(productId);
        return cartRepository.save(cart);
    }
}
