package com.demo.store.application;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String sku, String name, String description,
                                 Money price, int initialStock) {
        return productRepository.save(Product.create(sku, name, description, price, initialStock));
    }

    public Product getProduct(ProductId id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id.value()));
    }

    public List<Product> listActiveProducts() {
        return productRepository.findAllActive();
    }
}
