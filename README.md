# n11 Clone — Mikroservis + Saga Pattern + Elasticsearch + Tracing + React

Spring Boot 3.3 / Java 21 tabanlı **9 mikroservis**, RabbitMQ üzerinde
**choreography-based Saga pattern**, Elasticsearch ile **faceted full-text search**,
OpenTelemetry + Jaeger ile **distributed tracing**, Spring Cloud Gateway, ve React +
Vite + Tailwind ile yazılmış n11 tarzı Türkçe e-ticaret arayüzü.

İki saga koreografisi iki farklı gerçek dağıtık işlem sorununu çözer:

- **UserRegistrationSaga** — yeni kullanıcı için otomatik sepet oluşturma + başarısızlıkta
  kullanıcıyı geri silme (compensation).
- **CheckoutSaga** — sipariş → **stok rezervasyonu** → ödeme → sepet temizleme + bildirim;
  stok yetersizse veya ödeme reddedilirse siparişi CANCELLED'a alma, rezerve edilen stoğu
  geri bırakma ve kullanıcıyı bilgilendirme.

CI/CD: GitHub Actions her push'ta 9 servis + frontend için paralel build + test koşar.

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
       ┌──────────┬─────────┼──────────┬───────┼──────────┬──────────┬──────────┐
       ▼          ▼         ▼          ▼       ▼          ▼          ▼          ▼
  ┌─────────┐ ┌────────┐ ┌────────┐ ┌───────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌─────────┐
  │ auth    │ │ basket │ │ product│ │ order │ │ payment│ │ notif. │ │ review │ │ search  │
  │ :8080   │ │ :8081  │ │ :8082  │ │ :8083 │ │ :8084  │ │ :8085  │ │ :8086  │ │ :8087   │
  │ authdb  │ │basketdb│ │product │ │orderdb│ │payment │ │ notif. │ │reviewdb│ │ (ES idx)│
  └────┬────┘ └────┬───┘ └────┬───┘ └───┬───┘ └────┬───┘ └────┬───┘ └────────┘ └────┬────┘
       │          │          │          │          │          │                     │
       │          │          │          │          │          │                 pulls from
       │          │          └──────────┼──────────┼──────────┼─────────────────────┘
       │          │                     │          │          │
       └──────────┴─────────────────────┴──────────┴──────────┘
                                    │
                         ┌──────────▼─────────┐          ┌──────────────────┐
                         │    RabbitMQ        │          │  Elasticsearch   │
                         │    saga.exchange   │          │   :9200          │
                         │    :5672 / :15672  │          │   (products idx) │
                         └────────────────────┘          └──────────────────┘
                                    │
                         ┌──────────▼─────────┐
                         │   PostgreSQL 16    │  :5432
                         │   (per-service DB) │
                         └────────────────────┘
