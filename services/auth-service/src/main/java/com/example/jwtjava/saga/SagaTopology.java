package com.example.jwtjava.saga;

/**
 * Shared RabbitMQ topology for the user-registration saga.
 *
 * Choreography flow:
 *   1. auth-service publishes {@code user.registered} after persisting a new user.
 *   2. basket-service consumes it and creates an empty basket.
 *   3. If basket creation fails, basket-service publishes
 *      {@code basket.creation.failed}, which auth-service consumes to delete the
 *      orphaned user (compensating transaction).
 *
 * Each service declares the same exchange + its own queue, bound on the matching
 * routing key. Constants live here so the auth and basket sides stay in sync —
 * duplicating rather than sharing a lib to keep services independently
 * deployable.
 */
public final class SagaTopology {

    public static final String EXCHANGE = "saga.exchange";

    // Published by auth, consumed by basket
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_REGISTERED_QUEUE = "basket.user-registered.queue";

    // Published by basket, consumed by auth (compensation)
    public static final String BASKET_FAILED_ROUTING_KEY = "basket.creation.failed";
    public static final String BASKET_FAILED_QUEUE = "auth.basket-failed.queue";

    private SagaTopology() {}
}
