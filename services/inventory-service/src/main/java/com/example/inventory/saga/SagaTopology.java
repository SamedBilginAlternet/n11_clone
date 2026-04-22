package com.example.inventory.saga;

public final class SagaTopology {
    public static final String EXCHANGE = "saga.exchange";

    // Inbound
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "inventory.order-created.queue";

    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_CANCELLED_QUEUE = "inventory.order-cancelled.queue";

    // Outbound
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_OUT_OF_STOCK_ROUTING_KEY = "inventory.out-of-stock";

    private SagaTopology() {}
}
