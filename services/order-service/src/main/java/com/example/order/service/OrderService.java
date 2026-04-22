package com.example.order.service;

import com.example.order.dto.CheckoutRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.saga.OrderCreatedEvent;
import com.example.order.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaEventPublisher sagaEventPublisher;

    @Transactional
    public OrderResponse checkout(String userEmail, CheckoutRequest req) {
        // Compute total on the server — never trust client totals.
        BigDecimal total = req.items().stream()
                .map(i -> i.productPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .shippingAddress(req.shippingAddress())
                .build();

        req.items().forEach(i -> order.getItems().add(OrderItem.builder()
                .order(order)
                .productId(i.productId())
                .productName(i.productName())
                .productPrice(i.productPrice())
                .imageUrl(i.imageUrl())
                .quantity(i.quantity())
                .build()));

        orderRepository.save(order);
        log.info("Order persisted PENDING id={} email={} total={}", order.getId(), userEmail, total);

        // Kick off saga: payment-service will charge and respond with
        // PaymentSucceeded or PaymentFailed on the saga exchange.
        sagaEventPublisher.publishOrderCreated(
                OrderCreatedEvent.of(order.getId(), userEmail, total));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForUser(String userEmail) {
        return orderRepository.findByUserEmailOrderByIdDesc(userEmail).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(Long id, String userEmail) {
        return orderRepository.findByIdAndUserEmail(id, userEmail)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sipariş bulunamadı: " + id));
    }

    @Transactional
    public void markPaid(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order {} marked PAID — publishing OrderConfirmed", orderId);
            sagaEventPublisher.publishOrderConfirmed(order);
        }, () -> log.warn("markPaid: order {} not found", orderId));
    }

    @Transactional
    public void markCancelled(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setFailureReason(reason);
            orderRepository.save(order);
            log.info("Order {} marked CANCELLED reason={} — publishing OrderCancelled", orderId, reason);
            sagaEventPublisher.publishOrderCancelled(order, reason);
        }, () -> log.warn("markCancelled: order {} not found", orderId));
    }
}
