package com.example.jwtjava.exception;

import org.springframework.http.HttpStatus;

/** Thrown when registration is attempted with an already-used e-mail. */
public class UserAlreadyExistsException extends AuthException {

    public UserAlreadyExistsException(String email) {
        super("Bu e-posta adresi zaten kullanımda: " + email, HttpStatus.CONFLICT);
    }
}
