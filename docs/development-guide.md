# Development Guide

This document covers everything you need to develop on the n11 Clone project: prerequisites, project structure, how to add new services, endpoints, saga events, and frontend features, plus code conventions.

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **Java** | 21 (LTS) | Backend services runtime |
| **Maven** | 3.9+ | Build tool (each service ships Maven Wrapper) |
| **Node.js** | 20 (LTS) | Frontend build |
| **npm** | 10+ | Frontend package manager |
| **Docker** | 24+ | Container runtime |
| **Docker Compose** | v2 | Multi-container orchestration |
| **PostgreSQL** | 16 | Database (only for local-without-Docker dev) |
| **RabbitMQ** | 3.13 | Message broker (only for local-without-Docker dev) |
| **Elasticsearch** | 8.14 | Search engine (only for search-service local dev) |

### Quick Check

```bash
java --version       # OpenJDK 21.x
node --version       # v20.x
docker --version     # Docker 24+
docker compose version  # v2.x
```

---

## Running the Full Stack

The easiest way to run everything:

```bash
# From the project root
./setup-ports.sh          # Optional: auto-discover free ports
docker compose up --build # Build and start all 19 containers
```

First cold start takes 1-2 minutes (ES cluster, product indexing). Subsequent starts are faster due to cached Docker layers and volumes.

### Running a Single Service Locally

For rapid iteration on one service while others run in Docker:

