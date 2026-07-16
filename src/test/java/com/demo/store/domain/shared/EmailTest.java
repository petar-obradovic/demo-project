package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void givenValidAddress_whenConstructed_thenHoldsValue() {
        assertThat(new Email("ana@example.com").value()).isEqualTo("ana@example.com");
    }

    @Test
    void givenInvalidAddress_whenConstructed_thenThrows() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