```

---

## Servisler

| Servis | Port | Depo | Saga Rolü | Temel endpoint'ler |
|--------|------|------|-----------|--------------------|
| **auth-service** | 8080 | `authdb` | UserRegistered **publisher** + BasketCreationFailed **compensator** | `POST /api/auth/register\|login\|refresh\|logout`, `GET /api/users/me` |
| **basket-service** | 8081 | `basketdb` | UserRegistered **consumer** (empty cart), OrderConfirmed **consumer** (clear cart) | `GET /api/basket`, `POST /api/basket/items`, `PUT/DELETE /api/basket/items/{id}` |
| **product-service** | 8082 | `productdb` | — | `GET /api/products?category=&q=`, `GET /api/products/{id}`, `GET /api/products/slug/{slug}`, `GET /api/products/categories` |
| **order-service** | 8083 | `orderdb` | OrderCreated **publisher**, Payment{Succeeded\|Failed} **consumer**, Order{Confirmed\|Cancelled} **publisher** | `POST /api/orders/checkout`, `GET /api/orders`, `GET /api/orders/{id}` |
| **payment-service** | 8084 | `paymentdb` | OrderCreated **consumer**, Payment{Succeeded\|Failed} **publisher** | `GET /api/payments`, `GET /api/payments/order/{id}` |
| **notification-service** | 8085 | `notificationdb` | UserRegistered + OrderConfirmed + OrderCancelled **consumer** (fan-out) | `GET /api/notifications`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, `DELETE /api/notifications/{id}` |
| **review-service** | 8086 | `reviewdb` | — | `GET /api/reviews/product/{id}`, `GET /api/reviews/product/{id}/stats`, `POST /api/reviews`, `DELETE /api/reviews/{id}` |
| **search-service** | 8087 | Elasticsearch `products` index | — | `GET /api/search?q=&category=&brand=&minPrice=&maxPrice=&minRating=&sort=&page=&size=`, `POST /api/search/reindex` |
| **inventory-service** | 8088 | `inventorydb` | OrderCreated **consumer** (reserve), OrderCancelled **consumer** (release), InventoryReserved + InventoryOutOfStock **publisher** | `GET /api/inventory`, `GET /api/inventory/{productId}` |
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

### 2. CheckoutSaga (sipariş + stok rezervasyonu + ödeme)

```
order.checkout()
 ├─► orders.insert (status=PENDING)
 └─► publish "order.created"  (includes [productId, quantity] lines)
          │
          ▼
     inventory-service.onOrderCreated()                  ← atomically reserves all lines
      ├─► inventory_items: available--, reserved++
      ├─► reservations.insert per line
      │
      ├─► publish "inventory.reserved"    ┌─► publish "inventory.out-of-stock"
      ▼                                    ▼
 payment-service.onInventoryReserved()   order-service.onInventoryOutOfStock()
  ├─► payment_transactions.insert         ├─► orders.status = CANCELLED
  ├─► [mock decision]                     └─► publish "order.cancelled"
  │                                              └─► inventory releases stock (idempotent)
  ├─► publish "payment.succeeded"    ┌─► publish "payment.failed"
  ▼                                   ▼
 order-service.onPaymentSucceeded()  order-service.onPaymentFailed()
  ├─► orders.status = PAID            ├─► orders.status = CANCELLED
  └─► publish "order.confirmed"       └─► publish "order.cancelled"
       │                                     │
       ├─► basket.clear()                    ├─► inventory releases reserved stock
       └─► notification.save(CONFIRMED)      └─► notification.save(CANCELLED)
```

Three explicit failure modes, each with compensation:

1. **Inventory out-of-stock** — siparişi CANCELLED'a al, hiç ödeme almadığımız için kart
   hareketi yok, sadece kullanıcıya bildirim.
2. **Payment declined** — stok zaten rezerve edildi; `order.cancelled` inventory-service
   tarafından da tüketilir ve rezervasyonlar serbest bırakılır.
3. **İdempotent retry** — `release(orderId)` rezervasyon yoksa no-op; aynı event'in
   yeniden teslimi stok sayılarını bozmaz.

Mock ödeme karar mantığı (`payment-service`):

- e-posta "fail" içeriyor → ödeme reddedilir
- tutar > 100.000 TRY → ödeme reddedilir
- aksi halde onaylanır

Böylece üç başarısızlık yolu da UI'dan rahatça tetiklenebilir: `failuser@n11demo.com`
ile ödeme reddi, büyük miktarlı sepet ile limit aşımı, ve inventory seed'indeki
stoktan fazla ürün sipariş ederek out-of-stock.

### RabbitMQ topolojisi

Tek bir topic exchange: `saga.exchange`.

| Routing key | Yayın eden | Tüketen |
|-------------|------------|---------|
| `user.registered` | auth | basket, notification |
| `basket.creation.failed` | basket | auth |
| `order.created` | order | inventory |
| `inventory.reserved` | inventory | payment |
| `inventory.out-of-stock` | inventory | order |
| `payment.succeeded` | payment | order |
| `payment.failed` | payment | order |
| `order.confirmed` | order | basket, notification |
| `order.cancelled` | order | inventory, notification |

Her servis **kendi queue'sunu** aynı exchange'e uygun routing key ile bind eder. Kontrat
sadece **routing key + payload alan adları**; servisler birbirinin Java sınıflarını paylaşmaz.

---

## Arama (search-service + Elasticsearch)

**Index**: `products` — product-service'teki kataloğun denormalize kopyası. Metin alanları
ES'in built-in `turkish` analyzer'ı ile indekslenir, böylece "telefonlar" → "telefon"
kökleme ve diakritik (ç, ğ, ı, ö, ş, ü) normalize işlemleri sorgu zamanında çalışır.

**Sorgu yetenekleri** (`GET /api/search`):

| Parametre | Amaç |
|-----------|------|
| `q` | `multi_match` ile `name^3 / brand^2 / description` üzerinde arama + `fuzziness: AUTO` (yazım hatası toleransı) |
| `category`, `brand` | Keyword term filter |
| `minPrice`, `maxPrice` | `discountedPrice` üzerinde range filter |
| `minRating` | `rating ≥ X` filter |
| `sort` | `relevance` (varsayılan) \| `price_asc` \| `price_desc` \| `rating_desc` |
| `page`, `size` | Sayfalama |

**Facets**: her sorgu sonucu, aynı sorgu için ES aggregations üzerinden şu bilgileri
döner → frontend sol paneldeki filtreleri bu cevapla render eder:

- `brands` — marka başına ürün sayısı
- `categories` — kategori başına ürün sayısı
- `price` — `{ min, max }` aralığı

**Sync stratejisi**: `ProductIndexer`, servis açılışında `product-service`'ten
`/api/products` üzerinden sayfa sayfa pull edip bulk indexler. Bu, demo için yeterli
bir reconciliation yaklaşımıdır. `product-service` gelecekte `product.created/updated/deleted`
event'leri yayınlarsa, `@RabbitListener` ana yol olur ve bu bootstrap fallback olarak
kalır. `POST /api/search/reindex` manuel tetikleme için.

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
    ├── notifications/    # NotificationBell (polls unread-count every 15s)
    └── search/           # SearchPage + FacetSidebar + SearchResultCard (ES-backed)
```

