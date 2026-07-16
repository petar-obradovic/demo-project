package com.demo.store.api;

import com.demo.store.api.dto.ProductDtos.CreateProductRequest;
import com.demo.store.api.dto.ProductDtos.ProductResponse;
import com.demo.store.application.CatalogService;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(catalogService.createProduct(
                request.sku(), request.name(), request.description(),
                Money.of(request.price()), request.initialStock()));
    }

    @GetMapping
    public List<ProductResponse> list() {
        return catalogService.listActiveProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(catalogService.getProduct(new ProductId(id)));
    }
}
