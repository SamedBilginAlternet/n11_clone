package com.example.order.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Carries enough order data for downstream consumers to act without a
 * callback to order-service:
 *   - totalAmount → payment-service (after inventory passes through)
 *   - items (productId + quantity) → inventory-service reserves stock
 */
public record OrderCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount,
        List<Line> items
) implements Serializable {

    public record Line(Long productId, int quantity) implements Serializable {}

    public static OrderCreatedEvent of(Long orderId, String userEmail, BigDecimal totalAmount, List<Line> items) {
        return new OrderCreatedEvent(UUID.randomUUID().toString(), Instant.now(),
                orderId, userEmail, totalAmount, items);
    }
}
