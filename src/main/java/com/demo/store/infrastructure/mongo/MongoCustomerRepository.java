package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository springData;

    public MongoCustomerRepository(SpringDataCustomerRepository springData) {
        this.springData = springData;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerDocument doc = new CustomerDocument();
        doc.setId(customer.id().value());
        doc.setName(customer.name());
        doc.setEmail(customer.email().value());
        doc.setStreet(customer.shippingAddress().street());
        doc.setCity(customer.shippingAddress().city());
        doc.setZip(customer.shippingAddress().zip());
        doc.setCountry(customer.shippingAddress().country());
        springData.save(doc);
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return springData.findById(id.value()).map(doc -> new Customer(
                new CustomerId(doc.getId()),
                doc.getName(),
                new Email(doc.getEmail()),
                new Address(doc.getStreet(), doc.getCity(), doc.getZip(), doc.getCountry())));
    }
}
