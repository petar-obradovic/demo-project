package com.demo.store.application;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, String id) {
        super("%s %s not found".formatted(what, id));
    }
}
