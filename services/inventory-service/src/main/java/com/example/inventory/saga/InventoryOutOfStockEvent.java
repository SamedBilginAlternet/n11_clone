package com.example.inventory.saga;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record InventoryOutOfStockEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        Long missingProductId,
        int requestedQuantity,
        String reason
) implements Serializable {

    public static InventoryOutOfStockEvent of(Long orderId, String userEmail,
                                              Long missingProductId, int requested, String reason) {
        return new InventoryOutOfStockEvent(UUID.randomUUID().toString(), Instant.now(),
                orderId, userEmail, missingProductId, requested, reason);
    }
}
