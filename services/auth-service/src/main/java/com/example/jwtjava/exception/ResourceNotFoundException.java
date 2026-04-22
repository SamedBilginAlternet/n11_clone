package com.example.jwtjava.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist. */
public class ResourceNotFoundException extends AuthException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " bulunamadı: " + identifier, HttpStatus.NOT_FOUND);
    }
}
