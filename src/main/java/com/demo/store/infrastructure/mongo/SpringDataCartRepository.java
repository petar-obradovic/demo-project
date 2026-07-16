package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringDataCartRepository extends MongoRepository<CartDocument, String> {

    Optional<CartDocument> findByCustomerId(String customerId);
}
