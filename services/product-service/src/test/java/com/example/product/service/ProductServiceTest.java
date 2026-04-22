package com.example.product.service;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;

    @InjectMocks ProductService productService;

    private Product stub(long id) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .slug("product-" + id)
                .price(new BigDecimal("1000"))
                .discountPercentage(20)
                .stockQuantity(10)
                .category("elektronik")
                .brand("Brand")
                .rating(4.5)
                .reviewCount(12)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────
    // list — routes to the right repository method per filter
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("list with no filters uses findAll")
    void list_noFilters_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stub(1))));

        productService.list(null, null, pageable);

        verify(productRepository).findAll(pageable);
        verify(productRepository, never()).findByCategory(any(), any());
        verify(productRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("list with category filter uses findByCategory")
    void list_category_usesFindByCategory() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByCategory(eq("elektronik"), any()))
                .thenReturn(new PageImpl<>(List.of(stub(1))));

        productService.list("elektronik", null, pageable);

        verify(productRepository).findByCategory("elektronik", pageable);
    }

    @Test
    @DisplayName("list with q takes precedence over category (search wins)")
    void list_qWinsOverCategory() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByNameContainingIgnoreCase(any(), any()))
                .thenReturn(new PageImpl<>(List.of(stub(1))));

        productService.list("elektronik", "iPhone", pageable);

        verify(productRepository).findByNameContainingIgnoreCase(eq("iPhone"), any());
        verify(productRepository, never()).findByCategory(any(), any());
    }

    @Test
    @DisplayName("list trims whitespace on q before searching")
    void list_trimsQueryString() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByNameContainingIgnoreCase(any(), any()))
                .thenReturn(new PageImpl<>(List.of(stub(1))));

        productService.list(null, "  macbook  ", pageable);

        ArgumentCaptor<String> term = ArgumentCaptor.forClass(String.class);
        verify(productRepository).findByNameContainingIgnoreCase(term.capture(), any());
        assertThat(term.getValue()).isEqualTo("macbook");
    }

    // ────────────────────────────────────────────────────────────────────
    // getById / getBySlug
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns mapped response and computes discountedPrice")
    void getById_computesDiscountedPrice() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(stub(1)));

        var response = productService.getById(1L);

        // 1000 * (100 - 20) / 100 = 800
        assertThat(response.discountedPrice()).isEqualByComparingTo("800.00");
        assertThat(response.discountPercentage()).isEqualTo(20);
    }

    @Test
    @DisplayName("getById throws 404 when product missing")
    void getById_missingThrows404() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ürün bulunamadı");
    }

    @Test
    @DisplayName("getBySlug throws 404 when slug missing")
    void getBySlug_missingThrows404() {
        when(productRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySlug("nope"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ────────────────────────────────────────────────────────────────────
    // categories — static list contract
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("categories returns the 8 n11-style Turkish categories")
    void categories_returnsEightCategories() {
        var categories = productService.categories();

        assertThat(categories).hasSize(8);
        assertThat(categories).extracting("slug")
                .contains("elektronik", "moda", "ev-yasam", "supermarket");
    }
}
