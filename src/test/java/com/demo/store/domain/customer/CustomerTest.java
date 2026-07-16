package com.demo.store.domain.customer;

import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void givenValidData_whenRegistered_thenHasIdAndFields() {
        Customer c = Customer.register("Ana", new Email("ana@example.com"),
                new Address("Main St 1", "Belgrade", "11000", "RS"));
        assertThat(c.id()).isNotNull();
        assertThat(c.name()).isEqualTo("Ana");
        assertThat(c.email().value()).isEqualTo("ana@example.com");
    }

    @Test
    void givenBlankName_whenRegistered_thenThrows() {
        assertThatThrownBy(() -> Customer.register(" ", new Email("a@b.com"),
                new Address("s", "c", "z", "RS")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
