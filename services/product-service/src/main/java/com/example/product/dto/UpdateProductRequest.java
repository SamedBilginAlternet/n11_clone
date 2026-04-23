package com.example.product.dto;

import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        String category,
        String brand,
        String description,
        BigDecimal price,
        Integer discountPercentage,
        Integer stockQuantity,
        String imageUrl
) {}
