package com.example.search.dto;

import com.example.search.document.ProductDocument;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        List<ProductDocument> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        Facets facets
) {
    public record Facets(
            Map<String, Long> brands,
            Map<String, Long> categories,
            PriceStats price
    ) {}

    public record PriceStats(double min, double max) {}
}
