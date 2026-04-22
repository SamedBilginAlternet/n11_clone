package com.example.search.service;

import com.example.search.document.ProductDocument;
import com.example.search.dto.ProductFromCatalog;
import com.example.search.dto.ProductPage;
import com.example.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Pulls products from product-service on startup and bulk-indexes them.
 *
 * Why HTTP pull instead of events: product-service currently exposes products
 * via Flyway seed (no CRUD API that would emit events). A pull-based
 * reconciliation is fine for this demo and also doubles as a reindex path if
 * the ES volume is wiped. If product-service later publishes
 * product.created/updated/deleted events, this indexer becomes the fallback
 * bootstrap and the primary path moves to a @RabbitListener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexer {

    @Value("${product-service.base-url:http://localhost:8082}")
    private String productServiceBaseUrl;

    private final ProductSearchRepository repo;
    private final WebClient.Builder webClientBuilder;

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        log.info("Starting Elasticsearch product index bootstrap from {}", productServiceBaseUrl);
        try {
            indexAll();
        } catch (Exception ex) {
            // Don't fail app startup — product-service might not be up yet
            // in docker-compose. Search is degraded until next manual reindex.
            log.error("Initial index failed; search will be empty until /api/search/reindex is hit", ex);
        }
    }

    public long indexAll() {
        WebClient client = webClientBuilder.baseUrl(productServiceBaseUrl).build();
        int page = 0;
        int size = 100;
        long indexed = 0;

        while (true) {
            final int currentPage = page;
            ProductPage pageData = client.get()
                    .uri(uri -> uri.path("/api/products")
                            .queryParam("page", currentPage)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(ProductPage.class)
                    .block(Duration.ofSeconds(15));

            if (pageData == null || pageData.content().isEmpty()) break;

            List<ProductDocument> docs = pageData.content().stream().map(this::toDocument).toList();
            repo.saveAll(docs);
            indexed += docs.size();

            if (page + 1 >= pageData.totalPages()) break;
            page++;
        }

        log.info("Indexed {} products into Elasticsearch", indexed);
        return indexed;
    }

    private ProductDocument toDocument(ProductFromCatalog p) {
        return ProductDocument.builder()
                .id(p.id())
                .name(p.name())
                .slug(p.slug())
                .description(p.description())
                .category(p.category())
                .brand(p.brand())
                .price(p.price())
                .discountedPrice(p.discountedPrice())
                .discountPercentage(p.discountPercentage())
                .stockQuantity(p.stockQuantity())
                .imageUrl(p.imageUrl())
                .rating(p.rating())
                .reviewCount(p.reviewCount())
                .build();
    }
}
