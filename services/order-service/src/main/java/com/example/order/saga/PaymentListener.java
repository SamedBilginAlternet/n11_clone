package com.example.order.saga;

import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Drives the order state machine from payment-service's verdicts.
 * On success → order PAID + publish OrderConfirmed (fan-out to basket/notification).
 * On failure → order CANCELLED + publish OrderCancelled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final OrderService orderService;

    @RabbitListener(queues = SagaTopology.PAYMENT_SUCCEEDED_QUEUE)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("PaymentSucceeded orderId={} txn={}", event.orderId(), event.transactionId());
        orderService.markPaid(event.orderId());
    }

    @RabbitListener(queues = SagaTopology.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("PaymentFailed orderId={} reason={}", event.orderId(), event.reason());
        orderService.markCancelled(event.orderId(), event.reason());
    }
}
