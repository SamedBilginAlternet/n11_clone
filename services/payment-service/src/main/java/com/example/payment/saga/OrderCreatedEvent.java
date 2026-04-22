package com.example.payment.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal totalAmount
) implements Serializable {}
