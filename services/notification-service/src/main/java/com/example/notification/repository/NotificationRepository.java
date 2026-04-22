package com.example.notification.repository;

import com.example.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByIdDesc(String userEmail);

    long countByUserEmailAndReadFalse(String userEmail);

    Optional<Notification> findByIdAndUserEmail(Long id, String userEmail);
}
