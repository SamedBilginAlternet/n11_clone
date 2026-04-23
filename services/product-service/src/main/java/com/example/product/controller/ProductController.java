package com.example.product.controller;

import com.example.product.dto.CategoryResponse;
import com.example.product.dto.CreateProductRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.UpdateProductRequest;
import com.example.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Ürün katalog endpoint'leri (public)")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Ürünleri listeler (kategori / arama / sayfalama)")
    public Page<ProductResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 24) Pageable pageable
    ) {
        return productService.list(category, q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile tek ürün getirir")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Slug ile tek ürün getirir")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }

    @GetMapping("/categories")
    @Operation(summary = "Kategori listesini döner")
    public List<CategoryResponse> categories() {
        return productService.categories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
