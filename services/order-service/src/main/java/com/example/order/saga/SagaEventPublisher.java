package com.example.order.saga;

import com.example.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreated orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE, SagaTopology.ORDER_CREATED_ROUTING_KEY, event);
    }

    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.of(order.getId(), order.getUserEmail(), order.getTotalAmount());
        log.info("Publishing OrderConfirmed orderId={}", order.getId());
        rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE, SagaTopology.ORDER_CONFIRMED_ROUTING_KEY, event);
    }

    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.of(order.getId(), order.getUserEmail(), reason);
        log.info("Publishing OrderCancelled orderId={}", order.getId());
        rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE, SagaTopology.ORDER_CANCELLED_ROUTING_KEY, event);
    }
}
