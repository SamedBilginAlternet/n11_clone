package com.example.jwtjava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern HAS_UPPERCASE   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT       = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL     = Pattern.compile("[^a-zA-Z0-9]");
    private static final int     MIN_LENGTH       = 8;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank() || password.length() < MIN_LENGTH) return false;
        return HAS_UPPERCASE.matcher(password).find()
                && HAS_LOWERCASE.matcher(password).find()
                && HAS_DIGIT.matcher(password).find()
                && HAS_SPECIAL.matcher(password).find();
    }
}