### Saga'yı tarayıcıdan görme

- **Kayıt**: `/register` → giriş yapılır → 1–2 sn içinde zilde "Hoş geldin" bildirimi.
- **Başarılı ödeme**: ürün → sepet → `/checkout` → `/orders/{id}` sayfası 2 sn'de bir
  polluyor, `PENDING → PAID` geçişi canlı görülür. Sepet otomatik boşalır, "Siparişin
  onaylandı" bildirimi düşer.
- **Başarısız ödeme**: e-postanda "fail" varsa veya toplam 100.000 TRY'ı aşarsa sipariş
  birkaç saniye içinde `CANCELLED`'a döner, iptal bildirimi gelir.

### Arama deneyimi

- Navbar arama çubuğu ve kategori chip'leri `/search` rotasına gider — hepsi
  Elasticsearch üzerinden çalışır.
- URL, tüm filtrelerin tek kaynağıdır (`?q=&category=&brand=&minPrice=&...`); geri tuşu,
  sayfa yenileme ve deep link paylaşımı doğal çalışır.
- Sol facet paneli, her sorgu cevabındaki aggregations ile canlı güncellenir — seçim
  yapıldıkça diğer facet sayıları da revize olur.
- "telefoon" gibi yazım hataları `fuzziness: AUTO` ile düzeltilir.

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
| <http://localhost:16686> | **Jaeger UI** — saga trace waterfall'unu buradan izle |
| <http://localhost:9200> | Elasticsearch HTTP API |
| <http://localhost:9200/products/_search?pretty> | Ürün index'ini doğrudan sorgulama |
| <http://localhost:5432> | PostgreSQL (`postgres` / `postgres`) |

