package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataCustomerRepository extends MongoRepository<CustomerDocument, String> {
}
