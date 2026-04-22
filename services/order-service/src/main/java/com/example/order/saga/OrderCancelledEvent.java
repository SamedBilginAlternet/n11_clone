package com.example.order.saga;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        String reason
) implements Serializable {

    public static OrderCancelledEvent of(Long orderId, String userEmail, String reason) {
        return new OrderCancelledEvent(UUID.randomUUID().toString(), Instant.now(), orderId, userEmail, reason);
    }
}
