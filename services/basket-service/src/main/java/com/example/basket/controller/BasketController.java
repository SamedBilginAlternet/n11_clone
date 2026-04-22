package com.example.basket.controller;

import com.example.basket.dto.AddItemRequest;
import com.example.basket.dto.BasketResponse;
import com.example.basket.dto.UpdateItemRequest;
import com.example.basket.service.BasketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/basket")
@RequiredArgsConstructor
@Tag(name = "Basket", description = "Sepet işlemleri")
@SecurityRequirement(name = "bearerAuth")
public class BasketController {

    private final BasketService basketService;

    @GetMapping
    @Operation(summary = "Oturumdaki kullanıcının sepetini getirir")
    public BasketResponse getBasket(@AuthenticationPrincipal String userEmail) {
        return basketService.getBasket(userEmail);
    }

    @PostMapping("/items")
    @Operation(summary = "Sepete ürün ekler")
    public BasketResponse addItem(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody AddItemRequest req
    ) {
        return basketService.addItem(userEmail, req);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Sepet öğesinin adedini günceller")
    public BasketResponse updateItem(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest req
    ) {
        return basketService.updateItem(userEmail, itemId, req);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Sepet öğesini kaldırır")
    public BasketResponse removeItem(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long itemId
    ) {
        return basketService.removeItem(userEmail, itemId);
    }

    @DeleteMapping
    @Operation(summary = "Sepeti tamamen boşaltır")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal String userEmail) {
        basketService.clear(userEmail);
        return ResponseEntity.noContent().build();
    }
}
