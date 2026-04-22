package com.example.order.dto;

import com.example.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal productPrice,
        String imageUrl,
        int quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
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
