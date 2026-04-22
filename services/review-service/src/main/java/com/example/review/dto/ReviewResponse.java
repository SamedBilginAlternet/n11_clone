package com.example.review.dto;

import com.example.review.entity.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        String userName,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getProductId(), r.getUserName(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
