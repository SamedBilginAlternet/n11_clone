package com.example.payment.dto;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String transactionId,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        Instant createdAt
) {
    public static PaymentResponse from(PaymentTransaction t) {
        return new PaymentResponse(t.getId(), t.getTransactionId(), t.getOrderId(),
                t.getAmount(), t.getStatus(), t.getFailureReason(), t.getCreatedAt());
    }
}
