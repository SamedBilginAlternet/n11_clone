package com.example.search.event;

import java.io.Serializable;

public record ProductEvent(
        String eventType,
        Long productId,
        String name,
        String slug,
        String description,
        Double price,
        Double discountedPrice,
        Integer discountPercentage,
        Integer stockQuantity,
        String imageUrl,
        String category,
        String brand,
        Double rating,
        Integer reviewCount
) implements Serializable {
    public static final String CREATED = "product.created";
    public static final String UPDATED = "product.updated";
    public static final String DELETED = "product.deleted";
}
