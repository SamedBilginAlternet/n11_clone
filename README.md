# JWT Refresh Token API

Spring Boot 3 ile geliştirilmiş, production-ready JWT kimlik doğrulama servisi.
Access Token + Refresh Token mimarisini, token rotation, rate limiting ve Flyway migrations ile birlikte uygular.

---

## Teknolojiler

| Katman | Teknoloji |
|--------|-----------|
| Framework | Spring Boot 3.3, Java 21 |
| Security | Spring Security 6, jjwt 0.12 |
| Veritabanı | PostgreSQL 16, Spring Data JPA |
| Migration | Flyway |
| Rate Limiting | Bucket4j |
| Dokümantasyon | SpringDoc OpenAPI 3 / Swagger UI |
| Monitoring | Spring Boot Actuator |
| Build | Maven |
| Container | Docker, Docker Compose |

---

## Mimari

```
src/main/java/com/example/jwtjava/
├── config/
│   ├── SecurityConfig.java           # Filter chain, CORS, stateless session
│   ├── OpenApiConfig.java            # Swagger UI + Bearer scheme
│   ├── CustomAuthEntryPoint.java     # JSON 401
│   ├── CustomAccessDeniedHandler.java# JSON 403
│   └── GlobalExceptionHandler.java   # Unified ErrorResponse for all errors
├── controller/
│   ├── AuthController.java           # /api/auth/**
│   └── UserController.java           # /api/users/**
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── RefreshRequest.java
│   ├── AuthResponse.java
│   └── ErrorResponse.java            # Consistent error envelope
├── entity/
│   ├── BaseEntity.java               # createdAt / updatedAt (JPA Auditing)
│   ├── User.java
│   ├── RefreshToken.java
│   └── Role.java
├── exception/
│   ├── AuthException.java            # Base — carries HttpStatus
│   ├── TokenException.java           # 401
│   ├── UserAlreadyExistsException.java # 409
│   └── ResourceNotFoundException.java  # 404
├── filter/
│   ├── JwtAuthFilter.java            # Per-request token validation
│   ├── RateLimitFilter.java          # Bucket4j — 10 req/min per IP on /api/auth/**
│   └── RequestLoggingFilter.java     # MDC correlation ID + request timing
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── service/
│   ├── JwtService.java               # Token generation & validation
│   ├── RefreshTokenService.java      # Create, validate, revoke
│   ├── AuthService.java              # register / login / refresh / logout
│   └── UserDetailsServiceImpl.java
└── validation/
    ├── StrongPassword.java           # Custom annotation
    └── StrongPasswordValidator.java  # Uppercase + digit + special char
```

---

## Token Akışı

```
POST /api/auth/register  ──►  Access Token (15 dk) + Refresh Token (7 gün)
POST /api/auth/login     ──►  Access Token (15 dk) + Refresh Token (7 gün)

GET  /api/users/me
  Authorization: Bearer <access_token>

POST /api/auth/refresh   ──►  Yeni Access Token + Yeni Refresh Token
  { "refreshToken": "..." }       (eski refresh token iptal edilir — rotation)

POST /api/auth/logout
  { "refreshToken": "..." }       (refresh token DB'den revoke edilir)
```

---

## API Endpoint'leri

| Method | Path | Auth | Açıklama |
|--------|------|------|----------|
| POST | `/api/auth/register` | — | Yeni kullanıcı kaydı |
| POST | `/api/auth/login` | — | Giriş |
| POST | `/api/auth/refresh` | — | Access token yenile |
| POST | `/api/auth/logout` | — | Oturumu kapat |
| GET | `/api/users/me` | Bearer | Mevcut kullanıcı |
| GET | `/api/users/admin` | Bearer + ADMIN | Admin endpoint |
| GET | `/actuator/health` | — | Uygulama sağlığı |
| GET | `/swagger-ui.html` | — | API dokümantasyonu |

---

## Hızlı Başlangıç

### Docker ile (önerilen)

```bash
docker-compose up --build
```

Uygulama `http://localhost:8080` adresinde çalışır.

### Lokal geliştirme

**Gereksinimler:** Java 21, Maven, PostgreSQL

```bash
# Veritabanı oluştur
psql -U postgres -c "CREATE DATABASE jwtjava;"

# Uygulamayı başlat (dev profili ile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/jwtjava` | Veritabanı URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB kullanıcısı |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB şifresi |
| `JWT_SECRET` | *(varsayılan key)* | **Production'da mutlaka değiştirin** |
| `JWT_ACCESS_TOKEN_EXPIRY` | `900000` | Access token süresi (ms) |
| `JWT_REFRESH_TOKEN_EXPIRY` | `604800000` | Refresh token süresi (ms) |

---

## Şifre Politikası

Kayıt sırasında şifre aşağıdaki kurallara uymalıdır:

- Minimum **8 karakter**
- En az **1 büyük harf**
- En az **1 rakam**
- En az **1 özel karakter** (`!@#$%` vb.)

---

## Rate Limiting

`/api/auth/**` endpoint'lerine IP başına **dakikada 10 istek** sınırı uygulanır.
Limit aşılırsa `429 Too Many Requests` döner ve `Retry-After: 60` header'ı eklenir.

---

## Testleri Çalıştır

```bash
./mvnw test
```

Kapsam:
- `JwtServiceTest` — token üretimi, doğrulama, expiry
- `AuthServiceTest` — register/login akışları, hata senaryoları
- `StrongPasswordValidatorTest` — şifre politikası kuralları

---

## Güvenlik Notları

- JWT secret **en az 256-bit** olmalı ve ortam değişkeninden okunmalıdır
- Refresh token'lar veritabanında saklanır ve her `/refresh` çağrısında **rotate** edilir
- Çalıntı refresh token tespit edilirse `revokeAllUserTokens` ile tüm token'lar iptal edilebilir
- Docker container'ı **root olmayan kullanıcı** ile çalışır
