# n11 Clone — Mikroservis + Saga Pattern + React

Spring Boot 3.3 / Java 21 tabanlı 7 mikroservis, RabbitMQ üzerinde **choreography-based
Saga pattern**, Spring Cloud Gateway, ve React + Vite + Tailwind ile yazılmış n11 tarzı
Türkçe e-ticaret arayüzü.

İki saga koreografisi iki farklı gerçek dağıtık işlem sorununu çözer:

- **UserRegistrationSaga** — yeni kullanıcı için otomatik sepet oluşturma + başarısızlıkta
  kullanıcıyı geri silme (compensation).
- **CheckoutSaga** — sipariş → ödeme → sepet temizleme + bildirim; ödeme reddedilirse
  siparişi CANCELLED'a alma + kullanıcıyı bilgilendirme.

---

## Mimari

```
                         ┌──────────────────────┐
          browser ─────► │  frontend (nginx)    │  :3000
                         │  React + Vite + TS   │
                         └──────────┬───────────┘
                                    │  /api/*  (proxied)
                         ┌──────────▼───────────┐
                         │   api-gateway        │  :8000
                         │   Spring Cloud GW    │
                         └──┬──────────────────┬┘
                            │                  │
        ┌───────────────────┼────────┬─────────┼──────────────┬──────────────┐
        ▼                   ▼        ▼         ▼              ▼              ▼
  ┌─────────────┐   ┌──────────────┐ ┌──────────────┐  ┌──────────────┐ ┌──────────────┐
  │ auth        │   │ basket       │ │ product      │  │ order        │ │ review       │
  │ :8080       │   │ :8081        │ │ :8082        │  │ :8083        │ │ :8086        │
  │ authdb      │   │ basketdb     │ │ productdb    │  │ orderdb      │ │ reviewdb     │
  └──────┬──────┘   └──────┬───────┘ └──────────────┘  └──────┬───────┘ └──────────────┘
         │                 │                                   │
         │                 │        ┌──────────────┐           │
         │                 │        │ payment      │           │
         │                 │        │ :8084        │           │
         │                 │        │ paymentdb    │           │
         │                 │        └──────┬───────┘           │
         │                 │               │                    │
         │                 │        ┌──────▼───────┐            │
         │                 │        │ notification │            │
         │                 │        │ :8085        │            │
         │                 │        │ notif.db     │            │
         │                 │        └──────┬───────┘            │
         │                 │               │                    │
         └─────────────────┴────────┬──────┴────────────────────┘
                                    ▼
                         ┌────────────────────┐
                         │    RabbitMQ        │  :5672 / :15672 (mgmt UI)
                         │    saga.exchange   │
                         └────────────────────┘
                                    │
                         ┌──────────▼─────────┐
                         │   PostgreSQL 16    │  :5432
                         │   (per-service DB) │
                         └────────────────────┘
```

---

## Servisler

| Servis | Port | DB | Saga Rolü | Temel endpoint'ler |
|--------|------|----|-----------|--------------------|
| **auth-service** | 8080 | `authdb` | UserRegistered **publisher** + BasketCreationFailed **compensator** | `POST /api/auth/register\|login\|refresh\|logout`, `GET /api/users/me` |
| **basket-service** | 8081 | `basketdb` | UserRegistered **consumer** (empty cart), OrderConfirmed **consumer** (clear cart) | `GET /api/basket`, `POST /api/basket/items`, `PUT/DELETE /api/basket/items/{id}` |
| **product-service** | 8082 | `productdb` | — | `GET /api/products?category=&q=`, `GET /api/products/{id}`, `GET /api/products/slug/{slug}`, `GET /api/products/categories` |
| **order-service** | 8083 | `orderdb` | OrderCreated **publisher**, Payment{Succeeded\|Failed} **consumer**, Order{Confirmed\|Cancelled} **publisher** | `POST /api/orders/checkout`, `GET /api/orders`, `GET /api/orders/{id}` |
| **payment-service** | 8084 | `paymentdb` | OrderCreated **consumer**, Payment{Succeeded\|Failed} **publisher** | `GET /api/payments`, `GET /api/payments/order/{id}` |
| **notification-service** | 8085 | `notificationdb` | UserRegistered + OrderConfirmed + OrderCancelled **consumer** (fan-out) | `GET /api/notifications`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, `DELETE /api/notifications/{id}` |
| **review-service** | 8086 | `reviewdb` | — | `GET /api/reviews/product/{id}`, `GET /api/reviews/product/{id}/stats`, `POST /api/reviews`, `DELETE /api/reviews/{id}` |
| **api-gateway** | 8000 | — | — | Public giriş noktası, tüm `/api/**` yolları uygun servise yönlendirir |

