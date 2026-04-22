package com.example.order.saga;

import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentListenerTest {

    @Mock OrderService orderService;

    @InjectMocks PaymentListener listener;

    @Test
    void paymentSucceeded_callsMarkPaid() {
        var event = new PaymentSucceededEvent(
                "evt", Instant.now(), 7L, "u@example.com",
                new BigDecimal("100"), "TXN-1");

        listener.onPaymentSucceeded(event);

        verify(orderService).markPaid(7L);
    }

    @Test
    void paymentFailed_callsMarkCancelledWithReason() {
        var event = new PaymentFailedEvent(
                "evt", Instant.now(), 7L, "u@example.com", "Kart reddedildi");

        listener.onPaymentFailed(event);

        verify(orderService).markCancelled(7L, "Kart reddedildi");
    }
}
