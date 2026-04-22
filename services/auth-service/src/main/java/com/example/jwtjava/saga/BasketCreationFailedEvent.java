package com.example.jwtjava.saga;

import java.io.Serializable;
import java.time.Instant;

public record BasketCreationFailedEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String reason
) implements Serializable {}
