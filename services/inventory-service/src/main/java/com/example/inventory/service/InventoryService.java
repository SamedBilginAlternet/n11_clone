package com.example.inventory.service;

import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.Reservation;
import com.example.inventory.repository.InventoryItemRepository;
import com.example.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * All-or-nothing stock reservation. For a given order, either every line item
 * is reservable and we commit rows for all of them, or we roll back anything
 * this transaction touched and surface a failure to the saga.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository items;
    private final ReservationRepository reservations;

    public record ReservationOutcome(boolean success, Long firstMissingProductId, Integer requestedQty) {}

    /**
     * Reserves the requested quantities. On first line that can't be fulfilled
     * the whole transaction rolls back — no partial reservations persist.
     */
    @Transactional
    public ReservationOutcome reserve(Long orderId, Map<Long, Integer> productQuantities) {
        List<Reservation> toPersist = new ArrayList<>();

        for (var entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            InventoryItem item = items.findById(productId).orElse(null);
            if (item == null || item.getAvailableStock() < qty) {
                log.warn("Reservation failed for order={} productId={} need={} have={}",
                        orderId, productId,
                        qty, item == null ? 0 : item.getAvailableStock());
                // Rollback via exception — clean unwind of anything saved above.
                throw new OutOfStockException(productId, qty);
            }

            item.setAvailableStock(item.getAvailableStock() - qty);
            item.setReservedStock(item.getReservedStock() + qty);
            items.save(item);

            toPersist.add(Reservation.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(qty)
                    .build());
        }

        reservations.saveAll(toPersist);
        log.info("Reserved {} lines for order={}", toPersist.size(), orderId);
        return new ReservationOutcome(true, null, null);
    }

    /** Releases reservations, returning held stock to available. Idempotent. */
    @Transactional
    public void release(Long orderId) {
        List<Reservation> existing = reservations.findByOrderId(orderId);
        if (existing.isEmpty()) {
            log.info("release(order={}): nothing to release (already gone or never reserved)", orderId);
            return;
        }
        for (Reservation r : existing) {
            items.findById(r.getProductId()).ifPresent(item -> {
                item.setReservedStock(item.getReservedStock() - r.getQuantity());
                item.setAvailableStock(item.getAvailableStock() + r.getQuantity());
                items.save(item);
            });
        }
        reservations.deleteByOrderId(orderId);
        log.info("Released {} reservations for order={}", existing.size(), orderId);
    }

    public static class OutOfStockException extends RuntimeException {
        private final Long productId;
        private final int requested;

        public OutOfStockException(Long productId, int requested) {
            super("Product " + productId + " out of stock (requested " + requested + ")");
            this.productId = productId;
            this.requested = requested;
        }

        public Long getProductId() { return productId; }
        public int getRequested() { return requested; }
    }
}
