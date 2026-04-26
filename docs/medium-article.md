# Building a Production-Grade Microservices E-Commerce Platform with Spring Boot, Docker & Full Observability

*A deep dive into designing 11 microservices with Saga choreography, JWT security, Redis caching, Elasticsearch, RabbitMQ event-driven communication, and a complete observability stack — deployed on a single VPS.*

---

## Introduction

Microservices architecture promises scalability, team autonomy, and resilience — but delivering on that promise requires more than just splitting a monolith into smaller apps. You need proper inter-service communication, distributed transactions, centralized observability, and a deployment strategy that actually works.

In this article, I'll walk you through a **full-stack e-commerce platform** I built from scratch using **Java 21, Spring Boot 3.3, React 18, and Docker**. The system consists of **11 microservices**, handles distributed transactions via the **Saga pattern**, and includes a **complete observability stack** with Prometheus, Grafana, Jaeger, and Loki.

**Live demo:** [https://n11.samedbilgin.com](https://n11.samedbilgin.com)
**Source code:** [GitHub Repository](https://github.com/SamedBilginAlternet/n11_clone)

---

## Architecture Overview

```
                          ┌──────────────┐
                          │   Frontend   │
                          │  React + TS  │
                          └──────┬───────┘
                                 │ HTTPS
                          ┌──────▼───────┐
                          │  API Gateway │
                          │ Spring Cloud │
                          └──────┬───────┘
                                 │ lb://
                ┌────────────────┼────────────────┐
                │                │                │
         ┌──────▼──┐     ┌──────▼──┐     ┌───────▼───┐
         │  Auth   │     │ Product │     │  Basket   │
         │ Service │     │ Service │     │  Service  │
         └────┬────┘     └────┬────┘     └─────┬─────┘
              │               │                │
              │          ┌────▼────┐           │
              │          │  Redis  │           │
              │          │  Cache  │           │
              │          └─────────┘           │
              │                                │
    ┌─────────▼────────────────────────────────▼──────────┐
    │                  RabbitMQ (saga.exchange)            │
    │    Topic Exchange — Choreography-based Sagas         │
    └───┬────────┬──────────┬──────────┬──────────┬───────┘
        │        │          │          │          │
   ┌────▼──┐ ┌──▼────┐ ┌───▼───┐ ┌───▼────┐ ┌───▼──────┐
   │ Order │ │Payment│ │Inventory│ │Notific.│ │  Search  │
   │Service│ │Service│ │ Service │ │Service │ │  Service │
   └───┬───┘ └───┬───┘ └───┬────┘ └───┬────┘ └────┬─────┘
       │         │         │          │            │
   ┌───▼─────────▼─────────▼──┐       │     ┌─────▼──────┐
   │      PostgreSQL (×7)     │       │     │Elasticsearch│
   │   Per-service databases  │       │     │  products   │
   └──────────────────────────┘       │     └─────────────┘
                                      │
                               ┌──────▼──────┐
                               │  WebSocket  │
                               │  (STOMP)    │
                               └─────────────┘
```

The platform has **16 containers** in total:

| Layer | Components |
|-------|-----------|
| **Infrastructure** | PostgreSQL, RabbitMQ, Redis, Elasticsearch |
| **Platform** | Eureka Server, API Gateway |
| **Business** | Auth, Product, Basket, Order, Payment, Notification, Review, Search, Inventory |
| **Frontend** | React SPA served via Nginx |
| **Observability** | Prometheus, Grafana, Jaeger, Loki, OpenTelemetry Collector, Promtail |

---

## Key Architectural Patterns

### 1. Saga Pattern (Choreography-Based)

The hardest problem in microservices is **distributed transactions**. When a user checks out, we need to:

1. Create an order
2. Reserve inventory
3. Process payment
4. Send notification
5. Clear the basket

If payment fails at step 3, we must **compensate** — release inventory and cancel the order. I implemented this using the **Saga Choreography** pattern with RabbitMQ as the event bus.

**Checkout Saga Flow:**

```
basket-service          order-service         inventory-service
     │                       │                       │
     │──checkout request───▶│                       │
     │                       │──order.created──────▶│
     │                       │                       │──reserve stock
     │                       │                       │
     │                       │    ┌──────────────────│
     │                       │    │ inventory.reserved│
     │                       │◀───┘                  │
     │                       │                       │
     │               payment-service                 │
     │                       │──────────────────────▶│
     │                       │   payment.succeeded   │
     │                       │◀──────────────────────│
     │                       │                       │
     │   order.confirmed     │   notification-service│
     │◀──────────────────────│──────────────────────▶│
     │   (clear basket)      │                  (send notif)
```

All communication happens through a **single RabbitMQ topic exchange** (`saga.exchange`) with 9 routing keys:

```java
// SagaEventPublisher.java
public void publish(String routingKey, Object event) {
    rabbitTemplate.convertAndSend("saga.exchange", routingKey, event);
}

// Usage
sagaEventPublisher.publish("order.created", orderCreatedEvent);
sagaEventPublisher.publish("inventory.reserved", inventoryReservedEvent);
sagaEventPublisher.publish("payment.succeeded", paymentSucceededEvent);
```

**Compensation on failure:**

```java
@RabbitListener(queues = "order.payment-failed")
public void onPaymentFailed(PaymentFailedEvent event) {
    Order order = orderRepository.findById(event.orderId()).orElseThrow();
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);

    // Trigger inventory release
    sagaEventPublisher.publish("order.cancelled",
        new OrderCancelledEvent(order.getId(), order.getItems()));
}
```

### 2. API Gateway Pattern

Spring Cloud Gateway handles all incoming traffic with **reactive, non-blocking** routing:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/api/auth/**
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
```

The `lb://` prefix enables **client-side load balancing** via Eureka service discovery. A custom `RequestLoggingGlobalFilter` generates and propagates **Correlation IDs** across all downstream calls:

```java
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = Optional.ofNullable(
            exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
            .orElse(UUID.randomUUID().toString());

        exchange.getRequest().mutate()
            .header("X-Correlation-Id", correlationId);

        return chain.filter(exchange);
    }
}
```

### 3. CQRS (Command Query Responsibility Segregation)

Product data lives in **two stores** optimized for different access patterns:

- **Write model:** PostgreSQL (product-service) — normalized, ACID-compliant
- **Read model:** Elasticsearch (search-service) — denormalized, optimized for full-text search

Synchronization happens via RabbitMQ events:

```java
// product-service publishes on create/update/delete
sagaEventPublisher.publish("product.created", productEvent);

// search-service consumes and indexes
@RabbitListener(queues = "search.product-events")
public void onProductEvent(ProductEvent event) {
    switch (event.type()) {
        case CREATED, UPDATED -> elasticsearchOps.save(toDocument(event));
        case DELETED -> elasticsearchOps.delete(event.productId());
    }
}
```

### 4. Redis Caching Strategy

Product-service uses a **multi-tier cache** with different TTLs:

```java
@Cacheable(value = "product:id", key = "#id")
public ProductResponse getById(Long id) { ... }  // TTL: 30 min

@Cacheable(value = "product:categories")
public List<String> getCategories() { ... }       // TTL: 24 hours

@Caching(evict = {
    @CacheEvict(value = "product:id", key = "#id"),
    @CacheEvict(value = "product:categories", allEntries = true)
})
public ProductResponse update(Long id, ProductRequest req) { ... }
```

A custom **CacheErrorHandler** ensures the app continues working if Redis goes down:

```java
@Override
public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
    log.warn("Cache GET failed for key={}: {}", key, e.getMessage());
    // Returns null → service hits DB as fallback
}
```

---

## Security Architecture

### JWT with Refresh Token Rotation

```
Client                    Auth Service                Database
  │                            │                         │
  │──POST /api/auth/login────▶│                         │
  │                            │──validate credentials──▶│
  │                            │◀─────────user data──────│
  │                            │                         │
  │                            │──generate access token  │
  │                            │──generate refresh token │
  │                            │──store refresh token───▶│
  │◀──{accessToken,           │                         │
  │    refreshToken}───────────│                         │
  │                            │                         │
  │──GET /api/products─────────────────────────────────▶│
  │  Authorization: Bearer {accessToken}                 │
  │◀──200 OK─────────────────────────────────────────────│
  │                            │                         │
  │  (access token expires after 15 min)                 │
  │                            │                         │
  │──POST /api/auth/refresh──▶│                         │
  │  {refreshToken}            │──validate & revoke old─▶│
  │                            │──issue new pair────────▶│
  │◀──{new accessToken,       │                         │
  │    new refreshToken}───────│                         │
```

Key security features:
- **Access tokens:** 15-minute expiry, HS256 signed, stateless
- **Refresh tokens:** 7-day expiry, stored in DB, one-per-user, revoked on reuse
- **Rate limiting:** Bucket4j token bucket — 10 requests/minute on auth endpoints
- **Password hashing:** BCrypt with 10 rounds
- **Custom validation:** `@StrongPassword` annotation enforces complexity rules

### Per-Request JWT Validation

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, ...) {
        String token = extractToken(req);
        if (token != null && jwtService.isValid(token)) {
            String username = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);

            var auth = new UsernamePasswordAuthenticationToken(
                username, null,
                roles.stream().map(SimpleGrantedAuthority::new).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(req, res);
    }
}
```

---

## Observability: The Three Pillars

### Metrics (Prometheus + Grafana)

Every service exposes `/actuator/prometheus` with Micrometer. Prometheus scrapes every 15 seconds:

- HTTP request rate, latency (P50, P95, P99), error rate
- JVM heap usage, GC pauses, thread count
- HikariCP connection pool utilization
- RabbitMQ consumer lag

### Distributed Tracing (OpenTelemetry + Jaeger)

A single checkout request touches **8 services**. OpenTelemetry auto-instruments Spring Boot and propagates trace context:

```
Trace: checkout-abc123
├── api-gateway (2ms)
│   └── basket-service (15ms)
│       └── order-service (8ms)
│           ├── inventory-service (12ms)
│           │   └── PostgreSQL query (3ms)
│           └── payment-service (45ms)
│               └── notification-service (5ms)
│                   └── WebSocket push (1ms)
```

### Centralized Logging (Promtail + Loki)

All containers log to stdout. Promtail discovers Docker containers and ships logs to Loki with labels:

```
2026-04-26 14:30:12.456 INFO  [abc123,def456] OrderService -
  Order created: orderId=42, userId=7, total=299.90 TRY
```

Every log line includes `[correlationId, traceId]` — click a trace in Jaeger, jump to its logs in Grafana.

---

## Full-Text Search with Elasticsearch

The search service provides **Turkish-aware** full-text search:

```java
public SearchResult search(String query, SearchFilters filters) {
    NativeQuery nq = NativeQuery.builder()
        .withQuery(q -> q.bool(b -> {
            // Multi-match with field boosting
            b.must(m -> m.multiMatch(mm -> mm
                .query(query)
                .fields("name^3", "brand^2", "description")
                .fuzziness("AUTO")
            ));
            // Price range filter
            if (filters.minPrice() != null)
                b.filter(f -> f.range(r -> r.field("price")
                    .gte(JsonData.of(filters.minPrice()))));
            return b;
        }))
        .withAggregation("brands", a -> a.terms(t -> t.field("brand.keyword")))
        .withAggregation("categories", a -> a.terms(t -> t.field("category.keyword")))
        .build();

    return elasticsearchOps.search(nq, ProductDocument.class);
}
```

Features:
- **Fuzzy matching** — typo tolerance via Levenshtein distance
- **Field boosting** — product name matches rank 3× higher than description
- **Faceted search** — aggregations for brand/category filters
- **Turkish analyzer** — proper stemming and diacritical normalization

---

## Real-Time Notifications via WebSocket

When a saga event completes (order confirmed, payment processed), the notification service pushes real-time updates to the user's browser via **STOMP over WebSocket**:

```java
@RabbitListener(queues = "notification.order-confirmed")
public void onOrderConfirmed(OrderConfirmedEvent event) {
    Notification notif = Notification.builder()
        .userId(event.userId())
        .title("Order Confirmed!")
        .message("Your order #" + event.orderId() + " has been confirmed.")
        .build();

    notificationRepository.save(notif);
    messagingTemplate.convertAndSendToUser(
        event.userId().toString(), "/queue/notifications", notif);
}
```

The frontend connects with JWT authentication on the WebSocket handshake:

```typescript
const client = new Client({
  brokerURL: 'wss://n11.samedbilgin.com/ws',
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    client.subscribe('/user/queue/notifications', (msg) => {
      toast.success(JSON.parse(msg.body).title);
    });
  }
});
```

---

## Database Per Service

Each microservice owns its database schema, enforced by **Flyway migrations**:

```
authdb          ← auth-service (users, refresh_tokens)
basketdb        ← basket-service (baskets, basket_items)
productdb       ← product-service (products, categories)
orderdb         ← order-service (orders, order_items)
paymentdb       ← payment-service (payment_transactions)
notificationdb  ← notification-service (notifications)
reviewdb        ← review-service (reviews)
inventorydb     ← inventory-service (inventory, reservations)
```

All use `ddl-auto: validate` — Hibernate only validates the schema matches entities. **Flyway handles all DDL changes** with versioned migration scripts.

---

## Frontend: React + TypeScript + Zustand

The SPA uses a modern stack:

- **React 18** with TypeScript for type safety
- **Zustand** for lightweight state management (auth state, basket)
- **Radix UI** for accessible, unstyled components
- **Tailwind CSS** for utility-first styling
- **Framer Motion** for animations
- **React Router 6** for SPA routing
- **STOMP.js** for real-time WebSocket notifications

Nginx serves the built SPA and proxies `/api/*` to the gateway — **zero CORS issues** in production.

---

## Deployment: Single VPS with Docker Compose

The entire platform runs on a **DigitalOcean Droplet** ($24/month, 4 vCPU, 8 GB RAM) with a lite Docker Compose configuration:

```bash
# Clone and deploy
git clone https://github.com/SamedBilginAlternet/n11_clone.git
cd n11_clone
docker compose -f docker-compose.lite.yml up -d --build
```

**Memory budget (4 GB RAM):**

| Component | RAM |
|-----------|-----|
| 9 Spring Boot services (96 MB heap each) | ~1.7 GB |
| Eureka + Gateway (80 MB heap each) | ~320 MB |
| PostgreSQL + RabbitMQ + Redis | ~450 MB |
| Elasticsearch (128 MB heap) | ~256 MB |
| Frontend (Nginx) | ~16 MB |
| OS + Docker | ~400 MB |
| **Total** | **~3.1 GB** |

SSL via Let's Encrypt (auto-renewal with Certbot).

---

## Patterns Summary

| Pattern | Implementation |
|---------|---------------|
| **Microservices** | 11 Spring Boot services, Docker Compose |
| **API Gateway** | Spring Cloud Gateway, reactive routing |
| **Service Discovery** | Netflix Eureka |
| **Saga (Choreography)** | RabbitMQ topic exchange, 9 event types |
| **CQRS** | PostgreSQL (write) + Elasticsearch (read) |
| **Per-Service Database** | 8 PostgreSQL databases + Flyway |
| **Caching** | Redis with multi-tier TTL strategy |
| **JWT + Refresh Tokens** | Stateless auth, token rotation |
| **Rate Limiting** | Bucket4j, 10 req/min on auth |
| **Distributed Tracing** | OpenTelemetry → Jaeger |
| **Centralized Metrics** | Micrometer → Prometheus → Grafana |
| **Centralized Logging** | Promtail → Loki → Grafana |
| **Full-Text Search** | Elasticsearch, Turkish analyzer, facets |
| **Real-Time Updates** | WebSocket (STOMP), user-specific delivery |
| **Event-Driven Sync** | Product events → Elasticsearch indexing |

---

## Lessons Learned

1. **Saga choreography is elegant but hard to debug.** Without distributed tracing (Jaeger) and correlation IDs, you're blind. Invest in observability *before* building sagas.

2. **Redis cache-aside with fallback is essential.** Redis goes down more often than you'd think. A graceful degradation to DB prevents cascading failures.

3. **Flyway > ddl-auto.** Never use `ddl-auto: update` in production. Flyway gives you version-controlled, repeatable migrations.

4. **Docker Compose is enough for small-scale.** You don't need Kubernetes for a portfolio project. Docker Compose with proper health checks and restart policies handles 16 containers just fine.

5. **The observability stack pays for itself.** The first time you trace a 503 through 5 services using Jaeger, you'll never go back to `System.out.println`.

---

## What's Next?

- **Kubernetes deployment** with Helm charts
- **CI/CD pipeline** with GitHub Actions
- **API versioning** and backward compatibility
- **Circuit breaker** with Resilience4j
- **Distributed caching** with Redis Cluster

---

*If you found this helpful, follow me for more deep dives into microservices architecture and Spring Boot.*

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Cloud Gateway, React 18, TypeScript, PostgreSQL, Redis, RabbitMQ, Elasticsearch, Docker, Nginx, Let's Encrypt, Prometheus, Grafana, Jaeger, Loki, OpenTelemetry

**Tags:** #Microservices #SpringBoot #Java #Docker #Architecture #SagaPattern #Redis #Elasticsearch #Observability
