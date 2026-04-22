package com.example.notification.service;

import com.example.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Notification notification) {
        log.info("Pushing notification to {} via WebSocket", notification.getUserEmail());
        messagingTemplate.convertAndSendToUser(
                notification.getUserEmail(),
                "/queue/notifications",
                notification
        );
    }
}
