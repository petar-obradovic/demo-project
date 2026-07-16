package com.demo.store.application;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void givenValidData_whenCreateProduct_thenSavedAndReturned() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product created = catalogService.createProduct(
                "SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);

        assertThat(created.sku()).isEqualTo("SKU-1");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void givenUnknownId_whenGetProduct_thenNotFound() {
        ProductId id = ProductId.newId();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProduct(id))
                .isInstanceOf(NotFoundException.class);
    }
}
