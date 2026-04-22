package com.example.payment.repository;

import com.example.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUserEmailOrderByIdDesc(String userEmail);

    Optional<PaymentTransaction> findByOrderId(Long orderId);
}
