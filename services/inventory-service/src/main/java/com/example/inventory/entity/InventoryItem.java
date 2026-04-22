package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @Column(name = "product_id")
    private Long productId;

    /** Units currently sellable (not reserved, not sold). */
    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    /** Units held for pending orders. Decremented → available on cancel; consumed on confirm. */
    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;
}
