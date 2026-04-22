package com.example.inventory.repository;

import com.example.inventory.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
