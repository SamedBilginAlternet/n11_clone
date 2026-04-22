package com.example.inventory.dto;

import com.example.inventory.entity.InventoryItem;

public record InventoryResponse(
        Long productId,
        int availableStock,
        int reservedStock,
        boolean inStock
) {
    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableStock(),
                item.getReservedStock(),
                item.getAvailableStock() > 0
        );
    }
}
