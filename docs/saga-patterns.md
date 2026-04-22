# Saga Choreography Patterns

This document explains the distributed transaction patterns used in the project: how sagas work, why choreography was chosen over orchestration, the detailed event flows for both the UserRegistrationSaga and the CheckoutSaga, the RabbitMQ topology, event contracts, and idempotency rules.

---

## What Is a Saga?

A saga is a sequence of local transactions across multiple services. Each service executes its own transaction and publishes an event. If a step fails, previously completed steps are undone through **compensating transactions**. Unlike a distributed 2PC (two-phase commit), sagas never hold locks across services.

### Why Choreography Over Orchestration?

| Aspect | Choreography (this project) | Orchestration |
|--------|---------------------------|---------------|
| Coordination | Each service reacts to events autonomously | Central orchestrator directs each step |
| Coupling | Low -- services only know about events, not each other | Higher -- orchestrator knows all services |
| Single point of failure | None | Orchestrator |
| Complexity location | Distributed across listeners | Centralized in orchestrator |
| Best for | Simple linear flows (2-5 steps) | Complex flows with branching logic |

This project uses choreography because the flows are linear and each service has clear responsibility boundaries. The trade-off (harder to trace the flow) is mitigated by the observability stack -- Jaeger shows the entire saga as a single distributed trace.

---

## UserRegistrationSaga

Ensures that when a user registers, an empty shopping basket is created automatically. If basket creation fails, the user is deleted (compensation).

```mermaid
sequenceDiagram
    participant Client
    participant Auth as auth-service
    participant RMQ as RabbitMQ
    participant Basket as basket-service
    participant Notif as notification-service

    Client->>Auth: POST /api/auth/register
    Auth->>Auth: Validate + hash password
    Auth->>Auth: INSERT into users table
    Auth->>RMQ: Publish "user.registered"
    Auth-->>Client: 201 {accessToken} + cookie

    par Basket creates empty cart
        RMQ->>Basket: Consume "user.registered"
        Basket->>Basket: INSERT into baskets (empty)
    and Notification sends welcome
        RMQ->>Notif: Consume "user.registered"
        Notif->>Notif: INSERT welcome notification
    end

    alt Basket creation fails
        Basket->>RMQ: Publish "basket.creation.failed"
        RMQ->>Auth: Consume "basket.creation.failed"
        Auth->>Auth: DELETE user (compensation)
    end
```

### Event Details

**user.registered** (auth -> basket, notification):

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | `String` | UUID, unique per event instance |
| `occurredAt` | `Instant` | Timestamp of event creation |
| `userId` | `Long` | Database ID of the new user |
| `email` | `String` | User's email address |
| `fullName` | `String` | User's full name |

**basket.creation.failed** (basket -> auth):

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | `String` | UUID |
| `occurredAt` | `Instant` | Timestamp |
| `userId` | `Long` | ID of the user whose basket failed |
| `email` | `String` | User's email |
| `reason` | `String` | Failure description |

---

## CheckoutSaga

The checkout saga is the core distributed transaction of the platform. It coordinates order creation, inventory reservation, payment processing, basket clearing, and notification delivery across five services.

### Happy Path

```mermaid
sequenceDiagram
    participant Client
    participant Order as order-service
    participant RMQ as RabbitMQ
    participant Inv as inventory-service
    participant Pay as payment-service
    participant Basket as basket-service
    participant Notif as notification-service

    Client->>Order: POST /api/orders/checkout
    Order->>Order: INSERT order (status=PENDING)
    Order->>RMQ: Publish "order.created"
    Order-->>Client: 202 Accepted {order with PENDING status}

    RMQ->>Inv: Consume "order.created"
    Inv->>Inv: Reserve stock atomically
    Note over Inv: available--, reserved++ per line<br/>INSERT reservations per line
    Inv->>RMQ: Publish "inventory.reserved"

    RMQ->>Pay: Consume "inventory.reserved"
    Pay->>Pay: Process payment (mock decision)
    Pay->>Pay: INSERT payment_transaction (SUCCEEDED)
    Pay->>RMQ: Publish "payment.succeeded"

    RMQ->>Order: Consume "payment.succeeded"
    Order->>Order: UPDATE order status = PAID
    Order->>RMQ: Publish "order.confirmed"

    par Clear basket
        RMQ->>Basket: Consume "order.confirmed"
        Basket->>Basket: DELETE all basket items
    and Send notification
        RMQ->>Notif: Consume "order.confirmed"
        Notif->>Notif: INSERT "Order confirmed" notification
    end
```

### Failure Path: Inventory Out of Stock

