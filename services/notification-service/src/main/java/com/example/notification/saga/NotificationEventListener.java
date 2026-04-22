package com.example.notification.saga;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Pure consumer — no outbound events. Turns saga events into user-facing
 * in-app notifications stored in notificationdb. Messages are Turkish so the
 * UI can render them directly without a translation layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final NumberFormat TRY =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR"));

    private final NotificationRepository repo;

    @RabbitListener(queues = SagaTopology.USER_REGISTERED_QUEUE)
    public void onUserRegistered(Map<String, Object> event) {
        String email = (String) event.get("email");
        String fullName = (String) event.get("fullName");
        log.info("UserRegistered notification email={}", email);
        save(email, NotificationType.WELCOME,
                "Aramıza hoş geldin!",
                "Merhaba " + fullName + ", n11 Clone'a kaydın başarıyla tamamlandı. İyi alışverişler!");
    }

    @RabbitListener(queues = SagaTopology.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(Map<String, Object> event) {
        String email = (String) event.get("userEmail");
        Long orderId = ((Number) event.get("orderId")).longValue();
        BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());
        log.info("OrderConfirmed notification email={} orderId={}", email, orderId);
        save(email, NotificationType.ORDER_CONFIRMED,
                "Siparişin onaylandı",
                "#" + orderId + " numaralı siparişin başarıyla alındı. Tutar: " + TRY.format(amount));
    }

    @RabbitListener(queues = SagaTopology.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(Map<String, Object> event) {
        String email = (String) event.get("userEmail");
        Long orderId = ((Number) event.get("orderId")).longValue();
        String reason = (String) event.get("reason");
        log.info("OrderCancelled notification email={} orderId={}", email, orderId);
        save(email, NotificationType.ORDER_CANCELLED,
                "Siparişin iptal edildi",
                "#" + orderId + " numaralı siparişin ödeme sırasında iptal edildi. Sebep: " + reason);
    }

    private void save(String email, NotificationType type, String title, String message) {
        repo.save(Notification.builder()
                .userEmail(email)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build());
    }
}
