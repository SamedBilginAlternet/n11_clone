package com.example.search.dto;

import java.util.List;

public record ProductPage(
        List<ProductFromCatalog> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {}
