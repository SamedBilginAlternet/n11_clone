package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CheckoutItemRequest(
        @NotNull Long productId,
        @NotBlank String productName,
        @NotNull BigDecimal productPrice,
        String imageUrl,
        @Min(1) int quantity
) {}
