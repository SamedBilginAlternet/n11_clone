package com.example.jwtjava.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a refresh token is missing, expired, or revoked. */
public class TokenException extends AuthException {

    public TokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
