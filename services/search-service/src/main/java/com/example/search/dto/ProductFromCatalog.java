package com.example.search.dto;

/**
 * Mirror of product-service's ProductResponse for the indexer HTTP pull.
 * Only the fields we actually index are declared.
 */
public record ProductFromCatalog(
        Long id,
        String name,
        String slug,
        String description,
        double price,
        double discountedPrice,
        int discountPercentage,
        int stockQuantity,
        String imageUrl,
        String category,
        String brand,
        double rating,
        int reviewCount
) {}
