package com.example.jwtjava.saga;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName
) implements Serializable {

    public static UserRegisteredEvent of(Long userId, String email, String fullName) {
        return new UserRegisteredEvent(UUID.randomUUID().toString(), Instant.now(), userId, email, fullName);
    }
}
