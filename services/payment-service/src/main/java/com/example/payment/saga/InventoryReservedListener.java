package com.example.payment.saga;

import com.example.payment.service.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedListener {

    private final PaymentProcessor paymentProcessor;

    @RabbitListener(queues = SagaTopology.INVENTORY_RESERVED_QUEUE)
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("InventoryReserved orderId={} amount={} — charging", event.orderId(), event.totalAmount());
        paymentProcessor.process(event);
    }
}
