package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
}
