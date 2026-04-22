package com.example.order.service;

import com.example.order.dto.CheckoutItemRequest;
import com.example.order.dto.CheckoutRequest;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.saga.OrderCreatedEvent;
import com.example.order.saga.SagaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock SagaEventPublisher sagaEventPublisher;

    @InjectMocks OrderService orderService;

    private static final String EMAIL = "user@example.com";

    // ────────────────────────────────────────────────────────────────────
    // checkout
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkout persists PENDING order, computes total server-side, publishes OrderCreated")
    void checkout_happyPath() {
        var req = new CheckoutRequest("Istanbul, Kadıköy", List.of(
                new CheckoutItemRequest(10L, "A", new BigDecimal("100"), null, 2),   // 200
                new CheckoutItemRequest(11L, "B", new BigDecimal("50.50"), null, 3)  // 151.50
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        var response = orderService.checkout(EMAIL, req);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("351.50");
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(sagaEventPublisher).publishOrderCreated(event.capture());
        assertThat(event.getValue().userEmail()).isEqualTo(EMAIL);
        assertThat(event.getValue().totalAmount()).isEqualByComparingTo("351.50");
    }

    // ────────────────────────────────────────────────────────────────────
    // getForUser — scoped to owning user
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getForUser throws 404 when order belongs to someone else")
    void getForUser_wrongUser404() {
        when(orderRepository.findByIdAndUserEmail(1L, "mallory@x.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getForUser(1L, "mallory@x.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Sipariş bulunamadı");
    }

    // ────────────────────────────────────────────────────────────────────
    // markPaid / markCancelled — saga state transitions
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markPaid flips status to PAID and fans out OrderConfirmed")
    void markPaid_updatesAndPublishes() {
        Order order = Order.builder().id(1L).userEmail(EMAIL)
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("100"))
                .items(new java.util.ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markPaid(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(sagaEventPublisher).publishOrderConfirmed(order);
    }

    @Test
    @DisplayName("markCancelled records reason and fans out OrderCancelled")
    void markCancelled_recordsReasonAndPublishes() {
        Order order = Order.builder().id(1L).userEmail(EMAIL)
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("100"))
                .items(new java.util.ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markCancelled(1L, "Kart reddedildi");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("Kart reddedildi");
        verify(sagaEventPublisher).publishOrderCancelled(order, "Kart reddedildi");
    }

    @Test
    @DisplayName("markPaid on a missing order is a no-op (event replay safety)")
    void markPaid_missingOrderIsNoop() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        orderService.markPaid(99L);

        verify(sagaEventPublisher, never()).publishOrderConfirmed(any());
    }
}
