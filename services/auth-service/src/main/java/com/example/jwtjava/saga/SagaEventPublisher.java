package com.example.jwtjava.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegistered event userId={} email={}", event.userId(), event.email());
        rabbitTemplate.convertAndSend(
                SagaTopology.EXCHANGE,
                SagaTopology.USER_REGISTERED_ROUTING_KEY,
                event
        );
    }
}
