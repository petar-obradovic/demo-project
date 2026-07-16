package com.demo.store.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findAllActive();
}
