package com.example.basket.service;

import com.example.basket.dto.AddItemRequest;
import com.example.basket.dto.UpdateItemRequest;
import com.example.basket.entity.Basket;
import com.example.basket.entity.BasketItem;
import com.example.basket.exception.ResourceNotFoundException;
import com.example.basket.repository.BasketItemRepository;
import com.example.basket.repository.BasketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock BasketRepository basketRepository;
    @Mock BasketItemRepository basketItemRepository;

    @InjectMocks BasketService basketService;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void returnSavedEntityAsIs() {
        // Default save() behavior — echo the entity back so the in-memory Basket is what we assert on.
        when(basketRepository.save(any(Basket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AddItemRequest item(long productId, int qty) {
        return new AddItemRequest(productId, "Product " + productId,
                new BigDecimal("100.00"), null, qty);
    }

    // ────────────────────────────────────────────────────────────────────
    // addItem — merge logic
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem creates basket and adds first item when user has none")
    void addItem_createsBasketOnFirstCall() {
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());

        var response = basketService.addItem(EMAIL, item(10L, 2));

        assertThat(response.userEmail()).isEqualTo(EMAIL);
        assertThat(response.items()).hasSize(1);
        assertThat(response.itemCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("addItem merges quantities when the same product is added twice")
    void addItem_mergesSameProduct() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        existing.getItems().add(BasketItem.builder()
                .id(1L).basket(existing).productId(10L).productName("x")
                .productPrice(new BigDecimal("100")).quantity(1).build());
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        var response = basketService.addItem(EMAIL, item(10L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("addItem appends a new line for a different product")
    void addItem_addsDistinctProductAsNewLine() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        existing.getItems().add(BasketItem.builder()
                .id(1L).basket(existing).productId(10L).productName("x")
                .productPrice(new BigDecimal("100")).quantity(1).build());
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        var response = basketService.addItem(EMAIL, item(20L, 2));

        assertThat(response.items()).hasSize(2);
        assertThat(response.itemCount()).isEqualTo(3);
    }

    // ────────────────────────────────────────────────────────────────────
    // updateItem / removeItem error paths
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateItem throws ResourceNotFoundException when item id missing")
    void updateItem_missingThrows() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> basketService.updateItem(EMAIL, 99L, new UpdateItemRequest(3)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("removeItem throws when item not in basket")
    void removeItem_missingThrows() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> basketService.removeItem(EMAIL, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ────────────────────────────────────────────────────────────────────
    // saga-facing methods
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEmptyBasketFor skips when basket already exists (idempotent)")
    void createEmptyBasketFor_idempotent() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        when(basketRepository.existsByUserEmail(EMAIL)).thenReturn(true);
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        Basket result = basketService.createEmptyBasketFor(EMAIL);

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("clear empties items when basket exists")
    void clear_emptiesItems() {
        Basket existing = Basket.builder().userEmail(EMAIL).items(new ArrayList<>()).build();
        existing.getItems().add(BasketItem.builder().id(1L).basket(existing).productId(10L)
                .productName("x").productPrice(new BigDecimal("100")).quantity(1).build());
        when(basketRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        basketService.clear(EMAIL);

        assertThat(existing.getItems()).isEmpty();
    }
}
