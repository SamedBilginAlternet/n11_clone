package com.example.basket.dto;

import com.example.basket.entity.Basket;

import java.math.BigDecimal;
import java.util.List;

public record BasketResponse(
        Long id,
        String userEmail,
        List<BasketItemResponse> items,
        BigDecimal total,
        int itemCount
) {
    public static BasketResponse from(Basket basket) {
        List<BasketItemResponse> items = basket.getItems().stream()
                .map(BasketItemResponse::from)
                .toList();

        BigDecimal total = items.stream()
                .map(BasketItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = items.stream().mapToInt(BasketItemResponse::quantity).sum();

        return new BasketResponse(basket.getId(), basket.getUserEmail(), items, total, count);
    }
}