```mermaid
sequenceDiagram
    participant Client
    participant Order as order-service
    participant RMQ as RabbitMQ
    participant Inv as inventory-service
    participant Notif as notification-service

    Client->>Order: POST /api/orders/checkout
    Order->>Order: INSERT order (status=PENDING)
    Order->>RMQ: Publish "order.created"
    Order-->>Client: 202 Accepted

    RMQ->>Inv: Consume "order.created"
    Inv->>Inv: Check available stock
    Note over Inv: Insufficient stock for product #X
    Inv->>RMQ: Publish "inventory.out-of-stock"

    RMQ->>Order: Consume "inventory.out-of-stock"
    Order->>Order: UPDATE order status = CANCELLED
    Order->>Order: SET failureReason = "Out of stock"
    Order->>RMQ: Publish "order.cancelled"

    par Release stock (no-op, nothing was reserved)
        RMQ->>Inv: Consume "order.cancelled"
        Inv->>Inv: release(orderId) -> no-op
    and Notify user
        RMQ->>Notif: Consume "order.cancelled"
        Notif->>Notif: INSERT "Order cancelled" notification
    end
```

### Failure Path: Payment Declined

```mermaid
sequenceDiagram
    participant Client
    participant Order as order-service
    participant RMQ as RabbitMQ
    participant Inv as inventory-service
    participant Pay as payment-service
    participant Notif as notification-service

    Client->>Order: POST /api/orders/checkout
    Order->>Order: INSERT order (status=PENDING)
    Order->>RMQ: Publish "order.created"
    Order-->>Client: 202 Accepted

    RMQ->>Inv: Consume "order.created"
    Inv->>Inv: Reserve stock (SUCCESS)
    Inv->>RMQ: Publish "inventory.reserved"

    RMQ->>Pay: Consume "inventory.reserved"
    Pay->>Pay: Process payment
    Note over Pay: Email contains "fail" OR amount > 100,000 TRY
    Pay->>Pay: INSERT payment_transaction (FAILED)
    Pay->>RMQ: Publish "payment.failed"

    RMQ->>Order: Consume "payment.failed"
    Order->>Order: UPDATE order status = CANCELLED
    Order->>Order: SET failureReason from event
    Order->>RMQ: Publish "order.cancelled"

    par Release reserved stock
        RMQ->>Inv: Consume "order.cancelled"
        Inv->>Inv: DELETE reservations for orderId
        Inv->>Inv: reserved--, available++ per line
    and Notify user
        RMQ->>Notif: Consume "order.cancelled"
        Notif->>Notif: INSERT "Order cancelled" notification
    end
```

---

## Complete Checkout Saga State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: POST /checkout

    PENDING --> PAID: payment.succeeded
    PENDING --> CANCELLED: inventory.out-of-stock
    PENDING --> CANCELLED: payment.failed

    PAID --> [*]: order.confirmed sent
    CANCELLED --> [*]: order.cancelled sent

    note right of PAID
        Side effects:
        - Basket cleared
        - Confirmation notification
    end note

    note right of CANCELLED
        Side effects:
        - Inventory released
        - Cancellation notification
    end note
```

---

## Mock Payment Decision Logic

The `payment-service` uses a deterministic mock to make payment accept/reject testable:

| Condition | Result | How to trigger |
|-----------|--------|---------------|
| User email contains `"fail"` | FAILED | Register as `failuser@n11demo.com` |
| Total amount > 100,000 TRY | FAILED | Add expensive items to cart |
| Amount = 100,000 TRY exactly | SUCCEEDED | Boundary -- passes |
| All other cases | SUCCEEDED | Normal checkout |

---

## RabbitMQ Topology

All saga events flow through a single **topic exchange**.

```mermaid
flowchart LR
    subgraph Exchange
        EX["saga.exchange\n(topic)"]
    end

    subgraph Queues
        Q1["basket.user-registered.queue"]
        Q2["auth.basket-failed.queue"]
        Q3["inventory.order-created.queue"]
        Q4["payment.inventory-reserved.queue"]
        Q5["order.payment-succeeded.queue"]
        Q6["order.payment-failed.queue"]
        Q7["order.inventory-out-of-stock.queue"]
        Q8["basket.order-confirmed.queue"]
        Q9["notification.user-registered.queue"]
        Q10["notification.order-confirmed.queue"]
        Q11["notification.order-cancelled.queue"]
        Q12["inventory.order-cancelled.queue"]
    end

    EX -->|user.registered| Q1
    EX -->|user.registered| Q9
    EX -->|basket.creation.failed| Q2
    EX -->|order.created| Q3
    EX -->|inventory.reserved| Q4
    EX -->|inventory.out-of-stock| Q7
    EX -->|payment.succeeded| Q5
    EX -->|payment.failed| Q6
    EX -->|order.confirmed| Q8
    EX -->|order.confirmed| Q10
    EX -->|order.cancelled| Q11
    EX -->|order.cancelled| Q12
