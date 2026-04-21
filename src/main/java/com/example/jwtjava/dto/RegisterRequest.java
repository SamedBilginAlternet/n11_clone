package com.example.jwtjava.dto;

import com.example.jwtjava.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @StrongPassword
        String password,

        @NotBlank
        String fullName
) {}
