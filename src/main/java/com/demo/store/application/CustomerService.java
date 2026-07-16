package com.demo.store.application;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer register(String name, String email, Address shippingAddress) {
        return customerRepository.save(Customer.register(name, new Email(email), shippingAddress));
    }

    public Customer getCustomer(CustomerId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer", id.value()));
    }
}
