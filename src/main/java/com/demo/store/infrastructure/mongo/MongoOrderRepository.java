package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.order.OrderStatus;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springData;

    public MongoOrderRepository(SpringDataOrderRepository springData) {
        this.springData = springData;
    }

    @Override
    public Order save(Order order) {
        OrderDocument doc = new OrderDocument();
        doc.setId(order.id().value());
        doc.setCustomerId(order.customerId().value());
        doc.setLines(order.lines().stream().map(line -> {
            OrderDocument.Line l = new OrderDocument.Line();
            l.setProductId(line.productId().value());
            l.setName(line.name());
            l.setUnitPriceAmount(line.unitPrice().amount());
            l.setUnitPriceCurrency(line.unitPrice().currency().getCurrencyCode());
            l.setQuantity(line.quantity());
            l.setLineTotalAmount(line.lineTotal().amount());
            l.setLineTotalCurrency(line.lineTotal().currency().getCurrencyCode());
            return l;
        }).toList());
        doc.setTotalAmount(order.total().amount());
        doc.setTotalCurrency(order.total().currency().getCurrencyCode());
        doc.setStatus(order.status().name());
        doc.setPlacedAt(order.placedAt());
        springData.save(doc);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springData.findById(id.value()).map(MongoOrderRepository::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return springData.findByCustomerId(customerId.value()).stream()
                .map(MongoOrderRepository::toDomain)
                .toList();
    }

    static Order toDomain(OrderDocument doc) {
        List<OrderLine> lines = doc.getLines().stream().map(l -> new OrderLine(
                new ProductId(l.getProductId()),
                l.getName(),
                new Money(l.getUnitPriceAmount(), Currency.getInstance(l.getUnitPriceCurrency())),
                l.getQuantity(),
                new Money(l.getLineTotalAmount(), Currency.getInstance(l.getLineTotalCurrency()))))
                .toList();
        return new Order(
                new OrderId(doc.getId()),
                new CustomerId(doc.getCustomerId()),
                lines,
                new Money(doc.getTotalAmount(), Currency.getInstance(doc.getTotalCurrency())),
                OrderStatus.valueOf(doc.getStatus()),
                doc.getPlacedAt());
    }
}
