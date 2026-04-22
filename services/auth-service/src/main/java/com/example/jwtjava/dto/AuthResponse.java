package com.example.jwtjava.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public AuthResponse(String accessToken) {
        this(accessToken, "Bearer", 900);
    }
}
