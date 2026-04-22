package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repo;

    @GetMapping
    public List<NotificationResponse> myNotifications(@AuthenticationPrincipal String userEmail) {
        return repo.findByUserEmailOrderByIdDesc(userEmail).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal String userEmail) {
        return Map.of("count", repo.countByUserEmailAndReadFalse(userEmail));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal String userEmail, @PathVariable Long id) {
        Notification n = repo.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bildirim bulunamadı."));
        n.setRead(true);
        return NotificationResponse.from(repo.save(n));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userEmail, @PathVariable Long id) {
        Notification n = repo.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bildirim bulunamadı."));
        repo.delete(n);
        return ResponseEntity.noContent().build();
    }
}