Her Spring Boot servisi `/actuator/health` ve `/actuator/info` açar. auth-service Swagger UI'sı
gateway üzerinden `http://localhost:8000/swagger-ui.html`.

---

## Saga Akışları

### 1. UserRegistrationSaga (kayıt)

```
auth.register()
 ├─► users.insert
 └─► publish "user.registered"
          │
          ▼
     basket-service.onUserRegistered()
      ├─► baskets.insert (empty)
      └─► (hata olursa)
           publish "basket.creation.failed"
                │
                ▼
           auth-service.onBasketCreationFailed()
            └─► users.delete  ← compensation
```

### 2. CheckoutSaga (sipariş + ödeme)

```
order.checkout()
 ├─► orders.insert (status=PENDING)
 └─► publish "order.created"
          │
          ▼
     payment-service.onOrderCreated()
      ├─► payment_transactions.insert
      ├─► [mock decision]
      ├─► publish "payment.succeeded"            ┌─► publish "payment.failed"
      │                                          │
      ▼                                          ▼
 order-service.onPaymentSucceeded()         order-service.onPaymentFailed()
  ├─► orders.status = PAID                   ├─► orders.status = CANCELLED
  └─► publish "order.confirmed"              └─► publish "order.cancelled"
           │                                              │
           ├───► basket-service.clear()                   │
           └───► notification-service.save(CONFIRMED)     ├───► notification-service.save(CANCELLED)
```

Mock ödeme karar mantığı (`payment-service`):

- e-posta "fail" içeriyor → ödeme reddedilir
- tutar > 100.000 TRY → ödeme reddedilir
- aksi halde onaylanır

Böylece happy path ve compensation path UI'dan rahatça tetiklenebilir.

### RabbitMQ topolojisi

Tek bir topic exchange: `saga.exchange`.

| Routing key | Yayın eden | Tüketen |
|-------------|------------|---------|
| `user.registered` | auth | basket, notification |
| `basket.creation.failed` | basket | auth |
| `order.created` | order | payment |
| `payment.succeeded` | payment | order |
| `payment.failed` | payment | order |
| `order.confirmed` | order | basket, notification |
| `order.cancelled` | order | notification |

Her servis **kendi queue'sunu** aynı exchange'e uygun routing key ile bind eder. Kontrat
sadece **routing key + payload alan adları**; servisler birbirinin Java sınıflarını paylaşmaz.

---

## Frontend

`frontend/` — Vite + React 18 + TypeScript + Tailwind. Üretimde nginx arkasında çalışır,
`/api/*` gateway'e proxy'lenir (CORS preflight yok).

### Klasör yapısı (feature-based)

```
frontend/src/
├── app/                  # router + ErrorBoundary
├── layout/               # Navbar + Layout
├── shared/
│   ├── api/client.ts     # apiFetch + ApiError (RFC 7807 aware)
│   ├── api/problem.ts    # errorMessage / errorFields helpers
│   ├── hooks/useApi.ts   # data + loading + error + refetch (race-safe)
│   ├── providers/ToastProvider.tsx
│   ├── ui/{Button,Input,Card,Spinner,Badge,RatingStars}.tsx
│   └── utils/format.ts   # formatTRY, formatDateTime
└── features/
    ├── auth/             # store (zustand+persist), api, Login/RegisterPage
    ├── products/         # HomePage, ProductDetailPage, ProductCard, CategoryBar
    ├── basket/           # store, api, BasketPage
    ├── orders/           # api, CheckoutPage, OrdersPage, OrderDetailPage (polls PENDING)
    ├── reviews/          # ReviewList (embedded on product page)
    └── notifications/    # NotificationBell (polls unread-count every 15s)
```

### Saga'yı tarayıcıdan görme

- **Kayıt**: `/register` → giriş yapılır → 1–2 sn içinde zilde "Hoş geldin" bildirimi.
- **Başarılı ödeme**: ürün → sepet → `/checkout` → `/orders/{id}` sayfası 2 sn'de bir
  polluyor, `PENDING → PAID` geçişi canlı görülür. Sepet otomatik boşalır, "Siparişin
  onaylandı" bildirimi düşer.
- **Başarısız ödeme**: e-postanda "fail" varsa veya toplam 100.000 TRY'ı aşarsa sipariş
  birkaç saniye içinde `CANCELLED`'a döner, iptal bildirimi gelir.

---

## Hızlı Başlangıç

### Docker ile (önerilen)

