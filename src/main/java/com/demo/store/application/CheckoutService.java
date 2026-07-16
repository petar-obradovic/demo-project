package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartItem;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.InsufficientStockException;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PricingCalculator pricingCalculator;

    public CheckoutService(CartRepository cartRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           PricingCalculator pricingCalculator) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.pricingCalculator = pricingCalculator;
    }

    public Order checkout(CustomerId customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cart for customer", customerId.value()));
        if (cart.items().isEmpty()) {
            throw new EmptyCartException(customerId.value());
        }

        // Pass 1: load and verify ALL stock before touching anything.
        Map<ProductId, Product> products = new HashMap<>();
        for (CartItem item : cart.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new NotFoundException("Product", item.productId().value()));
            if (!product.canFulfill(item.quantity())) {
                throw new InsufficientStockException(
                        product.id(), item.quantity(), product.stockQuantity());
            }
            products.put(product.id(), product);
        }

        // Pass 2: apply — decrease stock, build order lines from cart snapshots.
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : cart.items()) {
            Product product = products.get(item.productId());
            product.decreaseStock(item.quantity());
            productRepository.save(product);
            lines.add(new OrderLine(item.productId(), item.name(), item.unitPrice(),
                    item.quantity(), pricingCalculator.lineTotal(item.unitPrice(), item.quantity())));
        }

        Order order = orderRepository.save(
                Order.place(customerId, lines, pricingCalculator.orderTotal(lines)));

        cart.clear();
        cartRepository.save(cart);
        return order;
    }
}
