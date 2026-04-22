package com.example.payment.saga;

public final class SagaTopology {
    public static final String EXCHANGE = "saga.exchange";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "payment.order-created.queue";

    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    private SagaTopology() {}
}
