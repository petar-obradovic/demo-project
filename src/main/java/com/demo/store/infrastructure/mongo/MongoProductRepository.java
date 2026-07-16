package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoProductRepository implements ProductRepository {

    private final SpringDataProductRepository springData;

    public MongoProductRepository(SpringDataProductRepository springData) {
        this.springData = springData;
    }

    @Override
    public Product save(Product product) {
        springData.save(toDocument(product));
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return springData.findById(id.value()).map(MongoProductRepository::toDomain);
    }

    @Override
    public List<Product> findAllActive() {
        return springData.findByActiveTrue().stream()
                .map(MongoProductRepository::toDomain)
                .toList();
    }

    static ProductDocument toDocument(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.id().value());
        doc.setSku(product.sku());
        doc.setName(product.name());
        doc.setDescription(product.description());
        doc.setPriceAmount(product.price().amount());
        doc.setPriceCurrency(product.price().currency().getCurrencyCode());
        doc.setStockQuantity(product.stockQuantity());
        doc.setActive(product.active());
        return doc;
    }

    static Product toDomain(ProductDocument doc) {
        return new Product(
                new ProductId(doc.getId()),
                doc.getSku(),
                doc.getName(),
                doc.getDescription(),
                new Money(doc.getPriceAmount(), Currency.getInstance(doc.getPriceCurrency())),
                doc.getStockQuantity(),
                doc.isActive());
    }
}
