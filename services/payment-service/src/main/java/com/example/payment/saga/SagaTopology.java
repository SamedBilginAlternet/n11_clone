package com.example.payment.saga;

public final class SagaTopology {
    public static final String EXCHANGE = "saga.exchange";

    /**
     * Payment consumes inventory.reserved — not order.created directly.
     * Reserving stock first means we never charge a card for an order whose
     * items are out of stock.
     */
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_RESERVED_QUEUE = "payment.inventory-reserved.queue";

    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    private SagaTopology() {}
}
