package com.demo.store.domain.shared;

public record Address(String street, String city, String zip, String country) {

    public Address {
        requireNonBlank(street, "street");
        requireNonBlank(city, "city");
        requireNonBlank(zip, "zip");
        requireNonBlank(country, "country");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
