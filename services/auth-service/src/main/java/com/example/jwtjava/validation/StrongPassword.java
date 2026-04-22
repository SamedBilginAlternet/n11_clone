package com.example.jwtjava.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom annotation that enforces a strong-password policy:
 * min 8 chars, at least one uppercase, one digit, one special character.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "Şifre en az 8 karakter, bir büyük harf, bir rakam ve bir özel karakter içermelidir.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
