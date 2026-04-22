# Testing Strategy

This document covers the testing approach used across all services: unit tests with Mockito, integration tests with Spring Boot Test + H2, how RabbitMQ is mocked, test locations and naming, and how to run tests locally and in CI.

---

## Overview

The test suite is designed for speed and reliability:

- **Unit tests** use Mockito to isolate business logic -- no Spring context, no database, no message broker. They run in milliseconds.
- **Integration tests** (auth-service only) use `@SpringBootTest` with H2 in-memory database and `MockMvc` to test the full HTTP pipeline including security, validation, and error handling.
- **No external dependencies** are required to run any test. All tests can execute with just `java` and `mvn`.

---

## Unit Test Pattern

All unit tests follow the same structure:

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaEventPublisher sagaEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkout_shouldCreateOrderAndPublishEvent() {
        // given
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        // when
        OrderResponse result = orderService.checkout("user@test.com", request);

        // then
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(sagaEventPublisher).publishOrderCreated(any());
    }
}
```

### Key Elements

| Annotation | Purpose |
|-----------|---------|
| `@ExtendWith(MockitoExtension.class)` | Initializes `@Mock` and `@InjectMocks` without Spring |
| `@Mock` | Creates a mock of a dependency |
| `@InjectMocks` | Creates the class under test with mocks injected via constructor |
| `@Captor` | Captures arguments passed to mock methods for assertion |

### What to assert

1. **Return values** -- The response DTO has the correct fields
2. **Side effects** -- `verify()` that the correct methods were called (e.g., repository save, event publish)
3. **Exceptions** -- `assertThrows()` for error paths
4. **No unwanted calls** -- `verifyNoMoreInteractions()` where appropriate

---

## Integration Test Pattern (auth-service)

Auth-service has integration tests that boot the full Spring context with H2:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_withValidData_returns201WithAccessToken() throws Exception {
        RegisterRequest req = new RegisterRequest("new@test.com", "Test1234!", "New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void register_withWeakPassword_returns400WithFields() throws Exception {
        RegisterRequest req = new RegisterRequest("weak@test.com", "short", "Weak User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }
}
```

### Test Profile Configuration

The `application-test.yml` uses H2 in-memory database:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

### What Integration Tests Verify

| Aspect | Verification Method |
|--------|-------------------|
| HTTP status codes | `status().isCreated()`, `status().isBadRequest()` |
| JSON response body | `jsonPath("$.accessToken").isNotEmpty()` |
| RFC 7807 error format | `jsonPath("$.status")`, `jsonPath("$.detail")`, `jsonPath("$.fields")` |
| Cookie behavior | `cookie().exists("refresh_token")`, `cookie().httpOnly()` |
| Security rules | Unauthenticated requests return 401 |
| Validation | Invalid payloads return 400 with field-level errors |

---

## How RabbitMQ Is Mocked in Tests

Services that use RabbitMQ declare a `@TestConfiguration` with `@Primary` beans that replace the real RabbitMQ components:

```java
@TestConfiguration
public class TestRabbitConfig {

    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public RabbitAdmin mockRabbitAdmin() {
        return Mockito.mock(RabbitAdmin.class);
    }
}
```

