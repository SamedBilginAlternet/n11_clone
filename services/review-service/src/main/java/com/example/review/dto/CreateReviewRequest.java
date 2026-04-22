package com.example.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull Long productId,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 2000) String comment
) {}
