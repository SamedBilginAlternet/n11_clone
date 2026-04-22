package com.example.basket.saga;

import com.example.basket.service.BasketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Final step of the checkout saga on the basket side. When order-service
 * confirms an order (i.e. payment succeeded), we empty the user's basket —
 * checkout semantics would otherwise leave the just-purchased items in the
 * cart. Event payload is read as a Map so we don't need to import
 * order-service's DTOs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedListener {

    private final BasketService basketService;

    @RabbitListener(queues = SagaTopology.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(Map<String, Object> event) {
        String email = (String) event.get("userEmail");
        Long orderId = ((Number) event.get("orderId")).longValue();
        log.info("OrderConfirmed orderId={} — clearing basket for {}", orderId, email);
        basketService.clear(email);
    }
}
