package com.example.order.controller;

import com.example.order.dto.CheckoutRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Sipariş ve ödeme (saga)")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Sepetten sipariş oluşturur, saga'yı başlatır")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody CheckoutRequest req
    ) {
        OrderResponse resp = orderService.checkout(userEmail, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    @GetMapping
    @Operation(summary = "Kullanıcının siparişlerini listeler")
    public List<OrderResponse> list(@AuthenticationPrincipal String userEmail) {
        return orderService.listForUser(userEmail);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sipariş detayı")
    public OrderResponse get(@AuthenticationPrincipal String userEmail, @PathVariable Long id) {
        return orderService.getForUser(id, userEmail);
    }
}
