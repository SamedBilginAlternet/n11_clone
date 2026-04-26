# 11 Mikroservis, 16 Container, 1 Sunucu: E-Ticaret Platformu Nasıl Ayağa Kalktı

*Java 21, Spring Boot, React, Docker, RabbitMQ, Redis, Elasticsearch ve tam observability stack ile sıfırdan bir e-ticaret platformu inşa ettim. Bu yazıda mimariyi, öğrendiklerimi ve yaptığım hataları paylaşıyorum.*

---

## Neden Bu Projeyi Yaptım?

n11 bootcamp sürecinde mikroservis mimarisi, event-driven sistemler ve distributed transactions konularını öğreniyordum. Ama sadece teoriyi okumak yetmiyordu — bir şeyi gerçekten anlamak için sıfırdan inşa etmem gerekiyordu.

Kendime şu soruyu sordum: *"Bir e-ticaret platformunun checkout akışında ödeme başarısız olursa, stok nasıl geri verilir?"*

Bu sorunun cevabını ararken Saga pattern'i öğrendim, RabbitMQ ile choreography-based event sistemi kurdum ve sonunda 11 mikroservisten oluşan bir platform ortaya çıktı.

**Canlı demo:** [https://n11.samedbilgin.com](https://n11.samedbilgin.com)
**Kaynak kod:** [GitHub](https://github.com/SamedBilginAlternet/n11_clone)

---

## Büyük Resim

İlk olarak sistemin genel görünümünü paylaşayım:

```mermaid
graph TB
    subgraph Client
        FE[React Frontend<br/>TypeScript + Zustand]
    end

    subgraph API Layer
        GW[API Gateway<br/>Spring Cloud Gateway]
        EU[Eureka Server<br/>Service Discovery]
    end

    subgraph Business Services
        AUTH[Auth Service<br/>JWT + Refresh Token]
        PROD[Product Service<br/>CRUD + Cache]
        BASK[Basket Service<br/>Sepet Yönetimi]
        ORD[Order Service<br/>Sipariş Orchestration]
        PAY[Payment Service<br/>Ödeme İşleme]
        INV[Inventory Service<br/>Stok Yönetimi]
        NOTIF[Notification Service<br/>WebSocket Push]
        REV[Review Service<br/>Değerlendirmeler]
        SRCH[Search Service<br/>Full-Text Arama]
    end

    subgraph Data Stores
        PG[(PostgreSQL<br/>7 Database)]
        RD[(Redis<br/>Cache)]
        ES[(Elasticsearch<br/>Search Index)]
    end

    subgraph Messaging
        RMQ[RabbitMQ<br/>saga.exchange]
    end

    subgraph Observability
        PROM[Prometheus] --> GRAF[Grafana]
        JAEG[Jaeger<br/>Distributed Tracing]
        LOKI[Loki<br/>Centralized Logs]
    end

    FE -->|HTTPS| GW
    GW -->|lb://| AUTH & PROD & BASK & ORD & PAY & INV & NOTIF & REV & SRCH
    GW -.->|register/discover| EU
    AUTH & PROD & BASK & ORD & PAY & INV & REV --> PG
    PROD --> RD
    SRCH --> ES
    AUTH & BASK & ORD & PAY & INV & NOTIF & SRCH -->|publish/consume| RMQ
    NOTIF -->|WebSocket STOMP| FE
```

16 container, tek bir `docker-compose.yml` ile ayağa kalkıyor. Bunu başardığımda gerçekten gururlandım.

---

## Checkout Saga: En Çok Öğrendiğim Kısım

Projenin en zor ve en öğretici kısmı buydu. Bir kullanıcı sepetini onaylayınca arka planda 5 servis koordineli çalışıyor. Herhangi birinde hata olursa geri sarma (compensation) yapılması gerekiyor.

Buna **Saga Pattern** deniyor. Ben choreography yaklaşımını seçtim — merkezi bir orchestrator yok, servisler birbirlerine event fırlatarak haberleşiyor.

```mermaid
sequenceDiagram
    participant B as Basket Service
    participant O as Order Service
    participant I as Inventory Service
    participant P as Payment Service
    participant N as Notification Service

    B->>O: checkout isteği
    O->>O: Sipariş oluştur (PENDING)
    O-->>I: order.created 📦

    I->>I: Stok kontrol & rezerve et
    alt Stok yeterli
        I-->>P: inventory.reserved ✅
        P->>P: Ödemeyi işle
        alt Ödeme başarılı
            P-->>O: payment.succeeded 💰
            O->>O: Status → PAID
            O-->>B: order.confirmed ✅
            O-->>N: order.confirmed 🔔
            B->>B: Sepeti temizle
            N->>N: WebSocket push → kullanıcıya bildirim
        else Ödeme başarısız
            P-->>O: payment.failed ❌
            O->>O: Status → CANCELLED
            O-->>I: order.cancelled 🔄
            I->>I: Stoku geri ver
        end
    else Stok yetersiz
        I-->>O: inventory.out-of-stock ❌
        O->>O: Status → CANCELLED
    end
```

### Bunu Nasıl Implement Ettim?

Tüm event'ler tek bir RabbitMQ **topic exchange** üzerinden akıyor:

```mermaid
graph LR
    subgraph saga.exchange
        direction LR
        E[Topic Exchange]
    end

    O[Order Service] -->|order.created| E
    O -->|order.confirmed| E
    O -->|order.cancelled| E
    I[Inventory Service] -->|inventory.reserved| E
    I -->|inventory.out-of-stock| E
    P[Payment Service] -->|payment.succeeded| E
    P -->|payment.failed| E
    A[Auth Service] -->|user.registered| E

    E -->|order.created| IQ[inventory queue]
    E -->|inventory.reserved| PQ[payment queue]
    E -->|payment.succeeded| OQ[order queue]
    E -->|payment.failed| OQ2[order queue]
    E -->|order.confirmed| BQ[basket queue]
    E -->|order.confirmed| NQ[notification queue]
    E -->|order.cancelled| IQ2[inventory queue]
    E -->|user.registered| BQ2[basket queue]
```

Her servis sadece kendi işini yapıyor, sonucu event olarak fırlatıyor. Kimse kimsenin iç detayını bilmiyor. İlk başta "bu kadar decoupled olmasına gerek var mı" diye düşündüm ama bir servisi değiştirdiğimde diğerlerinin hiç etkilenmediğini görünce anladım ki bu yaklaşım doğruymuş.

**Öğrendiğim en önemli şey:** Saga pattern'i observability olmadan debug etmek imkansız. İlk denememde bir event kayboldu, saatlerce log'lara baktım. Jaeger'ı entegre ettikten sonra trace'e bakıp 2 dakikada sorunu buldum.

---

## Kullanıcı Kayıt Saga'sı

Checkout kadar karmaşık olmayan ama yine event-driven olan ikinci saga:

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth Service
    participant B as Basket Service
    participant N as Notification Service

    C->>A: POST /api/auth/register
    A->>A: Kullanıcı oluştur (BCrypt hash)
    A-->>B: user.registered 📦
    A-->>N: user.registered 🔔

    B->>B: Boş sepet oluştur
    alt Sepet oluşturma başarısız
        B-->>A: basket.creation.failed ❌
        A->>A: Kullanıcıyı sil (compensation)
    end

    N->>N: Hoşgeldin bildirimi gönder
```

Burada compensation'ı ilk kez uyguladım. Sepet oluşturulamadığında auth-service kullanıcıyı geri siliyor. Distributed transaction'ın "ya hep ya hiç" mantığını event-driven şekilde sağlamış oluyoruz.

---

## JWT Güvenlik Akışı

Auth konusunda en çok refresh token rotation'ı implement etmeyi öğrendim. Access token 15 dakika sürüyor — süresi dolduğunda kullanıcı tekrar login olmak zorunda kalmasın diye refresh token kullanıyorum.

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant A as Auth Service
    participant DB as PostgreSQL

    Note over C,A: 1. Login
    C->>G: POST /api/auth/login {email, password}
    G->>A: forward
    A->>DB: Kullanıcı bul + BCrypt doğrula
    A->>A: Access token üret (15dk, HS256)
    A->>DB: Refresh token kaydet (7 gün)
    A->>C: {accessToken, refreshToken}

    Note over C,G: 2. API çağrısı
    C->>G: GET /api/products<br/>Authorization: Bearer {accessToken}
    G->>G: JwtAuthFilter → token geçerli mi?
    G->>A: forward (lb:// ile)
    A->>C: 200 OK

    Note over C,A: 3. Token yenileme (15dk sonra)
    C->>A: POST /api/auth/refresh {refreshToken}
    A->>DB: Refresh token geçerli mi? Revoke edilmiş mi?
    A->>DB: Eski token'ı revoke et
    A->>A: Yeni access + refresh token üret
    A->>DB: Yeni refresh token kaydet
    A->>C: {newAccessToken, newRefreshToken}
```

**Bucket4j ile rate limiting** de ekledim — auth endpoint'lerine dakikada 10 istek sınırı. Brute force saldırılarına karşı basit ama etkili bir önlem.

---

## Redis Cache Stratejisi

Product service'te her istek PostgreSQL'e gidiyordu ve yavaştı. Redis ekledikten sonra response süreleri 50ms'den 2ms'ye düştü.

```mermaid
graph TD
    REQ[GET /api/products/42] --> CHECK{Redis'te var mı?}
    CHECK -->|Cache HIT| RES[Response 2ms ⚡]
    CHECK -->|Cache MISS| DB[(PostgreSQL)]
    DB --> SAVE[Redis'e kaydet]
    SAVE --> RES2[Response 50ms]

    UPD[PUT /api/products/42] --> EVICT[Cache temizle]
    EVICT --> DB2[(PostgreSQL güncelle)]
```

Farklı veri tipleri için farklı TTL'ler kullandım:

| Cache | TTL | Neden? |
|-------|-----|--------|
| Ürün (ID ile) | 30 dakika | Sık güncellenmez |
| Kategoriler | 24 saat | Neredeyse hiç değişmez |
| Arama sonuçları | 10 dakika | Sık değişebilir |

En önemli öğrendiğim şey: **Redis çökerse uygulama da çökmemeli.** Bunun için custom `CacheErrorHandler` yazdım — Redis erişilemezse log basıp DB'ye fallback yapıyor. İlk başta bunu yapmamıştım, Redis restart'ta tüm servis 500 veriyordu.

---

## CQRS: Yazma ve Okuma Modellerini Ayırmak

Ürün verileri PostgreSQL'de (write), arama için Elasticsearch'te (read) tutuluyor. Senkronizasyon RabbitMQ event'leri ile:

```mermaid
graph LR
    subgraph Write Side
        PS[Product Service] -->|INSERT/UPDATE/DELETE| PG[(PostgreSQL)]
        PS -->|product.created/updated/deleted| RMQ[RabbitMQ]
    end

    subgraph Read Side
        RMQ -->|consume| SS[Search Service]
        SS -->|index/update/delete| ES[(Elasticsearch)]
    end

    subgraph Client Query
        CL[Client] -->|GET /api/search?q=telefon| SS
        SS -->|multi-match + fuzzy| ES
        ES -->|results| SS
        SS -->|facets + results| CL
    end
```

Elasticsearch'te **Türkçe analyzer** kullanıyorum — "telefonlar" yazınca "telefon" da buluyor. Fuzzy matching sayesinde "telfon" yazsan bile doğru sonuçlar geliyor.

---

## Observability: Hayat Kurtaran Üçlü

İlk başta observability'yi "gereksiz overhead" olarak görüyordum. İlk production bug'ında fikrimi değiştirdim.

```mermaid
graph TB
    subgraph Services
        S1[Auth] & S2[Product] & S3[Order] & S4[Payment]
    end

    subgraph Metrics Pipeline
        S1 & S2 & S3 & S4 -->|/actuator/prometheus| PROM[Prometheus<br/>Scrape every 15s]
        PROM --> GRAF[Grafana<br/>Dashboards]
    end

    subgraph Tracing Pipeline
        S1 & S2 & S3 & S4 -->|OTLP gRPC| OTEL[OTel Collector]
        OTEL --> JAEG[Jaeger<br/>Trace Visualization]
    end

    subgraph Logging Pipeline
        S1 & S2 & S3 & S4 -->|stdout| DOCKER[Docker Logs]
        DOCKER --> PRTL[Promtail<br/>Log Collector]
        PRTL --> LOKI[Loki<br/>Log Storage]
        LOKI --> GRAF
    end
```

**Correlation ID** her request'te üretiliyor ve tüm servislere taşınıyor. Gateway'de başlıyor, her servisin log'unda görünüyor:

```
[correlationId=abc-123, traceId=def-456] OrderService - Sipariş oluşturuldu: #42
[correlationId=abc-123, traceId=def-456] InventoryService - Stok rezerve edildi: 3 adet
[correlationId=abc-123, traceId=def-456] PaymentService - Ödeme başarılı: 299.90 TRY
```

Bir sorun olduğunda correlation ID ile grep yapıyorum — tek bir request'in 8 servisteki yolculuğunu baştan sona görebiliyorum.

---

## WebSocket ile Gerçek Zamanlı Bildirimler

Sipariş onaylandığında kullanıcıya anında bildirim gitmesini istiyordum. Polling (sürekli istek atma) yerine **WebSocket** tercih ettim:

```mermaid
sequenceDiagram
    participant C as React Client
    participant N as Notification Service
    participant RMQ as RabbitMQ

    Note over C,N: WebSocket bağlantısı (JWT ile auth)
    C->>N: CONNECT /ws<br/>Authorization: Bearer {token}
    N->>N: JWT doğrula
    N->>C: CONNECTED ✅
    C->>N: SUBSCRIBE /user/queue/notifications

    Note over RMQ,N: Saga event geldiğinde
    RMQ-->>N: order.confirmed
    N->>N: Bildirim kaydet (DB)
    N->>C: MESSAGE: "Siparişiniz onaylandı! 🎉"

    Note over C: React'te toast notification göster
```

STOMP protokolü kullandım — her kullanıcı kendi queue'suna subscribe oluyor. Bir kullanıcının bildirimi başka kullanıcıya gitmiyor.

---

## Veritabanı Stratejisi: Her Servise Ayrı DB

```mermaid
graph TD
    AUTH[Auth Service] --> ADB[(authdb<br/>users, refresh_tokens)]
    BASK[Basket Service] --> BDB[(basketdb<br/>baskets, basket_items)]
    PROD[Product Service] --> PDB[(productdb<br/>products, categories)]
    ORD[Order Service] --> ODB[(orderdb<br/>orders, order_items)]
    PAY[Payment Service] --> PAYDB[(paymentdb<br/>payment_transactions)]
    NOTIF[Notification Service] --> NDB[(notificationdb<br/>notifications)]
    REV[Review Service] --> RDB[(reviewdb<br/>reviews)]
    INV[Inventory Service] --> IDB[(inventorydb<br/>inventory, reservations)]

    style ADB fill:#e1f5fe
    style BDB fill:#e1f5fe
    style PDB fill:#e1f5fe
    style ODB fill:#e1f5fe
    style PAYDB fill:#e1f5fe
    style NDB fill:#e1f5fe
    style RDB fill:#e1f5fe
    style IDB fill:#e1f5fe
```

Her servis sadece kendi veritabanına erişiyor. Başka servisin verisine ihtiyaç varsa event ile alıyor, doğrudan DB'ye bağlanmıyor.

Schema yönetimi için **Flyway** kullanıyorum — `ddl-auto: update` yerine versiyonlanmış SQL migration'lar. Bootcamp'te hoca `ddl-auto: update` ile çalışıyordu, production'da ne kadar tehlikeli olduğunu araştırınca Flyway'e geçtim.

---

## Deploy: Tek Sunucu, 16 Container

Tüm sistemi **DigitalOcean** üzerinde 4 vCPU, 8 GB RAM bir sunucuya deploy ettim. GitHub Student Pack sayesinde $200 kredi ile 8 ay bedava.

```mermaid
graph TB
    subgraph DigitalOcean Droplet - 8 GB RAM
        subgraph Docker Compose
            NG[Nginx<br/>:80/:443<br/>SSL Termination]
            GW[API Gateway]
            EU[Eureka]

            subgraph App Services - 96MB heap each
                A[Auth] & B[Basket] & P[Product]
                O[Order] & PAY[Payment] & N[Notification]
                R[Review] & S[Search] & I[Inventory]
            end

            subgraph Data
                PG[(PostgreSQL)] & RD[(Redis)] & RMQ[RabbitMQ] & ES[(Elastic)]
            end
        end
    end

    DNS[n11.samedbilgin.com] -->|A Record| NG
    NG -->|proxy_pass| GW
    GW -->|lb://| A & B & P & O & PAY & N & R & S & I

    CERT[Let's Encrypt<br/>Auto-renewal] -.-> NG
```

JVM heap'leri 96 MB'a düşürdüm, PostgreSQL'i tune ettim, observability stack'ı çıkardım — 3.1 GB'a sığdı. İlk başta 192 MB heap ile denedim, OOM killer tüm container'ları öldürdü. Swap eklemek de önemli bir ders oldu.

---

## Kullandığım Pattern'lerin Özeti

```mermaid
mindmap
  root((E-Commerce<br/>Platform))
    Architecture
      Microservices
      API Gateway
      Service Discovery
      Per-Service Database
    Communication
      Saga Choreography
      Event-Driven (RabbitMQ)
      CQRS
      WebSocket (STOMP)
    Security
      JWT + Refresh Token
      Rate Limiting (Bucket4j)
      BCrypt Password Hashing
      CORS Configuration
    Data
      Redis Cache-Aside
      Elasticsearch Full-Text
      Flyway Migrations
      PostgreSQL per service
    Observability
      Distributed Tracing (Jaeger)
      Metrics (Prometheus + Grafana)
      Centralized Logging (Loki)
      Correlation ID Propagation
    Design Patterns
      Builder (Lombok)
      Factory (DTO mapping)
      Observer (RabbitMQ listeners)
      Repository (Spring Data)
      Filter Chain (Security)
      Strategy (Search sorting)
```

---

## Öğrendiğim 7 Şey

**1. Saga pattern'i debug etmek için observability şart.**
İlk denememde Jaeger yoktu. Bir event kaybolduğunda saatlerce baktım. Trace'ler olunca 2 dakikada buldum.

**2. Redis çökünce uygulama çökmemeli.**
CacheErrorHandler yazmayı unutmuştum. Redis restart'ta tüm product-service 500 verdi. Fallback mekanizması hayat kurtarıyor.

**3. `ddl-auto: update` production'da kullanılmaz.**
Bootcamp'te hep update kullandık. Araştırınca gördüm ki production'da kolon silinmesi, veri kaybı riski var. Flyway ile versiyonlanmış migration çok daha güvenli.

**4. Docker Compose küçük ölçekte yeterli.**
Kubernetes öğrenmeliyim diye stres yaptım ama 16 container'ı Compose ile gayet iyi yönettim. Health check + restart policy ile çoğu sorun kendini çözüyor.

**5. JVM heap tuning kritik.**
Default 256 MB heap ile 11 servis = 2.8 GB sadece heap. 96 MB'a düşürdüm, uygulama gayet çalışıyor. Micro-optimization öğrenmeden deploy yapmak tehlikeli.

**6. Correlation ID her yerde olmalı.**
Bir request'in 8 servisteki yolculuğunu izlemek için correlation ID şart. Gateway'de üretip header'da taşımak çok basit ama çok değerli.

**7. Hepsini bir arada görmek öğretiyor.**
Her pattern'i ayrı ayrı öğrenebilirsin ama birlikte çalıştıklarını görmek bambaşka bir deneyim. Cache invalidation'ın saga event'iyle nasıl tetiklendiğini, bir trace'in 8 servisi nasıl geçtiğini — bunları ancak end-to-end bir projede anlıyorsun.

---

## Tech Stack

| Katman | Teknoloji |
|--------|-----------|
| **Backend** | Java 21, Spring Boot 3.3, Spring Cloud Gateway |
| **Frontend** | React 18, TypeScript, Zustand, Tailwind CSS, Radix UI |
| **Database** | PostgreSQL 16, Redis 7, Elasticsearch 8.14 |
| **Messaging** | RabbitMQ 3.13 |
| **Security** | JWT (jjwt), BCrypt, Bucket4j |
| **Observability** | Prometheus, Grafana, Jaeger, Loki, OpenTelemetry |
| **Deploy** | Docker Compose, Nginx, Let's Encrypt, DigitalOcean |

---

*Sorularınız varsa yorumlarda yazın. Kodun tamamı GitHub'da açık — fork'layıp kendi projenize uyarlayabilirsiniz.*

**Etiketler:** #Microservices #SpringBoot #Java #Docker #SagaPattern #Redis #Elasticsearch #SystemDesign
