package com.example.order.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSucceededEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal amount,
        String transactionId
) implements Serializable {}
