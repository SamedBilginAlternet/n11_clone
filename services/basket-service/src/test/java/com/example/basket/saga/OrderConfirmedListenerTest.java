package com.example.basket.saga;

import com.example.basket.service.BasketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderConfirmedListenerTest {

    @Mock BasketService basketService;

    @InjectMocks OrderConfirmedListener listener;

    @Test
    void clearsUserBasketOnOrderConfirmed() {
        listener.onOrderConfirmed(Map.of("orderId", 7, "userEmail", "u@example.com"));

        verify(basketService).clear("u@example.com");
    }
}
