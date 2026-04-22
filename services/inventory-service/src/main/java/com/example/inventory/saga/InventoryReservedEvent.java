package com.example.inventory.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Re-broadcasts the order's business data so downstream services (payment)
 * don't need to fetch it again. Shape mirrors OrderCreatedEvent for an easy
 * swap on the payment side.
 */
public record InventoryReservedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount
) implements Serializable {

    public static InventoryReservedEvent of(Long orderId, String userEmail, BigDecimal totalAmount) {
        return new InventoryReservedEvent(UUID.randomUUID().toString(), Instant.now(),
                orderId, userEmail, totalAmount);
    }
}
