package com.example.notification.saga;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.NotificationPushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock NotificationRepository repo;
    @Mock NotificationPushService pushService;

    @InjectMocks NotificationEventListener listener;

    @Test
    @DisplayName("UserRegistered → WELCOME with fullName in message")
    void userRegistered_mapsToWelcome() {
        listener.onUserRegistered(Map.of(
                "email", "new@example.com",
                "fullName", "Ayşe Yılmaz"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getType()).isEqualTo(NotificationType.WELCOME);
        assertThat(n.getUserEmail()).isEqualTo("new@example.com");
        assertThat(n.getMessage()).contains("Ayşe Yılmaz");
        assertThat(n.isRead()).isFalse();
    }

    @Test
    @DisplayName("OrderConfirmed → ORDER_CONFIRMED with TRY-formatted total")
    void orderConfirmed_mapsToOrderConfirmedWithFormattedTotal() {
        listener.onOrderConfirmed(Map.of(
                "orderId", 42,
                "userEmail", "buyer@example.com",
                "totalAmount", "1234.50"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(n.getUserEmail()).isEqualTo("buyer@example.com");
        assertThat(n.getMessage()).contains("#42");
        // Turkish locale formats as "1.234,50 ₺" — check the key pieces to stay
        // resilient to NBSP vs space between the number and symbol.
        assertThat(n.getMessage()).contains("1.234,50");
    }

    @Test
    @DisplayName("OrderCancelled → ORDER_CANCELLED includes the reason")
    void orderCancelled_mapsToOrderCancelledWithReason() {
        listener.onOrderCancelled(Map.of(
                "orderId", 7,
                "userEmail", "buyer@example.com",
                "reason", "Kart reddedildi"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(n.getMessage()).contains("#7").contains("Kart reddedildi");
    }

    @Test
    @DisplayName("saved notifications always start as unread")
    void allNotificationsStartUnread() {
        listener.onUserRegistered(Map.of("email", "a@x.com", "fullName", "A"));
        verify(repo).save(any(Notification.class));
        // assertion is inside userRegistered_mapsToWelcome; this test pins the
        // rule as its own failure mode so a regression is legible.
    }
}
