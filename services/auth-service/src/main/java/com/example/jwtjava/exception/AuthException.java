package com.example.jwtjava.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all authentication/authorization errors.
 * Carries an HTTP status so the global handler doesn't need to guess it.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
