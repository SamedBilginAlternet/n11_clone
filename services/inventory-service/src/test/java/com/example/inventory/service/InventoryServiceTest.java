package com.example.inventory.service;

import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.Reservation;
import com.example.inventory.repository.InventoryItemRepository;
import com.example.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryItemRepository items;
    @Mock ReservationRepository reservations;

    @InjectMocks InventoryService service;

    private InventoryItem stock(long id, int available) {
        return InventoryItem.builder()
                .productId(id)
                .availableStock(available)
                .reservedStock(0)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────
    // reserve — happy path
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve decrements available, increments reserved, persists Reservation rows")
    void reserve_happyPath() {
        InventoryItem item10 = stock(10L, 5);
        InventoryItem item20 = stock(20L, 3);
        when(items.findById(10L)).thenReturn(Optional.of(item10));
        when(items.findById(20L)).thenReturn(Optional.of(item20));

        var outcome = service.reserve(42L, Map.of(10L, 2, 20L, 1));

        assertThat(outcome.success()).isTrue();
        assertThat(item10.getAvailableStock()).isEqualTo(3);
        assertThat(item10.getReservedStock()).isEqualTo(2);
        assertThat(item20.getAvailableStock()).isEqualTo(2);
        assertThat(item20.getReservedStock()).isEqualTo(1);
        verify(reservations).saveAll(any());
    }

    // ────────────────────────────────────────────────────────────────────
    // reserve — rollback when any line fails
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve throws OutOfStockException when a line can't be fulfilled")
    void reserve_outOfStockThrows() {
        when(items.findById(10L)).thenReturn(Optional.of(stock(10L, 1))); // have 1, need 5

        assertThatThrownBy(() -> service.reserve(42L, Map.of(10L, 5)))
                .isInstanceOf(InventoryService.OutOfStockException.class);
    }

    @Test
    @DisplayName("reserve throws for unknown product ids")
    void reserve_unknownProductThrows() {
        when(items.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(42L, Map.of(999L, 1)))
                .isInstanceOf(InventoryService.OutOfStockException.class);
    }

    // ────────────────────────────────────────────────────────────────────
    // release — restores stock, idempotent
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("release returns reserved units to available and deletes reservations")
    void release_restoresStock() {
        InventoryItem item = InventoryItem.builder().productId(10L).availableStock(3).reservedStock(2).build();
        Reservation r = Reservation.builder().id(1L).orderId(42L).productId(10L).quantity(2).build();
        when(reservations.findByOrderId(42L)).thenReturn(List.of(r));
        when(items.findById(10L)).thenReturn(Optional.of(item));

        service.release(42L);

        assertThat(item.getAvailableStock()).isEqualTo(5);
        assertThat(item.getReservedStock()).isZero();
        verify(reservations).deleteByOrderId(42L);
    }

    @Test
    @DisplayName("release is a no-op when no reservations exist for the order (replay safety)")
    void release_noReservationsIsNoop() {
        when(reservations.findByOrderId(42L)).thenReturn(List.of());

        service.release(42L);

        verify(reservations, never()).deleteByOrderId(any());
        verify(items, never()).save(any());
    }
}
