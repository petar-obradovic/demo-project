package com.demo.store.application;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void givenValidData_whenRegister_thenSavedAndReturned() {
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer c = customerService.register("Ana", "ana@example.com",
                new Address("Main St 1", "Belgrade", "11000", "RS"));

        assertThat(c.email()).isEqualTo(new Email("ana@example.com"));
    }

    @Test
    void givenUnknownId_whenGetCustomer_thenNotFound() {
        CustomerId id = CustomerId.newId();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(NotFoundException.class);
    }
}
