package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpringDataProductRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findByActiveTrue();
}
