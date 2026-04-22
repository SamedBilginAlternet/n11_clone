package com.example.payment.service;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.PaymentTransaction;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.saga.OrderCreatedEvent;
import com.example.payment.saga.PaymentFailedEvent;
import com.example.payment.saga.PaymentSucceededEvent;
import com.example.payment.saga.SagaTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock payment processor. Approves everything by default; fails deterministically
 * for two demo cases so the unhappy-path saga is easy to trigger without a real
 * gateway:
 *   - email contains "fail" → "Kart reddedildi"
 *   - amount > 100.000 TRY  → "Limit aşıldı"
 * Real integrations (iyzico, Stripe, etc.) would replace {@link #authorize}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void process(OrderCreatedEvent event) {
        Result result = authorize(event);
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        PaymentTransaction saved = paymentRepository.save(PaymentTransaction.builder()
                .transactionId(txnId)
                .orderId(event.orderId())
                .userEmail(event.userEmail())
                .amount(event.totalAmount())
                .status(result.ok() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED)
                .failureReason(result.reason())
                .build());

        if (result.ok()) {
            log.info("Payment SUCCEEDED orderId={} amount={} txn={}",
                    event.orderId(), event.totalAmount(), txnId);
            rabbitTemplate.convertAndSend(
                    SagaTopology.EXCHANGE,
                    SagaTopology.PAYMENT_SUCCEEDED_ROUTING_KEY,
                    PaymentSucceededEvent.of(event.orderId(), event.userEmail(), event.totalAmount(), txnId));
        } else {
            log.warn("Payment FAILED orderId={} reason={}", event.orderId(), result.reason());
            rabbitTemplate.convertAndSend(
                    SagaTopology.EXCHANGE,
                    SagaTopology.PAYMENT_FAILED_ROUTING_KEY,
                    PaymentFailedEvent.of(event.orderId(), event.userEmail(), result.reason()));
        }
    }

    private Result authorize(OrderCreatedEvent e) {
        if (e.userEmail() != null && e.userEmail().toLowerCase().contains("fail")) {
            return new Result(false, "Kart reddedildi (demo).");
        }
        if (e.totalAmount().compareTo(MAX_AMOUNT) > 0) {
            return new Result(false, "Limit aşıldı (demo).");
        }
        return new Result(true, null);
    }

    private record Result(boolean ok, String reason) {}
}
