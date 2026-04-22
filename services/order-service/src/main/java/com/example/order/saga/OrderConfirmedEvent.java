package com.example.order.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount
) implements Serializable {

    public static OrderConfirmedEvent of(Long orderId, String userEmail, BigDecimal totalAmount) {
        return new OrderConfirmedEvent(UUID.randomUUID().toString(), Instant.now(), orderId, userEmail, totalAmount);
    }
}
