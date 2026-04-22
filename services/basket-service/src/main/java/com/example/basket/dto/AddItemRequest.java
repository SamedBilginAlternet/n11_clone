package com.example.basket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddItemRequest(
        @NotNull Long productId,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.0") BigDecimal productPrice,
        String imageUrl,
        @Min(1) int quantity
) {}
