package com.example.notification.saga;

public final class SagaTopology {
    public static final String EXCHANGE = "saga.exchange";

    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    public static final String USER_REGISTERED_QUEUE = "notification.user-registered.queue";
    public static final String ORDER_CONFIRMED_QUEUE = "notification.order-confirmed.queue";
    public static final String ORDER_CANCELLED_QUEUE = "notification.order-cancelled.queue";

    private SagaTopology() {}
}
