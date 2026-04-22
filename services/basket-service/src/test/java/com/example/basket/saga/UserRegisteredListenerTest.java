package com.example.basket.saga;

import com.example.basket.service.BasketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

    @Mock BasketService basketService;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks UserRegisteredListener listener;

    private static final UserRegisteredEvent EVENT = new UserRegisteredEvent(
            "evt-1", Instant.now(), 42L, "new@example.com", "New User");

    @Test
    @DisplayName("happy path creates basket and does not publish compensation")
    void happyPath_noCompensation() {
        listener.onUserRegistered(EVENT);

        verify(basketService).createEmptyBasketFor("new@example.com");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("exception during basket creation publishes BasketCreationFailed on compensation routing key")
    void failure_publishesCompensation() {
        doThrow(new RuntimeException("DB down")).when(basketService).createEmptyBasketFor(anyString());

        listener.onUserRegistered(EVENT);

        ArgumentCaptor<BasketCreationFailedEvent> payload = ArgumentCaptor.forClass(BasketCreationFailedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(SagaTopology.EXCHANGE),
                eq(SagaTopology.BASKET_FAILED_ROUTING_KEY),
                payload.capture());
        assertThat(payload.getValue().userId()).isEqualTo(42L);
        assertThat(payload.getValue().email()).isEqualTo("new@example.com");
        assertThat(payload.getValue().reason()).contains("DB down");
    }
}
