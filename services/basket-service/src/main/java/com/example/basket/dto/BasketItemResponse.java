package com.example.basket.dto;

import com.example.basket.entity.BasketItem;

import java.math.BigDecimal;

public record BasketItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal productPrice,
        String imageUrl,
        int quantity,
        BigDecimal subtotal
) {
    public static BasketItemResponse from(BasketItem item) {
        return new BasketItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductPrice(),
                item.getImageUrl(),
                item.getQuantity(),
                item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