```bash
# Start infra + other services in Docker
docker compose up postgres rabbitmq elasticsearch jaeger

# Run the target service locally
cd services/auth-service
./mvnw spring-boot:run

# Or with a specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Each service's `application.yml` has sensible defaults for local development (connecting to `localhost:5432`, `localhost:5672`, etc.).

### Running the Frontend Locally

```bash
cd frontend
npm install
npm run dev    # Starts Vite dev server at http://localhost:5173
```

The Vite config proxies `/api/*` to `http://localhost:8000` (the gateway) or to wherever `VITE_API_BASE` points.

---

## Project Structure

### Root Level

```
JwtJava/
  docker-compose.yml              # All 19 containers defined here
  setup-ports.sh                  # Port discovery script
  frontend/                       # React SPA
  services/                       # 10 Spring Boot services
    api-gateway/
    auth-service/
    basket-service/
    product-service/
    order-service/
    payment-service/
    notification-service/
    review-service/
    search-service/
    inventory-service/
  infra/                          # Observability configs
    prometheus/prometheus.yml
    grafana/
      provisioning/
        datasources/datasources.yml
        dashboards/dashboards.yml
      dashboards/n11-overview.json
    loki/loki-config.yaml
    promtail/promtail-config.yaml
```

### Service Internal Structure (example: order-service)

```
services/order-service/
  Dockerfile                      # Multi-stage build
  pom.xml                         # Maven dependencies
  mvnw, mvnw.cmd, .mvn/          # Maven Wrapper
  src/
    main/
      java/com/example/order/
        OrderServiceApplication.java    # @SpringBootApplication
        config/
          SecurityConfig.java           # Security filter chain
          GlobalExceptionHandler.java   # RFC 7807 errors
        controller/
          OrderController.java          # REST endpoints
        dto/
          CheckoutRequest.java          # Request records
          CheckoutItemRequest.java
          OrderResponse.java            # Response records
        entity/
          BaseEntity.java               # Audit fields
          Order.java                    # JPA entity
          OrderItem.java
          OrderStatus.java              # Enum
        filter/
          JwtAuthFilter.java            # JWT authentication
          RequestLoggingFilter.java     # Correlation ID + logging
        repository/
          OrderRepository.java          # Spring Data JPA
        saga/
          SagaTopology.java             # Exchange/queue constants
          SagaRabbitConfig.java         # Bean declarations
          SagaEventPublisher.java       # RabbitTemplate publisher
          PaymentListener.java          # @RabbitListener consumers
          InventoryOutOfStockListener.java
          OrderCreatedEvent.java        # Event records
          OrderConfirmedEvent.java
          OrderCancelledEvent.java
          PaymentSucceededEvent.java
          PaymentFailedEvent.java
        service/
          JwtService.java               # Token parsing
          OrderService.java             # Business logic
      resources/
        application.yml                 # Spring Boot config
        db/migration/
          V1__create_orders.sql         # Flyway migrations
    test/
      java/com/example/order/
        service/OrderServiceTest.java
        saga/PaymentListenerTest.java
      resources/
        application-test.yml            # H2 in-memory for tests
```

### Frontend Structure

```
frontend/
  Dockerfile                        # Multi-stage: npm build -> nginx
  nginx.conf                        # /api/* proxy to gateway
  vite.config.ts
  src/
    app/                            # Router + ErrorBoundary
    layout/                         # Navbar + Layout shell
    shared/
      api/client.ts                 # apiFetch wrapper (RFC 7807 aware)
      api/problem.ts                # Error extraction helpers
      hooks/useApi.ts               # data + loading + error hook
      providers/ToastProvider.tsx    # Toast notifications
      ui/                           # Reusable UI components
      utils/format.ts               # formatTRY, formatDateTime
    features/
      auth/                         # Zustand store, Login/Register pages
      products/                     # Home, ProductDetail, CategoryBar
      basket/                       # BasketPage, Zustand store
      orders/                       # Checkout, OrdersPage, OrderDetail
      reviews/                      # ReviewList (embedded)
      notifications/                # NotificationBell (polling)
      search/                       # SearchPage, FacetSidebar
```

---

## How to Add a New Service

### Step 1: Create the Maven project

```bash
mkdir -p services/my-service/src/main/{java/com/example/myservice,resources/db/migration}
mkdir -p services/my-service/src/test/{java/com/example/myservice,resources}
```

Copy `pom.xml` from an existing service and update:
- `groupId`, `artifactId`, `name`
- Dependencies (add/remove RabbitMQ, ES, etc. as needed)

### Step 2: Create the application class

```java
@SpringBootApplication
@EnableJpaAuditing  // if using BaseEntity
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

### Step 3: Configure application.yml

```yaml
spring:
  application:
    name: my-service
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/myservicedb}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

server:
  port: 8089  # pick the next available port

jwt:
  secret: ${JWT_SECRET:3f6a2b8c1d4e5f7a9b0c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a}
```

### Step 4: Copy cross-cutting files

Copy from any existing service:
- `filter/JwtAuthFilter.java`
- `filter/RequestLoggingFilter.java`
- `config/SecurityConfig.java`
- `config/GlobalExceptionHandler.java`
- `service/JwtService.java`
- `entity/BaseEntity.java` (if needed)

Update package declarations.

### Step 5: Create the Dockerfile

Copy the multi-stage Dockerfile from any service, updating the `EXPOSE` port:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q

FROM deps AS test
COPY src ./src
RUN mvn test -Dspring.profiles.active=test

FROM deps AS builder
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 6: Add to docker-compose.yml

```yaml
my-service:
  build: ./services/my-service
  container_name: n11-myservice
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/myservicedb
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: postgres
    JWT_SECRET: ${JWT_SECRET:-...}
    MANAGEMENT_TRACING_SAMPLING_PROBABILITY: "1.0"
    MANAGEMENT_OTLP_TRACING_ENDPOINT: http://jaeger:4318/v1/traces
  depends_on:
    postgres:
      condition: service_healthy
  expose:
    - "8089"
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8089/actuator/health || exit 1"]
    interval: 15s
    timeout: 5s
    retries: 6
    start_period: 40s
  restart: on-failure
```

### Step 7: Add the database

In the postgres entrypoint in `docker-compose.yml`, add:
```
psql -U postgres -c \"CREATE DATABASE myservicedb\" || true
```

### Step 8: Add a gateway route (if HTTP-facing)

In `services/api-gateway/src/main/resources/application.yml`:

```yaml
- id: my-service
  uri: ${MY_SERVICE_URI:http://localhost:8089}
  predicates:
    - Path=/api/myresource/**
```

Add `MY_SERVICE_URI: http://my-service:8089` to the gateway's environment in docker-compose.

### Step 9: Add to Prometheus scrape targets

In `infra/prometheus/prometheus.yml`:

```yaml
targets:
  - my-service:8089
```

---

## How to Add a New Saga Event

### Step 1: Define the event record

In the publishing service:

```java
public record MyNewEvent(
    String eventId,
    Instant occurredAt,
    Long someId,
    String someData
) implements Serializable {
    public static MyNewEvent of(Long someId, String someData) {
        return new MyNewEvent(UUID.randomUUID().toString(), Instant.now(), someId, someData);
    }
}
```

### Step 2: Add routing key and queue constants

In the publisher's `SagaTopology.java`:

```java
public static final String MY_NEW_ROUTING_KEY = "my.new.event";
```

In the consumer's `SagaTopology.java`:

```java
public static final String MY_NEW_ROUTING_KEY = "my.new.event";
public static final String MY_NEW_QUEUE = "consumer-service.my-new-event.queue";
```

### Step 3: Declare the queue binding

In the consumer's `SagaRabbitConfig.java`:

```java
@Bean
public Queue myNewQueue() {
    return QueueBuilder.durable(SagaTopology.MY_NEW_QUEUE).build();
}

@Bean
public Binding myNewBinding(TopicExchange sagaExchange) {
    return BindingBuilder.bind(myNewQueue())
            .to(sagaExchange)
            .with(SagaTopology.MY_NEW_ROUTING_KEY);
}
```

### Step 4: Publish the event

```java
rabbitTemplate.convertAndSend(
    SagaTopology.EXCHANGE,
    SagaTopology.MY_NEW_ROUTING_KEY,
    MyNewEvent.of(id, data)
);
```

### Step 5: Consume the event

```java
@RabbitListener(queues = SagaTopology.MY_NEW_QUEUE)
public void onMyNewEvent(Map<String, Object> event) {
    Long someId = ((Number) event.get("someId")).longValue();
    // ... business logic
}
```

Note: Consumers receive events as `Map<String, Object>` rather than the typed record. This decouples services -- they don't share Java classes.

---

## How to Add a New API Endpoint

### Step 1: Define the DTO records

```java
// Request
public record CreateThingRequest(
    @NotBlank String name,
    @Min(1) int quantity
) {}

// Response
public record ThingResponse(Long id, String name, int quantity, Instant createdAt) {
    public static ThingResponse from(Thing entity) {
        return new ThingResponse(entity.getId(), entity.getName(),
            entity.getQuantity(), entity.getCreatedAt());
    }
}
```

### Step 2: Create the controller method

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ThingResponse create(
    @AuthenticationPrincipal String userEmail,
    @Valid @RequestBody CreateThingRequest req
) {
    return thingService.create(userEmail, req);
}
```

### Step 3: Update SecurityConfig (if needed)

If the endpoint should be public, add it to the `permitAll()` list.

---

## How to Add a New Frontend Feature

### Step 1: Create the feature directory

```
frontend/src/features/my-feature/
  api.ts          # API calls using apiFetch
  MyFeaturePage.tsx
  components/
    MyComponent.tsx
```

### Step 2: Define API calls

```typescript
import { apiFetch } from '@/shared/api/client';

export async function getThings(): Promise<Thing[]> {
  return apiFetch('/api/things');
}
```

### Step 3: Add routing

In `frontend/src/app/router.tsx`, add the route:

```typescript
{ path: '/my-feature', element: <MyFeaturePage /> }
```

---

## Redis & Caching

Redis is used **only by product-service** as a read-through cache for the product catalog. All other services rely on PostgreSQL and RabbitMQ exclusively.

### What Is Cached

| Cache Name | Key | TTL | Method | Endpoint |
|------------|-----|-----|--------|----------|
| `products:byId` | Product ID | 30 min | `ProductService.getById()` | `GET /api/products/{id}` |
| `products:bySlug` | Slug string | 30 min | `ProductService.getBySlug()` | `GET /api/products/slug/{slug}` |
| `products:categories` | (none — single entry) | 24 hours | `ProductService.categories()` | `GET /api/products/categories` |

The paginated `list()` method is **not cached** — `Page<T>` objects cannot be reliably serialized with Redis JSON, and the DB query is fast with indexes.

### How It Works

```
First request:   Controller → @Cacheable → Cache MISS → PostgreSQL → write to Redis → Response
Next requests:   Controller → @Cacheable → Cache HIT  → read from Redis → Response (no DB call)
After TTL:       Entry expires → next request is a cache miss again
```

### Configuration

**Redis container** (`docker-compose.yml`):
- `redis:7-alpine` with 128 MB memory limit
- Eviction policy: `allkeys-lru` (least-recently-used keys evicted when full)
- Persistent volume: `redis_data`

**Spring Boot** (`product-service/application.yml`):
```yaml
spring.data.redis:
  host: ${SPRING_DATA_REDIS_HOST:localhost}
  port: ${SPRING_DATA_REDIS_PORT:6379}
spring.cache:
  type: redis
  redis:
    time-to-live: 600000        # 10 min default
    cache-null-values: false
```

**Cache manager** (`RedisConfig.java`):
- Serializer: `GenericJackson2JsonRedisSerializer` (stores Java objects as JSON)
- Per-cache TTL overrides for `products:byId` (30 min), `products:bySlug` (30 min), `products:categories` (24 h)

### Error Handling

`CacheConfig.java` implements `CachingConfigurer` with a custom `CacheErrorHandler`. If Redis is unreachable or serialization fails:
- A `WARN` log is emitted with cache name, key, and error message
- The request falls through to the database — **no 500 error**
- The service remains fully functional without Redis

This is intentionally separate from `RedisConfig` to avoid bean resolution conflicts between `CachingConfigurer.cacheManager()` and the `@Bean` method.

### Cache Invalidation

There is **no explicit invalidation** (`@CacheEvict`). Entries expire via TTL only. This is acceptable because:
- The product catalog is read-only (seeded via Flyway, no CRUD API)
- A 30-minute staleness window is fine for a catalog service
- If a CRUD API is added later, `@CacheEvict` should be added to write methods

### Adding a New Cache

1. Add `@Cacheable` to the service method:
   ```java
   @Cacheable(value = "products:myCache", key = "#someParam")
   public MyResponse myMethod(Long someParam) { ... }
   ```

2. Optionally add a custom TTL in `RedisConfig.java`:
   ```java
   .withCacheConfiguration("products:myCache",
       defaults.entryTtl(Duration.ofMinutes(15)))
   ```

3. **Avoid caching `Page<T>`** — Spring Data's `PageImpl` does not deserialize cleanly with JSON. Cache individual items or simple DTOs instead.

### Local Development

Redis is only required when running product-service. To run other services locally, Redis is not needed. If product-service starts without Redis, the `CacheErrorHandler` logs warnings and every request hits the database directly.

### Useful Commands

```bash
# Connect to Redis CLI (default port)
docker exec -it n11-redis redis-cli

# See all cached keys
KEYS *

# Inspect a specific cached product
GET "products:byId::42"

# Clear all caches
FLUSHALL

# Monitor real-time commands
MONITOR
```

---

## Code Conventions

### Package Structure

Every service follows the same package layout:

```
com.example.{service}/
  config/       # SecurityConfig, GlobalExceptionHandler, CORS, etc.
  controller/   # @RestController classes
  dto/          # Request/Response records
  entity/       # JPA entities
  filter/       # Servlet filters (JWT, logging, rate limiting)
  repository/   # Spring Data JPA interfaces
  saga/         # RabbitMQ topology, events, listeners, publishers
  service/      # Business logic + JwtService
  validation/   # Custom validators (auth-service only)
  exception/    # Custom exception classes
```

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Service class | `{Entity}Service` | `OrderService` |
| Controller | `{Entity}Controller` | `OrderController` |
| Repository | `{Entity}Repository` | `OrderRepository` |
| Request DTO | `{Action}{Entity}Request` | `CheckoutRequest`, `AddItemRequest` |
| Response DTO | `{Entity}Response` | `OrderResponse`, `ProductResponse` |
| Event | `{Entity}{Action}Event` | `OrderCreatedEvent`, `PaymentSucceededEvent` |
| Listener | `{Event}Listener` or `{Source}Listener` | `PaymentListener`, `InventoryListener` |
| Migration | `V{n}__{description}.sql` | `V1__create_orders.sql` |

### Common Patterns

| Pattern | Usage |
|---------|-------|
| `record` | All DTOs (request, response, event) are Java records |
| `@Builder` | All JPA entities use Lombok Builder |
| `@RequiredArgsConstructor` | Constructor injection everywhere (no `@Autowired`) |
| `@AuthenticationPrincipal String` | Extract authenticated user email in controllers |
| `ResponseEntity` | Used when status code varies; direct return otherwise |
| `ProblemDetail` | All error responses follow RFC 7807 |

### Commit Convention

Follow Conventional Commits with one atomic commit per feature:

- `feat:` -- new feature or service
- `fix:` -- bug fix
- `refactor:` -- internal restructuring without behavior change
- `chore:` -- build, compose, infrastructure
- `docs:` -- documentation
- `test:` -- adding or modifying tests

---

## Related Documentation

- [Architecture](architecture.md) -- System-level understanding
- [Testing](testing.md) -- How to write and run tests
- [Data Model](data-model.md) -- Entity schemas and migrations
- [Deployment](deployment.md) -- Docker and infrastructure details
