package com.example.payment.saga;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        String userEmail,
        BigDecimal amount,
        String transactionId
) implements Serializable {
    public static PaymentSucceededEvent of(Long orderId, String userEmail, BigDecimal amount, String txnId) {
        return new PaymentSucceededEvent(UUID.randomUUID().toString(), Instant.now(), orderId, userEmail, amount, txnId);
    }
}
