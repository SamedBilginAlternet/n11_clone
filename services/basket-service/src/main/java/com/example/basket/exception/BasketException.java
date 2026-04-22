package com.example.basket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BasketException extends RuntimeException {

    private final HttpStatus status;

    public BasketException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
