package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AddressTest {

    @Test
    void givenAllFields_whenConstructed_thenOk() {
        assertThatNoException().isThrownBy(
                () -> new Address("Main St 1", "Belgrade", "11000", "RS"));
    }

    @Test
    void givenBlankField_whenConstructed_thenThrows() {
        assertThatThrownBy(() -> new Address(" ", "Belgrade", "11000", "RS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Address("Main St 1", "Belgrade", "11000", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
