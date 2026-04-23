package com.example.product.event;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductEvent(
        String eventType,
        Long productId,
        String name,
        String slug,
        String description,
        BigDecimal price,
        BigDecimal discountedPrice,
        int discountPercentage,
        int stockQuantity,
        String imageUrl,
        String category,
        String brand,
        double rating,
        int reviewCount
) implements Serializable {
    public static final String CREATED = "product.created";
    public static final String UPDATED = "product.updated";
    public static final String DELETED = "product.deleted";
}