```bash
docker compose up --build
```

Tüm bileşenler ayağa kalkınca:

| URL | Ne? |
|-----|-----|
| <http://localhost:3000> | React arayüzü |
| <http://localhost:8000> | API gateway |
| <http://localhost:8000/swagger-ui.html> | auth-service Swagger UI |
| <http://localhost:15672> | RabbitMQ yönetim paneli (`guest` / `guest`) |
| <http://localhost:5432> | PostgreSQL (`postgres` / `postgres`) |

Gateway, yedi servisin healthcheck'lerinin `healthy` olmasını bekler — ilk soğuk açılış
1–2 dakika sürebilir.

### Lokal geliştirme

**Gereksinimler:** Java 21, Maven, Node 20, PostgreSQL 16, RabbitMQ.

Her servisi kendi dizininden ayrı ayrı başlat:

```bash
# Örnek: auth-service
cd services/auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend
npm install
npm run dev          # http://localhost:5173, /api proxied to :8000
```

---

## Ortam Değişkenleri (Docker)

| Değişken | Varsayılan | Notlar |
|----------|-----------|--------|
| `JWT_SECRET` | (demo key) | **Production'da mutlaka değiştir.** Tüm servislerin JWT imzalı/doğrulanmış tokenları aynı secret ile kullanır. |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `postgres` / `postgres` | Tek Postgres instance, servis başına ayrı DB. |
| `RABBITMQ_DEFAULT_USER` / `_PASS` | `guest` / `guest` | AMQP 5672, yönetim UI 15672. |
| `JWT_ACCESS_TOKEN_EXPIRY` | `900000` (15 dk) | Access token süresi (ms). |
| `JWT_REFRESH_TOKEN_EXPIRY` | `604800000` (7 gün) | Refresh token süresi. |

Per-servis override'lar her Dockerfile/application.yml içinde dokümante edildi.

---

## Standartlar

- **Error envelope**: RFC 7807 `application/problem+json`. Tüm servislerin
  `GlobalExceptionHandler`'ı `ProblemDetail` döner (`status`, `title`, `detail`, `instance`,
  `timestamp`, validasyonda `fields`, rate-limit'te `retryAfterSeconds`).
- **Migrations**: Flyway; her servisin `src/main/resources/db/migration/` klasöründe.
  Schema değişiklikleri yalnızca yeni `V{n}__*.sql` dosyasıyla.
- **JPA auditing**: `BaseEntity` ile `createdAt` / `updatedAt` otomatik.
- **Docker**: multi-stage build, çalışma image'i `eclipse-temurin:21-jre-alpine`,
  non-root `appuser`.
- **Healthcheck**: her servis kendi `/actuator/health` endpoint'iyle compose
  `depends_on: condition: service_healthy` kuralına uygun.

---

## Güvenlik Notları

- JWT secret **en az 256-bit** olmalı ve ortam değişkeninden okunmalıdır.
- Refresh token'lar auth-service DB'sinde saklanır ve her `/refresh` çağrısında **rotate**
  edilir (eski revoke edilir + yeni üretilir).
- `/api/auth/**` üzerinde Bucket4j ile IP başına dk/10 rate limit.
- Kayıt şifresi: **8+ karakter · büyük harf · rakam · özel karakter** (`@StrongPassword`).
- Gateway tek giriş noktası; mikroservisler Docker ağı dışına expose edilmez.

---

## Test

Birim ve integration testleri auth-service'te mevcut:

```bash
cd services/auth-service && ./mvnw test
```

- `JwtServiceTest` — token üretimi/doğrulama, expiry.
- `AuthServiceTest` — register/login, UserAlreadyExistsException, saga publisher mock'lanır.
- `StrongPasswordValidatorTest` — şifre politikası.
- `AuthControllerIntegrationTest`, `UserControllerIntegrationTest` — `@SpringBootTest` +
  H2, RFC 7807 cevap doğrulaması (`application/problem+json`).

Diğer servisler için testler opsiyonel olarak eklenebilir — mevcut auth-service testleri
saga'nın yayınladığı event'i `SagaEventPublisher` mock'u ile izole doğruluyor.

---

## Commit Konvansiyonu

Tek özellik → tek commit. Conventional Commits benzeri prefix kullanılır:

- `feat:` yeni özellik / servis
- `fix:` hata düzeltmesi
- `refactor:` davranışı değiştirmeyen iç yeniden yapılanma
- `chore:` compose / build / altyapı
- `docs:` README vb.
- `test:` test ekleme

---

## Lisans

Eğitim amaçlı demo projesi; production için değildir.
