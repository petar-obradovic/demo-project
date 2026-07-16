package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);
}