For unit tests (which don't start Spring), the `RabbitTemplate` is simply a `@Mock`:

```java
@Mock
private RabbitTemplate rabbitTemplate;
```

Saga event publishers are tested by verifying that `rabbitTemplate.convertAndSend()` was called with the correct exchange, routing key, and event payload:

```java
verify(rabbitTemplate).convertAndSend(
    eq("saga.exchange"),
    eq("order.created"),
    argThat(event -> {
        OrderCreatedEvent e = (OrderCreatedEvent) event;
        return e.orderId().equals(42L) && e.userEmail().equals("user@test.com");
    })
);
```

---

## Test Coverage by Service

### auth-service

| Test Class | What It Tests |
|-----------|---------------|
| `JwtServiceTest` | Token generation, claim extraction, expiration detection, invalid token handling |
| `AuthServiceTest` | Register (happy + duplicate email), login (happy + bad credentials), refresh (happy + expired + revoked), logout |
| `StrongPasswordValidatorTest` | All password rules: too short, no uppercase, no lowercase, no digit, no special char, valid password |
| `AuthControllerIntegrationTest` | Full HTTP pipeline: register/login responses, cookie handling, RFC 7807 error format, validation errors |
| `UserControllerIntegrationTest` | `/api/users/me` with valid/invalid token, `/api/users/admin` role-based access |

### basket-service

| Test Class | What It Tests |
|-----------|---------------|
| `BasketServiceTest` | Add item (new + quantity merge), update quantity, remove item, clear basket, get basket for unknown user |
| `UserRegisteredListenerTest` | Empty basket creation on user registration, idempotent handling (basket already exists) |
| `OrderConfirmedListenerTest` | Basket clearing when order is confirmed |

### product-service

| Test Class | What It Tests |
|-----------|---------------|
| `ProductServiceTest` | List with no filter, filter by category, search by `q`, `q` priority over category, whitespace trim, discountedPrice computation, 404 for unknown ID, category list (8 categories) |

### order-service

| Test Class | What It Tests |
|-----------|---------------|
| `OrderServiceTest` | Checkout creates PENDING order, total calculated server-side, OrderCreatedEvent published with correct items |
| `PaymentListenerTest` | `payment.succeeded` -> PAID + OrderConfirmedEvent, `payment.failed` -> CANCELLED + OrderCancelledEvent, replay safety (unknown orderId -> no-op) |

### payment-service

| Test Class | What It Tests |
|-----------|---------------|
| `PaymentProcessorTest` | Decision matrix: normal user -> SUCCEEDED, email contains "fail" -> FAILED, amount > 100,000 -> FAILED, amount = 100,000 exactly -> SUCCEEDED, transaction persisted, correct saga event published |

### notification-service

| Test Class | What It Tests |
|-----------|---------------|
| `NotificationEventListenerTest` | `user.registered` -> WELCOME notification, `order.confirmed` -> ORDER_CONFIRMED with Turkish currency format, `order.cancelled` -> ORDER_CANCELLED, all notifications default to `read=false` |

### review-service

| Test Class | What It Tests |
|-----------|---------------|
| `ReviewControllerTest` | Create review, duplicate review (409 Conflict), display name derived from email, delete own review, delete someone else's review (404) |

### inventory-service

| Test Class | What It Tests |
|-----------|---------------|
| `InventoryServiceTest` | All-or-nothing reservation, OutOfStockException on insufficient stock, unknown product fails, release restores available stock, release idempotency (no reservations -> no-op) |

---

## Test File Locations

Tests mirror the main source tree:

```
services/{service}/src/
  main/java/com/example/{pkg}/
    service/OrderService.java
    saga/PaymentListener.java
  test/java/com/example/{pkg}/
    service/OrderServiceTest.java
    saga/PaymentListenerTest.java
  test/resources/
    application-test.yml          # H2 config (integration tests)
```

### Naming Convention

| Main Class | Test Class |
|-----------|-----------|
| `OrderService` | `OrderServiceTest` |
| `PaymentListener` | `PaymentListenerTest` |
| `PaymentProcessor` | `PaymentProcessorTest` |
| `StrongPasswordValidator` | `StrongPasswordValidatorTest` |
| `AuthController` | `AuthControllerIntegrationTest` |
| `UserController` | `UserControllerIntegrationTest` |

---

## Running Tests

### Single Service

```bash
cd services/order-service
./mvnw test
```

### All Services

There is no root reactor POM, so iterate:

```bash
for d in services/*/; do
  echo "=== Testing $(basename $d) ==="
  (cd "$d" && ./mvnw -q test) || echo "FAILED: $d"
done
```

### With Specific Test

```bash
cd services/auth-service
./mvnw test -Dtest=AuthServiceTest
./mvnw test -Dtest=AuthControllerIntegrationTest#register_withValidData_returns201WithAccessToken
```

### Skipping Tests in Docker Build

Not recommended, but possible:

```bash
docker compose build --build-arg MAVEN_OPTS="-DskipTests"
```

The multi-stage Dockerfile runs tests in a separate stage (`FROM deps AS test`). If tests fail, the Docker build fails -- this is intentional and acts as a quality gate.

---

## CI Pipeline

The CI runs on every push and pull request via GitHub Actions.

### Backend Job

- **Strategy**: Matrix build across all 10 services
- **Command**: `mvn -B -ntp verify` (runs tests + packages the JAR)
- **Caching**: Maven dependencies are cached between runs
- **Profile**: `test` (H2 in-memory, no external deps)

### Frontend Job

- **Command**: `npm run build` (includes `tsc -b` for type checking)
- **TypeScript errors** cause the pipeline to fail

### Concurrency

A concurrency group ensures that pushing to the same branch cancels any in-flight CI run, saving resources.

---

## Writing New Tests

### Unit Test Checklist

1. Use `@ExtendWith(MockitoExtension.class)` -- never `@SpringBootTest` for unit tests
2. Mock all dependencies with `@Mock`
3. Inject into the class under test with `@InjectMocks`
4. Test the happy path first, then error paths
5. Verify side effects (repo saves, event publishes) with `verify()`
6. Assert exceptions with `assertThrows()` or AssertJ's `assertThatThrownBy()`
7. Use `@Captor` to inspect complex arguments passed to mocks

### Integration Test Checklist

1. Use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
2. Ensure `application-test.yml` exists with H2 config
3. If the service uses RabbitMQ, add a `@TestConfiguration` with mock beans
4. Test HTTP-level behavior: status codes, response bodies, headers, cookies
5. Test validation: submit invalid payloads and verify RFC 7807 responses
6. Test security: submit requests without tokens and verify 401

### What NOT to Test

- **Spring Data JPA methods** like `findById`, `save` -- these are tested by Spring itself
- **Lombok-generated code** -- getters, setters, builders
- **Configuration classes** -- unless they contain logic
- **Third-party library behavior** -- Flyway migrations, Jackson serialization

---

## Test Dependencies

Common test dependencies across services:

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, MockMvc, H2 |
| `h2` (scope: test) | In-memory database for integration tests |
| `mockito-core` | Mocking framework |
| `assertj-core` | Fluent assertions |

These are all included transitively via `spring-boot-starter-test`.

---

## Related Documentation

- [Development Guide](development-guide.md) -- Project structure and conventions
- [Saga Patterns](saga-patterns.md) -- Understanding what saga tests verify
- [Deployment](deployment.md) -- How tests run during Docker build
