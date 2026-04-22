package com.example.inventory.saga;

import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga participant on the inventory side.
 *
 *   OrderCreated  → try to reserve all lines atomically. On success, publish
 *                   inventory.reserved (payment-service consumes next).
 *                   On failure, publish inventory.out-of-stock — order-service
 *                   cancels the order and the happy path stops here.
 *   OrderCancelled→ release any reservations. Covers both the "payment
 *                   declined" path and the "we ourselves cancelled" path,
 *                   so releases are idempotent via deleteByOrderId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryListener {

    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = SagaTopology.ORDER_CREATED_QUEUE)
    public void onOrderCreated(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String userEmail = (String) event.get("userEmail");
        BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");
        Map<Long, Integer> lines = toLines(items);

        if (lines.isEmpty()) {
            log.warn("OrderCreated orderId={} had no items payload — skipping reservation", orderId);
            rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE,
                    SagaTopology.INVENTORY_RESERVED_ROUTING_KEY,
                    InventoryReservedEvent.of(orderId, userEmail, totalAmount));
            return;
        }

        try {
            inventoryService.reserve(orderId, lines);
            log.info("Reservation OK for order={} lines={}", orderId, lines.size());
            rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE,
                    SagaTopology.INVENTORY_RESERVED_ROUTING_KEY,
                    InventoryReservedEvent.of(orderId, userEmail, totalAmount));
        } catch (InventoryService.OutOfStockException ex) {
            log.warn("Out of stock for order={} productId={} requested={}",
                    orderId, ex.getProductId(), ex.getRequested());
            rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE,
                    SagaTopology.INVENTORY_OUT_OF_STOCK_ROUTING_KEY,
                    InventoryOutOfStockEvent.of(orderId, userEmail,
                            ex.getProductId(), ex.getRequested(),
                            "Stok yetersiz (ürün #" + ex.getProductId() + ")"));
        }
    }

    @RabbitListener(queues = SagaTopology.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        log.info("OrderCancelled orderId={} — releasing reservations", orderId);
        inventoryService.release(orderId);
    }

    /**
     * OrderCreatedEvent's items look like: [{productId: 10, quantity: 2}, ...]
     * We only need (productId, quantity) here; other fields on the item are
     * order-service's internal DTO shape.
     */
    private Map<Long, Integer> toLines(List<Map<String, Object>> items) {
        Map<Long, Integer> out = new HashMap<>();
        if (items == null) return out;
        for (Map<String, Object> item : items) {
            Object pid = item.get("productId");
            Object qty = item.get("quantity");
            if (pid != null && qty != null) {
                out.merge(((Number) pid).longValue(), ((Number) qty).intValue(), Integer::sum);
            }
        }
        return out;
    }
}
