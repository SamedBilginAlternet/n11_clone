package com.example.product.service;

import com.example.product.dto.CategoryResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final List<CategoryResponse> CATEGORIES = List.of(
            new CategoryResponse("elektronik", "Elektronik", "📱"),
            new CategoryResponse("moda", "Moda", "👗"),
            new CategoryResponse("ev-yasam", "Ev & Yaşam", "🏠"),
            new CategoryResponse("anne-bebek", "Anne & Bebek", "🍼"),
            new CategoryResponse("kozmetik", "Kozmetik", "💄"),
            new CategoryResponse("spor-outdoor", "Spor & Outdoor", "⚽"),
            new CategoryResponse("kitap-muzik", "Kitap & Müzik", "📚"),
            new CategoryResponse("supermarket", "Süpermarket", "🛒")
    );

    private final ProductRepository productRepository;

    public Page<ProductResponse> list(String category, String q, Pageable pageable) {
        Page<Product> page;
        if (q != null && !q.isBlank()) {
            page = productRepository.findByNameContainingIgnoreCase(q.trim(), pageable);
        } else if (category != null && !category.isBlank()) {
            page = productRepository.findByCategory(category, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return page.map(ProductResponse::from);
    }

    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id));
    }

    public ProductResponse getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + slug));
    }

    public List<CategoryResponse> categories() {
        return CATEGORIES;
    }
}
