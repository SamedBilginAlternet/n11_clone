package com.example.order.saga;

import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * If inventory-service refuses to reserve stock for a newly created order
 * (any item out of stock), the order is cancelled with the reason that came
 * back on the event. order.cancelled fires automatically inside
 * {@link OrderService#markCancelled} so notification-service and
 * inventory-service (which also releases anything it did reserve) learn
 * about it the usual way.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryOutOfStockListener {

    private final OrderService orderService;

    @RabbitListener(queues = SagaTopology.INVENTORY_OUT_OF_STOCK_QUEUE)
    public void onInventoryOutOfStock(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String reason = (String) event.getOrDefault("reason", "Stok yetersiz");
        log.warn("InventoryOutOfStock orderId={} — cancelling (reason: {})", orderId, reason);
        orderService.markCancelled(orderId, reason);
    }
}
