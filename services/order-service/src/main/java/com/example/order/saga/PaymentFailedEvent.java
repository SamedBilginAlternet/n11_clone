package com.example.order.saga;

import java.io.Serializable;
import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        String reason
) implements Serializable {}
