package com.example.payment.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Same shape as inventory-service's InventoryReservedEvent — inventory
 * re-broadcasts the order's business data (id, email, total) so we don't
 * need to call back to order-service.
 */
public record InventoryReservedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount
) implements Serializable {}
