package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public Order getOrder(OrderId id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id.value()));
    }

    public List<Order> listByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order pay(OrderId id) {
        Order order = getOrder(id);
        order.markPaid();
        return orderRepository.save(order);
    }

    public Order ship(OrderId id) {
        Order order = getOrder(id);
        order.markShipped();
        return orderRepository.save(order);
    }

    public Order deliver(OrderId id) {
        Order order = getOrder(id);
        order.markDelivered();
        return orderRepository.save(order);
    }

    public Order cancel(OrderId id) {
        Order order = getOrder(id);
        order.cancel();
        order.lines().forEach(line ->
                productRepository.findById(line.productId()).ifPresent(product -> {
                    product.increaseStock(line.quantity());
                    productRepository.save(product);
                }));
        return orderRepository.save(order);
    }
}
