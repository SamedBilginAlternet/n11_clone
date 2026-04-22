package com.example.order.saga;

/**
 * Shared messaging contract for the checkout saga. Mirrored (not imported)
 * across all participating services so each remains independently deployable.
 *
 * Routing keys flow:
 *   order.created     — published by order-service after PENDING persist
 *   payment.succeeded — published by payment-service on successful charge
 *   payment.failed    — published by payment-service on declined charge
 *   order.confirmed   — published by order-service after payment.succeeded
 *   order.cancelled   — published by order-service after payment.failed
 */
public final class SagaTopology {

    public static final String EXCHANGE = "saga.exchange";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // order-service listens on these two to advance its own state machine
    public static final String PAYMENT_SUCCEEDED_QUEUE = "order.payment-succeeded.queue";
    public static final String PAYMENT_FAILED_QUEUE = "order.payment-failed.queue";

    private SagaTopology() {}
}
