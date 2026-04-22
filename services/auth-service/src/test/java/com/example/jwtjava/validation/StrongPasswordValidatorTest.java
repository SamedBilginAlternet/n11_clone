package com.example.jwtjava.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @DisplayName("Valid passwords pass")
    @ValueSource(strings = {
            "Secure1!",
            "P@ssw0rd",
            "MyStr0ng#Pass",
            "Hello1@World"
    })
    void valid_passwords_pass(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Weak passwords are rejected")
    @ValueSource(strings = {
            "",
            "short1!",         // too short
            "alllowercase1!",  // no uppercase
            "ALLUPPERCASE1!",  // no lowercase — wait, spec only requires uppercase+digit+special
            "NoDigits!Abc",    // no digit
            "NoSpecial1Abc",   // no special char
            "12345678"         // only digits
    })
    void weak_passwords_are_rejected(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("Null and blank passwords are rejected")
    @ValueSource(strings = {"   "})
    void blank_password_rejected(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }
}
