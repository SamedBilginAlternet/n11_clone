# API Reference

All endpoints are accessed through the API Gateway at `http://localhost:18000`. Authentication is via JWT Bearer token unless noted as public.

---

## Error Response Format

All services use RFC 7807 `application/problem+json` for error responses:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation error",
  "instance": "/api/auth/register",
  "timestamp": "2026-04-22T10:00:00.000Z",
  "fields": {
    "email": "must not be blank",
    "password": "Password must be at least 8 characters with uppercase, lowercase, digit, and special character."
  }
}
```

The `fields` property is present only on validation errors (400). The `retryAfterSeconds` property is present only on rate-limit responses (429).

### Standard Error Codes

| Status | Meaning | When |
|--------|---------|------|
| 400 | Bad Request | Validation failure, missing/malformed body |
| 401 | Unauthorized | Missing/expired/invalid JWT |
| 403 | Forbidden | Valid JWT but insufficient role |
| 404 | Not Found | Resource does not exist or is not owned by the user |
| 405 | Method Not Allowed | Wrong HTTP method for the endpoint |
| 409 | Conflict | Duplicate resource (e.g., email already registered, duplicate review) |
| 429 | Too Many Requests | Rate limit exceeded on `/api/auth/**` |
| 500 | Internal Server Error | Unexpected server error |

---

## Auth Service

Base path: `/api/auth` and `/api/users`

### POST /api/auth/register

Create a new user account. **Public endpoint.**

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass1!",
  "fullName": "John Doe"
}
```

**Response (201):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Also sets `Set-Cookie: refresh_token=<UUID>; HttpOnly; Path=/api/auth`.

**Errors:** 400 (validation), 409 (email already exists)

---

### POST /api/auth/login

Authenticate an existing user. **Public endpoint.**

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass1!"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Also sets `Set-Cookie: refresh_token=<UUID>; HttpOnly; Path=/api/auth`.

**Errors:** 401 (bad credentials)

---

### POST /api/auth/refresh

Rotate the refresh token and get a new access token. **Public endpoint.** The refresh token is read from the `refresh_token` cookie.

**Request:** No body. Cookie `refresh_token` must be present.

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Also sets a new `Set-Cookie: refresh_token=<new-UUID>; HttpOnly; Path=/api/auth`.

**Errors:** 400 (no cookie), 401 (expired or revoked)

---

### POST /api/auth/logout

Revoke the refresh token and clear the cookie. **Public endpoint.**

**Request:** No body. Cookie `refresh_token` should be present.

**Response:** 204 No Content. Sets `Set-Cookie: refresh_token=; Max-Age=0`.

---

### GET /api/users/me

Get the current authenticated user's profile. **Requires JWT.**

**Response (200):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "John Doe",
  "roles": ["USER"]
}
```

---

### GET /api/users/admin

Admin-only endpoint. **Requires JWT with ADMIN role.**

**Response (200):**
```json
{
  "message": "Admin paneline hos geldiniz!"
}
```

**Errors:** 403 (not ADMIN)

---

## Basket Service

Base path: `/api/basket`. **All endpoints require JWT.**

### GET /api/basket

Get the current user's basket.

**Response (200):**
```json
{
  "id": 1,
  "userEmail": "user@example.com",
  "items": [
    {
      "id": 10,
      "productId": 5,
      "productName": "iPhone 15",
      "productPrice": 64999.00,
      "imageUrl": "https://...",
      "quantity": 2
    }
  ]
}
```

---

### POST /api/basket/items

Add an item to the basket. If the product already exists, quantities merge.

**Request:**
```json
{
  "productId": 5,
  "productName": "iPhone 15",
  "productPrice": 64999.00,
  "imageUrl": "https://...",
  "quantity": 1
}
```

**Response (200):** Updated basket (same shape as GET).

---

### PUT /api/basket/items/{itemId}

Update the quantity of a basket item.

**Request:**
```json
{
  "quantity": 3
}
```

**Response (200):** Updated basket.

---

### DELETE /api/basket/items/{itemId}

Remove a single item from the basket.

**Response (200):** Updated basket.

---

### DELETE /api/basket

Clear the entire basket.

**Response:** 204 No Content.

---

## Product Service

Base path: `/api/products`. **All endpoints are public** (no JWT required).

### GET /api/products

List products with optional filtering and pagination.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `category` | String | No | -- | Filter by category name |
| `q` | String | No | -- | Search by name (SQL LIKE) |
| `page` | int | No | 0 | Page number (0-indexed) |
| `size` | int | No | 24 | Page size |
| `sort` | String | No | -- | Sort field and direction (e.g., `price,asc`) |

**Response (200):** Spring `Page<ProductResponse>`:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Samsung Galaxy S24",
      "slug": "samsung-galaxy-s24",
      "description": "...",
      "price": 49999.00,
      "discountPercentage": 10,
      "discountedPrice": 44999.10,
      "stockQuantity": 50,
      "imageUrl": "https://...",
      "category": "Elektronik",
      "brand": "Samsung",
      "rating": 4.5,
      "reviewCount": 12
    }
  ],
  "totalElements": 48,
  "totalPages": 2,
  "number": 0,
  "size": 24
}
```

---

### GET /api/products/{id}

Get a single product by ID.

**Response (200):** Single `ProductResponse`. **Errors:** 404.

---

### GET /api/products/slug/{slug}

Get a single product by URL slug.

**Response (200):** Single `ProductResponse`. **Errors:** 404.

---

### GET /api/products/categories

List all product categories with counts.

**Response (200):**
```json
[
  { "name": "Elektronik", "count": 12 },
  { "name": "Ev & Yasam", "count": 8 }
]
```

---

## Order Service

Base path: `/api/orders`. **All endpoints require JWT.**

### POST /api/orders/checkout

Create an order and initiate the checkout saga. Returns immediately with PENDING status.

**Request:**
```json
{
  "shippingAddress": "123 Main St, Istanbul",
  "items": [
    {
      "productId": 5,
      "productName": "iPhone 15",
      "productPrice": 64999.00,
      "imageUrl": "https://...",
      "quantity": 1
    }
  ]
}
```

**Response (202 Accepted):**
```json
{
  "id": 42,
  "userEmail": "user@example.com",
  "totalAmount": 64999.00,
  "status": "PENDING",
  "shippingAddress": "123 Main St, Istanbul",
  "failureReason": null,
  "items": [...],
  "createdAt": "2026-04-22T10:00:00Z",
  "updatedAt": "2026-04-22T10:00:00Z"
}
```

The client should poll `GET /api/orders/{id}` to observe the status transition (PENDING -> PAID or PENDING -> CANCELLED).

---

### GET /api/orders

List all orders for the current user (newest first).

**Response (200):** Array of `OrderResponse`.

---

### GET /api/orders/{id}

Get a specific order. Only returns orders owned by the current user.

**Response (200):** Single `OrderResponse`. **Errors:** 404 (not found or not owned).

---

## Payment Service

Base path: `/api/payments`. **All endpoints require JWT.**

### GET /api/payments

List all payment transactions for the current user.

**Response (200):**
```json
[
  {
    "id": 1,
    "transactionId": "txn_a1b2c3d4",
    "orderId": 42,
    "userEmail": "user@example.com",
    "amount": 64999.00,
    "status": "SUCCEEDED",
    "failureReason": null,
    "createdAt": "2026-04-22T10:00:01Z"
  }
]
```

---

### GET /api/payments/order/{orderId}

Get the payment transaction for a specific order. Only returns transactions owned by the current user.

**Response (200):** Single `PaymentResponse`. **Errors:** 404.

---

## Notification Service

Base path: `/api/notifications`. **All endpoints require JWT.**

### GET /api/notifications

List all notifications for the current user (newest first).

**Response (200):**
```json
[
  {
    "id": 1,
    "type": "ORDER_CONFIRMED",
    "title": "Siparisin onaylandi",
    "message": "42 numarali siparisin onaylandi. Toplam: 64.999,00 TL",
    "read": false,
    "createdAt": "2026-04-22T10:00:02Z"
  }
]
```

**Notification types:** `WELCOME`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`, `SYSTEM`

---

### GET /api/notifications/unread-count

Get the count of unread notifications.

**Response (200):**
```json
{
  "count": 3
}
```

---

### PATCH /api/notifications/{id}/read

Mark a notification as read.

**Response (200):** Updated `NotificationResponse`. **Errors:** 404.

---

### DELETE /api/notifications/{id}

Delete a notification.

**Response:** 204 No Content. **Errors:** 404.

---

## Review Service

Base path: `/api/reviews`.

### GET /api/reviews/product/{productId} (Public)

List reviews for a product with pagination.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number |
| `size` | 20 | Page size |

**Response (200):** `Page<ReviewResponse>`:
```json
{
  "content": [
    {
      "id": 1,
      "productId": 5,
      "userName": "User",
      "rating": 4,
      "comment": "Great phone!",
      "createdAt": "2026-04-22T09:00:00Z"
    }
  ],
  "totalElements": 21,
  "totalPages": 2
}
```

---

### GET /api/reviews/product/{productId}/stats (Public)

Get aggregate review statistics for a product.

**Response (200):**
```json
{
  "productId": 5,
  "count": 21,
  "averageRating": 4.2
}
```

---

### GET /api/reviews/mine (Requires JWT)

List all reviews by the current user.

**Response (200):** Array of `ReviewResponse`.

---

### POST /api/reviews (Requires JWT)

Create a review. One review per user per product.

**Request:**
```json
{
  "productId": 5,
  "rating": 4,
  "comment": "Great phone, fast delivery!"
}
```

**Validation:** `rating` must be 1-5, `comment` max 2000 chars.

**Response (201):** `ReviewResponse`. **Errors:** 409 (already reviewed this product).

---

### DELETE /api/reviews/{id} (Requires JWT)

Delete your own review.

**Response:** 204 No Content. **Errors:** 404 (not found or not owned).

---

## Search Service

Base path: `/api/search`. **All endpoints are public.**

### GET /api/search

Full-text search with facets, backed by Elasticsearch.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | String | No | -- | Full-text query (Turkish-analyzed, fuzzy) |
| `category` | String | No | -- | Filter by category (exact match) |
| `brand` | String | No | -- | Filter by brand (exact match) |
| `minPrice` | Double | No | -- | Minimum discounted price |
| `maxPrice` | Double | No | -- | Maximum discounted price |
| `minRating` | Double | No | -- | Minimum average rating |
| `sort` | String | No | `relevance` | One of: `relevance`, `price_asc`, `price_desc`, `rating_desc` |
| `page` | int | No | 0 | Page number |
| `size` | int | No | 24 | Page size |

**Response (200):**
```json
{
  "hits": [
    {
      "id": 1,
      "name": "Samsung Galaxy S24",
      "slug": "samsung-galaxy-s24",
      "description": "...",
      "category": "Elektronik",
      "brand": "Samsung",
      "price": 49999.00,
      "discountedPrice": 44999.10,
      "discountPercentage": 10,
      "stockQuantity": 50,
      "imageUrl": "https://...",
      "rating": 4.5,
      "reviewCount": 12
    }
  ],
  "total": 5,
  "page": 0,
  "size": 24,
  "facets": {
    "brands": { "Samsung": 3, "Apple": 2 },
    "categories": { "Elektronik": 5 },
    "price": { "min": 2999.0, "max": 64999.0 }
  }
}
```

---

### POST /api/search/reindex

Trigger a full reindex from product-service. **Public endpoint.**

**Response (200):**
```json
{
  "indexed": 48
}
```

---

## Inventory Service

Base path: `/api/inventory`. **No gateway route** -- internal/debugging only. Accessible directly at `http://n11-inventory:8088` within Docker.

### GET /api/inventory

List all inventory items.

**Response (200):**
```json
[
  {
    "productId": 5,
    "availableStock": 48,
    "reservedStock": 2
  }
]
```

---

### GET /api/inventory/{productId}

Get inventory for a specific product.

**Response (200):** Single `InventoryResponse`. **Errors:** 404.

---

## Common curl Examples

### Register and Login

```bash
# Register
curl -c cookies.txt -X POST http://localhost:18000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!","fullName":"Test User"}'

# Login
curl -c cookies.txt -X POST http://localhost:18000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@n11demo.com","password":"User123!"}'

# Store access token
TOKEN=$(curl -s -c cookies.txt -X POST http://localhost:18000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@n11demo.com","password":"User123!"}' | jq -r .accessToken)
```

### Browse Products

```bash
# List all products
curl http://localhost:18000/api/products

# Search products
curl "http://localhost:18000/api/search?q=telefon&category=Elektronik&sort=price_asc"

# Get product by slug
curl http://localhost:18000/api/products/slug/samsung-galaxy-s24
```

### Shopping Flow

```bash
# Add item to basket
curl -X POST http://localhost:18000/api/basket/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"productName":"Samsung Galaxy S24","productPrice":49999.00,"quantity":1}'

# View basket
curl http://localhost:18000/api/basket -H "Authorization: Bearer $TOKEN"

# Checkout
curl -X POST http://localhost:18000/api/orders/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress":"Istanbul, Turkey","items":[{"productId":1,"productName":"Samsung Galaxy S24","productPrice":49999.00,"quantity":1}]}'

# Check order status (poll until PAID or CANCELLED)
curl http://localhost:18000/api/orders/42 -H "Authorization: Bearer $TOKEN"

# Check notifications
curl http://localhost:18000/api/notifications -H "Authorization: Bearer $TOKEN"
```

### Refresh Token

```bash
# Refresh (uses cookie from login)
curl -b cookies.txt -c cookies.txt -X POST http://localhost:18000/api/auth/refresh
```

---

## Related Documentation

- [Authentication](authentication.md) -- JWT and cookie details
- [Saga Patterns](saga-patterns.md) -- How checkout flows through services
- [Data Model](data-model.md) -- Entity schemas behind these endpoints
