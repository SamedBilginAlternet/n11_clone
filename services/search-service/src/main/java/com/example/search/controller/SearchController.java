package com.example.search.controller;

import com.example.search.dto.SearchResponse;
import com.example.search.service.ProductIndexer;
import com.example.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final ProductIndexer productIndexer;

    @GetMapping
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return searchService.search(q, category, brand, minPrice, maxPrice, minRating, sort, page, size);
    }

    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        long count = productIndexer.indexAll();
        return Map.of("indexed", count);
    }
}