Gateway, dokuz servisin + Elasticsearch'ün + Jaeger'ın healthcheck'lerinin `healthy`
olmasını bekler — ilk soğuk açılış 1–2 dakika sürebilir (ES cluster'ın `yellow`'a
gelmesi + indexer'ın ürünleri çekmesi dahil).

### Demo hesapları

auth-service açılışta üç kullanıcı seed eder (idempotent — varsa atlar):

| E-posta | Şifre | Rol | Demo amacı |
|---------|-------|-----|------------|
| `admin@n11demo.com` | `Admin123!` | ADMIN + USER | Admin erişimi, `/api/users/admin` |
| `user@n11demo.com` | `User123!` | USER | **Happy-path checkout** — ödeme onaylanır |
| `failuser@n11demo.com` | `User123!` | USER | **Compensation path** — e-posta "fail" içerdiği için payment-service ödemeyi reddeder; order CANCELLED'a döner ve iptal bildirimi gelir |

review-service'in V2 migration'ı da seçili ürünler için 21 örnek yorum seed eder, böylece
ürün detay sayfasında ortalama puan ve yorum sayısı sıfırla açılmaz.

### Lokal geliştirme

**Gereksinimler:** Java 21, Maven, Node 20, PostgreSQL 16, RabbitMQ, Elasticsearch 8.

Her servisi kendi dizininden ayrı ayrı başlat:

```bash
# Örnek: auth-service
cd services/auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# search-service (product-service önce ayakta olmalı)
cd services/search-service
ELASTICSEARCH_URIS=http://localhost:9200 \
PRODUCT_SERVICE_URI=http://localhost:8082 \
  ./mvnw spring-boot:run

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
| `ELASTICSEARCH_URIS` | `http://elasticsearch:9200` | search-service'in ES adresi. |
| `PRODUCT_SERVICE_URI` | `http://product-service:8082` | search-service bootstrap indexer'ının pull hedefi. |
| `ES_JAVA_OPTS` | `-Xms512m -Xmx512m` | ES heap. Daha az RAM'li makinelerde 256m'e düşürülebilir. |
| `JWT_ACCESS_TOKEN_EXPIRY` | `900000` (15 dk) | Access token süresi (ms). |
| `JWT_REFRESH_TOKEN_EXPIRY` | `604800000` (7 gün) | Refresh token süresi. |

Per-servis override'lar her Dockerfile/application.yml içinde dokümante edildi.

---

## Standartlar

- **Error envelope**: RFC 7807 `application/problem+json`. Tüm servislerin
  `GlobalExceptionHandler`'ı `ProblemDetail` döner (`status`, `title`, `detail`, `instance`,
  `timestamp`, validasyonda `fields`, rate-limit'te `retryAfterSeconds`).
- **Migrations**: Flyway; her servisin `src/main/resources/db/migration/` klasöründe.
  Schema değişiklikleri yalnızca yeni `V{n}__*.sql` dosyasıyla. (search-service'in
  relational DB'si yoktur; ES mapping `@Document` anotasyonlarıyla gelir.)
- **JPA auditing**: `BaseEntity` ile `createdAt` / `updatedAt` otomatik.
- **Docker**: multi-stage build, çalışma image'i `eclipse-temurin:21-jre-alpine`,
  non-root `appuser`.
- **Healthcheck**: her servis kendi `/actuator/health` endpoint'iyle compose
  `depends_on: condition: service_healthy` kuralına uygun. Elasticsearch
  `_cluster/health?wait_for_status=yellow` ile sağlıklı kabul edilir.

---

## Güvenlik Notları

- JWT secret **en az 256-bit** olmalı ve ortam değişkeninden okunmalıdır.
- Refresh token'lar auth-service DB'sinde saklanır ve her `/refresh` çağrısında **rotate**
  edilir (eski revoke edilir + yeni üretilir).
- `/api/auth/**` üzerinde Bucket4j ile IP başına dk/10 rate limit.
- Kayıt şifresi: **8+ karakter · büyük harf · rakam · özel karakter** (`@StrongPassword`).
- Gateway tek giriş noktası; mikroservisler Docker ağı dışına expose edilmez.
- Elasticsearch lokal demo için `xpack.security` kapalı çalışır — **production'da mutlaka
  TLS + credential zorunlu yap**.

---

## Distributed Tracing (OpenTelemetry + Jaeger)

Her servis `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` ile gelir ve
OTLP HTTP üzerinden Jaeger'a (`http://jaeger:4318/v1/traces`) `%100` örnekleme ile span
gönderir. Spring Web / WebClient / RestClient / RabbitTemplate / Spring Data zaten
Micrometer-observed olduğu için tek bir `POST /api/orders/checkout` şu sırayı tek bir
**trace waterfall** olarak üretir:

```
browser → gateway → order-service (HTTP)
                  → RabbitMQ publish  (order.created)
                                     → inventory-service (AMQP consume)
                                     → PostgreSQL (reserve + insert)
                                     → RabbitMQ publish (inventory.reserved)
                                                        → payment-service
                                                        → RabbitMQ publish (payment.succeeded)
                                                                           → order-service
                                                                           → RabbitMQ publish (order.confirmed)
                                                                                              → basket, notification
```

Jaeger UI'da <http://localhost:16686> → `service = order-service` → son trace'i aç →
sağdaki timeline saga'nın bütün adımlarını sürüklenebilir bir waterfall olarak gösterir.
Bu, saga'nın soyut tarafını somutlaştıran en hızlı yol.

Tracing env vars (compose otomatik set ediyor):

| Değişken | Değer |
|----------|-------|
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | `http://jaeger:4318/v1/traces` |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` |

Production'da sampling'i düşürün (`0.1` gibi) — %100 sadece demo için.

---

## CI

`.github/workflows/ci.yml` her `push` ve `pull_request`'te:

- **backend job** — matrix build: 9 servisin her biri için `mvn -B -ntp verify`.
  Maven dependency cache açık. `verify` test suite'ini ve Spring Boot packaging'i
  çalıştırır.
- **frontend job** — `npm run build` (`tsc -b` dahil), böylece TS tip hataları
  pipeline'ı düşürür.
- Concurrency group aynı branch'e yeni push gelince in-flight run'ları iptal eder.

Badge eklemek için: `![CI](https://github.com/SamedBilginAlternet/JwtJava/actions/workflows/ci.yml/badge.svg)`

---

## Test

Her servisin kendi testleri var. Tek bir servis için:

```bash
cd services/<service-name> && ./mvnw test
```

Tüm projeyi (root'ta bir reactor POM yok; kısa yoldan):

```bash
for d in services/*/; do (cd "$d" && ./mvnw -q test); done
```

Testler **tamamen Mockito birim testleri** — Spring context yüklemezler, DB/RabbitMQ/ES
gerektirmezler, milisaniyeler içinde koşarlar. auth-service'in integration testleri
`@SpringBootTest + H2` ile RFC 7807 response envelope'unu da doğrular.

| Servis | Test dosyaları | Neyi pin'liyor |
|--------|-----------------|----------------|
| **auth-service** | `JwtServiceTest`, `AuthServiceTest`, `StrongPasswordValidatorTest`, `AuthControllerIntegrationTest`, `UserControllerIntegrationTest` | Token üretim/doğrulama, register/login hata senaryoları, şifre politikası, uçtan uca kontroller + RFC 7807 doğrulaması |
| **basket-service** | `BasketServiceTest`, `UserRegisteredListenerTest`, `OrderConfirmedListenerTest` | Aynı ürünü iki kez eklerken quantity merge, idempotent `createEmptyBasketFor`, compensation event'i yayınlama, `order.confirmed` ile sepet temizleme |
| **product-service** | `ProductServiceTest` | Filter routing (hiçbiri / kategori / q), q önceliği, whitespace trim, server-side `discountedPrice`, 404 yolu, 8 kategori kontratı |
| **order-service** | `OrderServiceTest`, `PaymentListenerTest` | Server-side total hesabı, OrderCreated yayını, PAID/CANCELLED state geçişleri + OrderConfirmed/Cancelled yayını, replay safety (bulunmayan order no-op) |
| **payment-service** | `PaymentProcessorTest` | Karar matrisi (normal / "fail" e-posta / limit aşımı / tam sınır), transaction persist + saga event'inin doğru routing key'e gitmesi |
| **review-service** | `ReviewControllerTest` | (product, user) tekilliği, display name'in e-postadan türetilmesi, DELETE'in sahibine scoped olması (yabancı review → 404) |
| **notification-service** | `NotificationEventListenerTest` | 3 saga event'inin doğru `NotificationType`'a map'lenmesi, Türkçe para formatı, `read=false` default'u |
| **inventory-service** | `InventoryServiceTest` | All-or-nothing reservation, OutOfStockException'da rollback, unknown product → fail, release stok restorasyon matematiği, release idempotency |

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
