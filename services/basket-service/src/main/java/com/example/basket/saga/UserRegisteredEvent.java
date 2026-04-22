package com.example.basket.saga;

import java.io.Serializable;
import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName
) implements Serializable {}
