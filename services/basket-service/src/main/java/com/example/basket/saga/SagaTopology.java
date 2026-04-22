package com.example.basket.saga;

/**
 * Mirror of the auth-service SagaTopology. Duplicated (rather than shared via a
 * library) so each service can be deployed independently — the contract lives
 * in these constant values.
 */
public final class SagaTopology {

    public static final String EXCHANGE = "saga.exchange";

    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_REGISTERED_QUEUE = "basket.user-registered.queue";

    public static final String BASKET_FAILED_ROUTING_KEY = "basket.creation.failed";

    private SagaTopology() {}
}
