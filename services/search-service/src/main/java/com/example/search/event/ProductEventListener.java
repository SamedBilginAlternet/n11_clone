package com.example.search.event;

import com.example.search.config.ProductEventRabbitConfig;
import com.example.search.document.ProductDocument;
import com.example.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final ProductSearchRepository repository;

    @RabbitListener(queues = ProductEventRabbitConfig.QUEUE_CREATED)
    public void onProductCreated(ProductEvent event) {
        log.info("Product created: id={} name={}", event.productId(), event.name());
        repository.save(toDocument(event));
    }

    @RabbitListener(queues = ProductEventRabbitConfig.QUEUE_UPDATED)
    public void onProductUpdated(ProductEvent event) {
        log.info("Product updated: id={} name={}", event.productId(), event.name());
        repository.save(toDocument(event));
    }

    @RabbitListener(queues = ProductEventRabbitConfig.QUEUE_DELETED)
    public void onProductDeleted(ProductEvent event) {
        log.info("Product deleted: id={}", event.productId());
        repository.deleteById(event.productId());
    }

    private ProductDocument toDocument(ProductEvent e) {
        return ProductDocument.builder()
                .id(e.productId())
                .name(e.name())
                .slug(e.slug())
                .description(e.description())
                .category(e.category())
                .brand(e.brand())
                .price(e.price() != null ? e.price() : 0)
                .discountedPrice(e.discountedPrice() != null ? e.discountedPrice() : 0)
                .discountPercentage(e.discountPercentage() != null ? e.discountPercentage() : 0)
                .stockQuantity(e.stockQuantity() != null ? e.stockQuantity() : 0)
                .imageUrl(e.imageUrl())
                .rating(e.rating() != null ? e.rating() : 0)
                .reviewCount(e.reviewCount() != null ? e.reviewCount() : 0)
                .build();
    }
}
