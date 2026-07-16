package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;

import java.util.Optional;

public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByCustomerId(CustomerId customerId);
}
