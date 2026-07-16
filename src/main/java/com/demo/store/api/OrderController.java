package com.demo.store.api;

import com.demo.store.api.dto.OrderDtos.OrderResponse;
import com.demo.store.application.CheckoutService;
import com.demo.store.application.OrderService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.OrderId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    public OrderController(CheckoutService checkoutService, OrderService orderService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
    }

    @PostMapping("/checkout/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@PathVariable String customerId) {
        return OrderResponse.from(checkoutService.checkout(new CustomerId(customerId)));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orderService.getOrder(new OrderId(id)));
    }

    @GetMapping
    public List<OrderResponse> listByCustomer(@RequestParam String customerId) {
        return orderService.listByCustomer(new CustomerId(customerId)).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable String id) {
        return OrderResponse.from(orderService.pay(new OrderId(id)));
    }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable String id) {
        return OrderResponse.from(orderService.ship(new OrderId(id)));
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable String id) {
        return OrderResponse.from(orderService.deliver(new OrderId(id)));
    }
}
