package com.example.product.service;

import com.example.product.dto.CategoryResponse;
import com.example.product.dto.CreateProductRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.UpdateProductRequest;
import com.example.product.entity.Product;
import com.example.product.event.ProductEventPublisher;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final ProductEventPublisher eventPublisher;

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

    @Cacheable(value = "products:byId", key = "#id")
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id));
    }

    @Cacheable(value = "products:bySlug", key = "#slug")
    public ProductResponse getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + slug));
    }

    @Cacheable(value = "products:categories")
    public List<CategoryResponse> categories() {
        return CATEGORIES;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products:list", allEntries = true),
        @CacheEvict(value = "products:categories", allEntries = true)
    })
    public ProductResponse create(CreateProductRequest req) {
        String slug = req.name().toLowerCase()
                .replaceAll("[^a-z0-9ğüşıöç]+", "-")
                .replaceAll("^-|-$", "");
        Product product = Product.builder()
                .name(req.name())
                .slug(slug + "-" + System.currentTimeMillis())
                .description(req.description())
                .price(req.price())
                .discountPercentage(req.discountPercentage())
                .stockQuantity(req.stockQuantity())
                .imageUrl(req.imageUrl())
                .category(req.category())
                .brand(req.brand())
                .rating(0)
                .reviewCount(0)
                .build();
        product = productRepository.save(product);
        eventPublisher.publishCreated(product);
        return ProductResponse.from(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products:byId", key = "#id"),
        @CacheEvict(value = "products:list", allEntries = true)
    })
    public ProductResponse update(Long id, UpdateProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id));
        if (req.name() != null) product.setName(req.name());
        if (req.category() != null) product.setCategory(req.category());
        if (req.brand() != null) product.setBrand(req.brand());
        if (req.description() != null) product.setDescription(req.description());
        if (req.price() != null) product.setPrice(req.price());
        if (req.discountPercentage() != null) product.setDiscountPercentage(req.discountPercentage());
        if (req.stockQuantity() != null) product.setStockQuantity(req.stockQuantity());
        if (req.imageUrl() != null) product.setImageUrl(req.imageUrl());
        product = productRepository.save(product);
        eventPublisher.publishUpdated(product);
        return ProductResponse.from(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products:byId", key = "#id"),
        @CacheEvict(value = "products:list", allEntries = true)
    })
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id);
        }
        productRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }
}
