package com.demo.store.api;

import com.demo.store.api.dto.CartDtos.AddItemRequest;
import com.demo.store.api.dto.CartDtos.CartResponse;
import com.demo.store.api.dto.CartDtos.ChangeQuantityRequest;
import com.demo.store.application.CartService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts/{customerId}")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@PathVariable String customerId) {
        return CartResponse.from(cartService.getOrCreateCart(new CustomerId(customerId)));
    }

    @PostMapping("/items")
    public CartResponse addItem(@PathVariable String customerId,
                                @Valid @RequestBody AddItemRequest request) {
        return CartResponse.from(cartService.addItem(
                new CustomerId(customerId), new ProductId(request.productId()), request.quantity()));
    }

    @PatchMapping("/items/{productId}")
    public CartResponse changeQuantity(@PathVariable String customerId,
                                       @PathVariable String productId,
                                       @Valid @RequestBody ChangeQuantityRequest request) {
        return CartResponse.from(cartService.changeQuantity(
                new CustomerId(customerId), new ProductId(productId), request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable String customerId,
                                   @PathVariable String productId) {
        return CartResponse.from(cartService.removeItem(
                new CustomerId(customerId), new ProductId(productId)));
    }
}
