package com.example.basket.saga;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record BasketCreationFailedEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String reason
) implements Serializable {

    public static BasketCreationFailedEvent of(Long userId, String email, String reason) {
        return new BasketCreationFailedEvent(UUID.randomUUID().toString(), Instant.now(), userId, email, reason);
    }
}
