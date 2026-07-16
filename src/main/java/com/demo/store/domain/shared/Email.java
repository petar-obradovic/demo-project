package com.demo.store.domain.shared;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    public Email {
        if (value == null || !SIMPLE_EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
