package com.example.inventory.repository;

import com.example.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * Row-level pessimistic lock so concurrent OrderCreated events for the same
     * product can't both reserve the last unit. RabbitMQ's default single
     * consumer per queue usually makes this academic, but defence-in-depth.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findById(Long productId);
}
