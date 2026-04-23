package com.example.product.dto;

import com.example.product.entity.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductResponse(
        Long id,
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
) {
    public static ProductResponse from(Product p) {
        BigDecimal discounted = p.getPrice()
                .multiply(BigDecimal.valueOf(100 - p.getDiscountPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ProductResponse(
                p.getId(), p.getName(), p.getSlug(), p.getDescription(),
                p.getPrice(), discounted, p.getDiscountPercentage(), p.getStockQuantity(),
                p.getImageUrl(), p.getCategory(), p.getBrand(), p.getRating(), p.getReviewCount()
        );
    }
}
