package com.example.payment.controller;

import com.example.payment.dto.PaymentResponse;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping
    public List<PaymentResponse> myPayments(@AuthenticationPrincipal String userEmail) {
        return paymentRepository.findByUserEmailOrderByIdDesc(userEmail).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getByOrderId(@AuthenticationPrincipal String userEmail, @PathVariable Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .filter(t -> t.getUserEmail().equals(userEmail))
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ödeme kaydı bulunamadı."));
    }
}
