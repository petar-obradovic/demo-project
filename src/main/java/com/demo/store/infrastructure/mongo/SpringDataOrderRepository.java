package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpringDataOrderRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByCustomerId(String customerId);
}
