package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String brand,
        String description,
        @NotNull @Positive BigDecimal price,
        int discountPercentage,
        int stockQuantity,
        String imageUrl
) {}
