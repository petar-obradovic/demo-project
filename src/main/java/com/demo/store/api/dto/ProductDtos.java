package com.demo.store.api.dto;

import com.demo.store.domain.product.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int initialStock) {
    }

    public record ProductResponse(String id, String sku, String name, String description,
                                  BigDecimal price, String currency, int stockQuantity,
                                  boolean active) {

        public static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.id().value(),
                    product.sku(),
                    product.name(),
                    product.description(),
                    product.price().amount(),
                    product.price().currency().getCurrencyCode(),
                    product.stockQuantity(),
                    product.active());
        }
    }
}
