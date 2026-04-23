package com.example.product.event;

import com.example.product.config.ProductRabbitConfig;
import com.example.product.dto.ProductResponse;
import com.example.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(Product product) {
        publish(ProductEvent.CREATED, product);
    }

    public void publishUpdated(Product product) {
        publish(ProductEvent.UPDATED, product);
    }

    public void publishDeleted(Long productId) {
        var event = new ProductEvent(ProductEvent.DELETED, productId,
                null, null, null, null, null, 0, 0, null, null, null, 0, 0);
        log.info("Publishing {} productId={}", ProductEvent.DELETED, productId);
        rabbitTemplate.convertAndSend(ProductRabbitConfig.EXCHANGE, ProductEvent.DELETED, event);
    }

    private void publish(String eventType, Product p) {
        var response = ProductResponse.from(p);
        var event = new ProductEvent(eventType, p.getId(), p.getName(), p.getSlug(),
                p.getDescription(), p.getPrice(), response.discountedPrice(),
                p.getDiscountPercentage(), p.getStockQuantity(), p.getImageUrl(),
                p.getCategory(), p.getBrand(), p.getRating(), p.getReviewCount());
        log.info("Publishing {} productId={} name={}", eventType, p.getId(), p.getName());
        rabbitTemplate.convertAndSend(ProductRabbitConfig.EXCHANGE, eventType, event);
    }
}
