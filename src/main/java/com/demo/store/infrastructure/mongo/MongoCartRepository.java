package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartId;
import com.demo.store.domain.cart.CartItem;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoCartRepository implements CartRepository {

    private final SpringDataCartRepository springData;

    public MongoCartRepository(SpringDataCartRepository springData) {
        this.springData = springData;
    }

    @Override
    public Cart save(Cart cart) {
        CartDocument doc = new CartDocument();
        doc.setId(cart.id().value());
        doc.setCustomerId(cart.customerId().value());
        doc.setItems(cart.items().stream().map(item -> {
            CartDocument.Item i = new CartDocument.Item();
            i.setProductId(item.productId().value());
            i.setName(item.name());
            i.setUnitPriceAmount(item.unitPrice().amount());
            i.setUnitPriceCurrency(item.unitPrice().currency().getCurrencyCode());
            i.setQuantity(item.quantity());
            return i;
        }).toList());
        springData.save(doc);
        return cart;
    }

    @Override
    public Optional<Cart> findByCustomerId(CustomerId customerId) {
        return springData.findByCustomerId(customerId.value()).map(doc -> {
            List<CartItem> items = doc.getItems().stream().map(i -> new CartItem(
                    new ProductId(i.getProductId()),
                    i.getName(),
                    new Money(i.getUnitPriceAmount(), Currency.getInstance(i.getUnitPriceCurrency())),
                    i.getQuantity())).toList();
            return new Cart(new CartId(doc.getId()), new CustomerId(doc.getCustomerId()), items);
        });
    }
}
