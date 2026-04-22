package com.example.payment.saga;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        String reason
) implements Serializable {
    public static PaymentFailedEvent of(Long orderId, String userEmail, String reason) {
        return new PaymentFailedEvent(UUID.randomUUID().toString(), Instant.now(), orderId, userEmail, reason);
    }
}
