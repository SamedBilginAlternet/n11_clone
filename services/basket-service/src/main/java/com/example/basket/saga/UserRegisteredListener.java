package com.example.basket.saga;

import com.example.basket.service.BasketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Saga participant on the basket side:
 *   - On UserRegistered → create an empty basket for the new user.
 *   - On failure → publish BasketCreationFailed so auth-service can compensate
 *     by deleting the orphaned user.
 *
 * The RabbitMQ starter retries the listener automatically (see application.yml);
 * we only publish the failure event after retries are exhausted (i.e. once the
 * exception actually bubbles out of the listener method). For this demo we fail
 * fast on the first exception to keep the saga flow observable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final BasketService basketService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = SagaTopology.USER_REGISTERED_QUEUE)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegistered userId={} email={}", event.userId(), event.email());
        try {
            basketService.createEmptyBasketFor(event.email());
            log.info("Empty basket provisioned for email={}", event.email());
        } catch (Exception ex) {
            log.error("Failed to create basket for email={} — publishing compensation",
                    event.email(), ex);
            rabbitTemplate.convertAndSend(
                    SagaTopology.EXCHANGE,
                    SagaTopology.BASKET_FAILED_ROUTING_KEY,
                    BasketCreationFailedEvent.of(event.userId(), event.email(), ex.getMessage())
            );
        }
    }
}