```

### Routing Key Reference

| Routing Key | Publisher | Consumer(s) |
|-------------|----------|-------------|
| `user.registered` | auth-service | basket-service, notification-service |
| `basket.creation.failed` | basket-service | auth-service |
| `order.created` | order-service | inventory-service |
| `inventory.reserved` | inventory-service | payment-service |
| `inventory.out-of-stock` | inventory-service | order-service |
| `payment.succeeded` | payment-service | order-service |
| `payment.failed` | payment-service | order-service |
| `order.confirmed` | order-service | basket-service, notification-service |
| `order.cancelled` | order-service | inventory-service, notification-service |

### Queue Naming Convention

Queues follow the pattern: `{owning-service}.{event-name}.queue`

This makes it immediately clear which service owns the queue and what event it processes when inspecting RabbitMQ management UI.

---

## Event Contracts

Each event is a Java `record` implementing `Serializable`. Events carry all the data downstream consumers need -- no callbacks to the publishing service required.

### Common Fields (all events)

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | `String` | UUID -- unique identifier for idempotency |
| `occurredAt` | `Instant` | When the event was created |

### UserRegisteredEvent

| Field | Type |
|-------|------|
| `userId` | `Long` |
| `email` | `String` |
| `fullName` | `String` |

### BasketCreationFailedEvent

| Field | Type |
|-------|------|
| `userId` | `Long` |
| `email` | `String` |
| `reason` | `String` |

### OrderCreatedEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `totalAmount` | `BigDecimal` |
| `items` | `List<Line>` |
| `items[].productId` | `Long` |
| `items[].quantity` | `int` |

### InventoryReservedEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `totalAmount` | `BigDecimal` |

### InventoryOutOfStockEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `missingProductId` | `Long` |
| `requestedQuantity` | `int` |
| `reason` | `String` |

### PaymentSucceededEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `amount` | `BigDecimal` |
| `transactionId` | `String` |

### PaymentFailedEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `reason` | `String` |

### OrderConfirmedEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `totalAmount` | `BigDecimal` |

### OrderCancelledEvent

| Field | Type |
|-------|------|
| `orderId` | `Long` |
| `userEmail` | `String` |
| `reason` | `String` |

---

## Idempotency Rules

In a distributed system, messages can be delivered more than once (at-least-once delivery). Each consumer is designed to handle duplicates safely:

| Consumer | Idempotency Strategy |
|----------|---------------------|
| basket-service `onUserRegistered` | `createEmptyBasketFor` checks if basket already exists for email -- no-op if so |
| basket-service `onOrderConfirmed` | Clearing an already-empty basket is a no-op |
| inventory-service `onOrderCreated` | Reservation is atomic per orderId; if reservations already exist, the service can detect this |
| inventory-service `onOrderCancelled` | `release(orderId)` deletes reservations and restores stock; if no reservations exist, it is a no-op |
| order-service `onPaymentSucceeded` | Finds order by ID; if already PAID, no state change occurs |
| order-service `onPaymentFailed` | Finds order by ID; if already CANCELLED, no state change occurs |
| notification-service | Notifications are additive; duplicate delivery creates a duplicate notification (acceptable for this use case) |
| auth-service `onBasketCreationFailed` | Deletes user by ID; if user doesn't exist, no-op |

### Event ID

Every event carries a `UUID eventId`. While the current implementation relies on business-level idempotency (checking database state), the eventId field provides a foundation for implementing deduplication tables (outbox pattern) if needed in the future.

---

## Observing Sagas in Action

### From the browser

1. **Happy path**: Register, add items to cart, checkout at `/checkout`. Poll the order detail page -- status transitions from `PENDING` to `PAID` within seconds. A confirmation notification appears.

2. **Payment failure**: Log in as `failuser@n11demo.com` and checkout. The order transitions to `CANCELLED` with the failure reason. A cancellation notification appears.

3. **Stock failure**: Add more items than available stock and checkout. Same `CANCELLED` result with stock-related reason.

### From Jaeger

Open Jaeger UI at `http://localhost:26686`, select `order-service`, and find the checkout trace. The waterfall view shows every hop:

```
order-service (HTTP POST /checkout)
  -> RabbitMQ publish order.created
    -> inventory-service (AMQP consume)
      -> PostgreSQL reserve
      -> RabbitMQ publish inventory.reserved
        -> payment-service (AMQP consume)
          -> RabbitMQ publish payment.succeeded
            -> order-service (AMQP consume)
              -> RabbitMQ publish order.confirmed
                -> basket-service (AMQP consume)
                -> notification-service (AMQP consume)
```

### From Grafana Logs

Query Loki with the correlationId from the checkout response header:

```logql
{correlationId="<value-from-X-Correlation-Id-header>"}
```

This shows every log line across all services involved in that specific saga execution.

---

## Related Documentation

- [Architecture](architecture.md) -- Service communication patterns
- [Observability](observability.md) -- How to trace saga flows end-to-end
- [API Reference](api-reference.md) -- Checkout endpoint details
- [Data Model](data-model.md) -- Entity schemas for orders, payments, inventory
