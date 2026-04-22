package com.example.payment.service;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.PaymentTransaction;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.saga.InventoryReservedEvent;
import com.example.payment.saga.PaymentFailedEvent;
import com.example.payment.saga.PaymentSucceededEvent;
import com.example.payment.saga.SagaTopology;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock PaymentRepository paymentRepository;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks PaymentProcessor processor;

    private InventoryReservedEvent event(String email, String amount) {
        return new InventoryReservedEvent("evt", Instant.now(), 1L, email, new BigDecimal(amount));
    }

    @Test
    @DisplayName("approves a normal amount and publishes payment.succeeded")
    void approvesNormalAmount() {
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event("user@n11demo.com", "100.00"));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(saved.getValue().getTransactionId()).startsWith("TXN-");

        verify(rabbitTemplate).convertAndSend(
                eq(SagaTopology.EXCHANGE),
                eq(SagaTopology.PAYMENT_SUCCEEDED_ROUTING_KEY),
                any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("declines emails containing 'fail' with the demo-reason string")
    void declinesEmailContainingFail() {
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event("failuser@n11demo.com", "100.00"));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).contains("Kart reddedildi");

        ArgumentCaptor<PaymentFailedEvent> event = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(SagaTopology.EXCHANGE),
                eq(SagaTopology.PAYMENT_FAILED_ROUTING_KEY),
                event.capture());
        assertThat(event.getValue().reason()).contains("Kart reddedildi");
    }

    @Test
    @DisplayName("declines amounts over 100.000 TRY with limit-exceeded reason")
    void declinesAmountOverLimit() {
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event("vip@n11demo.com", "150000.00"));

        verify(rabbitTemplate).convertAndSend(
                eq(SagaTopology.EXCHANGE),
                eq(SagaTopology.PAYMENT_FAILED_ROUTING_KEY),
                any(PaymentFailedEvent.class));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getFailureReason()).contains("Limit aşıldı");
    }

    @Test
    @DisplayName("approves exactly at the 100.000 TRY boundary")
    void approvesAtExactLimit() {
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event("ok@n11demo.com", "100000.00"));

        verify(rabbitTemplate).convertAndSend(
                eq(SagaTopology.EXCHANGE),
                eq(SagaTopology.PAYMENT_SUCCEEDED_ROUTING_KEY),
                any(PaymentSucceededEvent.class));
    }
}
