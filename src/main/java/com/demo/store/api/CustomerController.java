package com.demo.store.api;

import com.demo.store.api.dto.CustomerDtos.CustomerResponse;
import com.demo.store.api.dto.CustomerDtos.RegisterCustomerRequest;
import com.demo.store.application.CustomerService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.shared.Address;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody RegisterCustomerRequest request) {
        return CustomerResponse.from(customerService.register(
                request.name(), request.email(),
                new Address(request.street(), request.city(), request.zip(), request.country())));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable String id) {
        return CustomerResponse.from(customerService.getCustomer(new CustomerId(id)));
    }
}
