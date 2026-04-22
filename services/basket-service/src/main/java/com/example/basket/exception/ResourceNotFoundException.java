package com.example.basket.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BasketException {
    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s bulunamadı: %s".formatted(resource, identifier), HttpStatus.NOT_FOUND);
    }
}
