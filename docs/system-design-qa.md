# n11 Clone — Sistem Tasarimi Soru & Cevap (100 Soru)

Bu belge, JwtJava (n11 Clone) projesinin sistem tasarimini anlamak isteyen junior gelistiriciler icin hazirlanmistir. Her soru detayli bir sekilde, projedeki gercek dosya yollari, sinif adlari ve kod ornekleriyle cevaplanmistir.

---

## Bolum 1: Java & OOP Temelleri (Sorular 1-25)

---

### Soru 1: Neden entity siniflarinda `private` alanlar kullanildi? `public` olsa ne olurdu? (Seviye: Baslangic)

Projede tum entity siniflarinda alanlar `private` olarak tanimlanmistir. Ornegin `services/auth-service/src/main/java/com/example/jwtjava/entity/User.java` dosyasindaki `User` sinifinda `private Long id`, `private String email`, `private String password` gibi alanlar vardir. Bu, OOP'nin **kapsulleme (encapsulation)** prensibinin dogrudan uygulamasidir. Kapsulleme, bir nesnenin ic durumunu dis dunyadan gizleyerek yalnizca kontrol edilmis yollarla (getter/setter) erisime izin verir.

Eger alanlar `public` olsaydi, herhangi bir sinif dogrudan `user.password = "123"` yazarak sifre alanini degistirebilirdi ve bu durumda sifrenin BCrypt ile hashlenmesi gibi is kurallarini atlayabilirdi. `private` alanlar sayesinde sifre degisikligi yalnizca `AuthService.register()` metodu uzerinden, `passwordEncoder.encode()` cagrisiyla gerceklesir. Ayni sekilde, `Order` sinifindaki `status` alani `private` oldugu icin durum gecisleri (`PENDING -> PAID`, `PENDING -> CANCELLED`) yalnizca `OrderService.markPaid()` ve `OrderService.markCancelled()` metodlari uzerinden yapilir; boylece gecersiz durum gecisleri onlenmis olur.

Lombok'un `@Getter` ve `@Setter` anotasyonlari bu alanlara disaridan erisim icin standart getter/setter metodlari uretir, ancak bu bile bilincsiz erisimi engellemez — sadece IDE ve derleme zamaninda kontrol saglar. `BaseEntity` sinifinda `@Getter` kullanilip `@Setter` kullanilmamasi, `createdAt` ve `updatedAt` alanlarinin disaridan degistirilmesini engellerken okunmasina izin verir.

---

### Soru 2: Neden `abstract class BaseEntity` kullanildi? Ne zaman `interface` tercih edilirdi? (Seviye: Orta)

`services/auth-service/src/main/java/com/example/jwtjava/entity/BaseEntity.java` dosyasindaki `BaseEntity` sinifi soyut (abstract) bir siniftir ve `createdAt` ile `updatedAt` audit alanlarini icerir. Bu sinif `@MappedSuperclass` ile isaretlenmistir, yani JPA bu sinifi bagimsiz bir tablo olarak degil, onu extend eden sinifin tablosuna eklenen alanlar olarak ele alir. `User`, `Order`, `Basket`, `RefreshToken` gibi entity'ler `BaseEntity`'yi extend ederek bu audit alanlarini otomatik olarak miras alir.

Abstract class seciminin sebebi, **ortak durum (state) paylasma** gereksinimi olmasidir. `BaseEntity` icerisinde `private Instant createdAt` ve `private Instant updatedAt` gibi somut alanlar ve `@CreatedDate`, `@LastModifiedDate` gibi JPA anotasyonlari vardir. Java'da interface'ler durum (instance field) tasimadigindan, bu tasarim interface ile mumkun olmazdi. Interface'ler yalnizca `default` metodlar ve sabitler icerebilir; `private Instant createdAt` gibi bir alan interface'e konulamaz.

Interface tercih edilecek durum ise: eger bir sinifin birden fazla bagimli davranisi miras almasi gerekseydi. Java'da coklu kalitim (multiple inheritance) yalnizca interface uzerinden mumkundur. Ornegin `User` sinifi hem `BaseEntity`'yi extend eder hem de `UserDetails` interface'ini implement eder. Eger `BaseEntity` bir interface olsaydi, `User` sinifi baska bir abstract class'i da extend edebilirdi — ama burada ortak alan paylasimi gerektiginden abstract class dogru secimdir.

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
```

`@EntityListeners(AuditingEntityListener.class)` sayesinde JPA, entity kaydedildiginde `createdAt`'i, guncellendiginde `updatedAt`'i otomatik olarak set eder. Bu davranisi her entity'de tekrar yazmak yerine tek bir yere koymak DRY (Don't Repeat Yourself) prensibini saglar.

---

### Soru 3: Neden DTO'lar icin Java `record` kullanildi? Normal siniftan farki nedir? (Seviye: Baslangic)

Projede tum DTO (Data Transfer Object) siniflarinda Java `record` yapisi kullanilmistir. Ornegin `services/auth-service/src/main/java/com/example/jwtjava/dto/RegisterRequest.java`:

```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @StrongPassword String password,
    @NotBlank String fullName
) {}
```

Record, Java 16 ile gelen ve **degismez (immutable) veri tasiyicilari** olusturmak icin tasarlanmis ozel bir sinif turudur. Bir record tanimladiginizda Java derleyicisi otomatik olarak su elemanlari uretir: tum alanlar icin `private final` field'lar, parametreli constructor, her alan icin getter metodu (`email()`, `password()`, `fullName()`), `equals()`, `hashCode()` ve `toString()` metodlari. Ayni seyi normal bir sinifla yapmak icin ~40 satir boilerplate kod yazmaniz gerekirdi.

Record'un en kritik ozelligi **immutability** saglamasidir. Alanlar `final` oldugu icin olusturulduktan sonra degistirilemez. Bu, DTO'lar icin muhtesem bir ozellik cunku: (1) bir RegisterRequest olusturulup controller'dan service'e gecirildikten sonra yanlislikla degistirilmesi mumkun degildir, (2) thread-safe'dir — bircok thread ayni DTO'yu guvenle okuyabilir, (3) niyeti acikca ifade eder — "bu nesne sadece veri tasiyor, is mantigi icerisinde degismeyecek".

Projede `ProductResponse`, `AuthResponse`, `OrderResponse`, `SearchResponse`, `CheckoutRequest`, `CheckoutItemRequest`, `CategoryResponse`, `ReviewStatsResponse` gibi onlarca record vardir. Hatta saga event'leri bile record olarak tanimlanmistir: `UserRegisteredEvent`, `OrderCreatedEvent`, `PaymentSucceededEvent` vb.

Normal bir sinif ise entity'lerde (orn. `User`, `Order`, `Product`) tercih edilmistir cunku JPA entity'leri degisebilir (mutable) olmalidir — Hibernate alanlarini proxy uzerinden degistirir ve bu degisiklikleri veritabanina yansitir.

---

### Soru 4: Entity siniflarinda neden `@Builder` pattern'i kullanildi? (Seviye: Baslangic)

Projede `User`, `Order`, `Product`, `Basket`, `RefreshToken`, `InventoryItem`, `Notification` gibi tum entity siniflarinda Lombok'un `@Builder` anotasyonu kullanilmistir. Builder pattern, bir nesneyi adim adim ve okunabilir bir sekilde olusturmanizi saglar.

`services/auth-service/src/main/java/com/example/jwtjava/service/AuthService.java` dosyasinda Builder'in kullanim ornegi:

```java
User user = User.builder()
    .email(request.email())
    .password(passwordEncoder.encode(request.password()))
    .fullName(request.fullName())
    .roles(EnumSet.of(Role.USER))
    .build();
```

Builder olmadan ayni nesneyi olusturmak icin ya cok parametreli bir constructor (`new User(null, email, password, fullName, roles)`) ya da bos constructor + setter zincirleri (`user.setEmail(...); user.setPassword(...);`) kullanmaniz gerekirdi. Cok parametreli constructor'da hangi parametrenin hangi alana karsilik geldigini anlamak cok zordur (ozellikle ayni tipteki parametreler yan yana geldiginde). Builder ise her alani adini belirterek set ettiginiz icin kod self-documenting olur.

Ek bir avantaj olarak, Builder pattern bazi alanlari opsiyonel birakmak icin idealdir. `Order.builder()` cagrisinda `failureReason` set edilmez cunku siparis olusturulurken henuz hata yoktur; sadece iptal durumunda `order.setFailureReason(reason)` ile set edilir. `@Builder.Default` ise varsayilan degerleri belirtmek icin kullanilir — ornegin `Order` sinifinda `@Builder.Default private List<OrderItem> items = new ArrayList<>()` ile items listesi her zaman bos bir liste olarak baslatilir, `null` olmaz.

---

### Soru 5: Neden `@RequiredArgsConstructor` kullanildi? `@Autowired` ile farki nedir? (Seviye: Orta)

Projede hemen hemen tum service, controller, filter ve listener siniflarinda `@RequiredArgsConstructor` kullanilmistir. Ornegin `services/order-service/src/main/java/com/example/order/service/OrderService.java`:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final SagaEventPublisher sagaEventPublisher;
    // ...
}
```

`@RequiredArgsConstructor` Lombok anotasyonudur ve siniftaki tum `final` alanlar icin bir constructor uretir. Spring, tek bir constructor goren bean'lerde `@Autowired` yazmadan otomatik olarak constructor injection uygular. Bu nedenle ayri bir `@Autowired` anotasyonuna gerek kalmaz.

Constructor injection, field injection'a (`@Autowired private OrderRepository orderRepository;`) su sebeplerden dolayi tercih edilir:

1. **Immutability**: Alanlar `final` oldugu icin nesne olusturulduktan sonra bagimliliklari degistirilemez. Bu, runtime'da birinin yanlislikla bir servise farkli bir repository atamasi gibi hatalari onler.
2. **Test edilebilirlik**: Birim testlerinde Spring context baslatmadan, dogrudan `new OrderService(mockRepo, mockPublisher)` yazarak nesneyi olusturabilirsiniz. Projede tum testler bu sekilde yazilmistir — `OrderServiceTest` sinifinda `@InjectMocks` ve `@Mock` anotasyonlari constructor injection uzerinden calisir.
3. **Fail-fast**: Eger bir bagimlilik eksikse, uygulama baslatiginda hemen hata verir. Field injection'da ise hata ancak ilgili alan kullanildiginda (lazy) ortaya cikar.
4. **NullPointerException onleme**: `final` alan bos birakilamazdi — derleyici constructor'da atanmasini zorunlu kilar.

---

### Soru 6: Dependency injection'da neden `final` alanlar kullanildi? (Seviye: Orta)

Projede tum service siniflarindaki bagimliliiklar `private final` olarak tanimlanmistir. Ornegin `AuthService` sinifindaki `private final UserRepository userRepository`. Bu secimin temeli **degismezlik (immutability)** ilkesidir.

`final` anahtar kelimesi, bir alanin yalnizca constructor'da atanabilecegini ve sonra asla degistirilemeyecegini garanti eder. Bu, uc onemli fayda saglar:

Birincisi, **thread safety**. Mikroservis ortaminda ayni servis instance'i birden fazla HTTP istegini ayni anda isler (her istek ayri bir thread'dir). Eger repository alani `final` olmasaydi, bir thread'in yanlislikla `this.orderRepository = null` yazmasi (veya reflection ile degistirmesi) tum diger thread'leri de etkilerdi. `final` alanlar bu riskleri tamamen ortadan kaldirir.

Ikincisi, **derleme zamani guvenceleri**. Eger constructor'da `orderRepository` alanini atamamisaniz, Java derleyicisi `Variable 'orderRepository' might not have been initialized` hatasi verir ve kod derlenmez bile. Bu, runtime NullPointerException'larini derleme zamanina ceker.

Ucuncusu, `final` alanlar `@RequiredArgsConstructor` ile tam uyumludur — Lombok yalnizca `final` olan alanlari constructor'a dahil eder. `final` olmayan alanlar (orn. `@Value("${jwt.secret}") private String secretKey` gibi konfigurasyonlar) constructor'a dahil edilmez ve Spring tarafindan ayri bir mekanizmayla (field injection veya setter injection) enjekte edilir.

---

### Soru 7: Neden `OrderStatus` icin `enum` kullanildi? (Seviye: Baslangic)

`services/order-service/src/main/java/com/example/order/entity/OrderStatus.java` dosyasi:

```java
public enum OrderStatus {
    PENDING,     // order created, awaiting payment
    PAID,        // payment succeeded, order confirmed
    CANCELLED,   // payment failed or user cancelled
    FAILED       // unexpected failure during saga
}
```

Enum, sinirli sayida sabit degeri temsil etmek icin Java'nin en uygun yapilarindan biridir. Siparis durumu yalnizca `PENDING`, `PAID`, `CANCELLED` veya `FAILED` olabilir — bu dort deger disinda bir sey olmasi mantiksal olarak mumkun degildir. Eger durumu `String` olarak saklasaydik, biri yanlislikla `"pending"` (kucuk harf), `"Pending"` veya `"PENDINGG"` (yazim hatasi) yazabilirdi ve bu tutarsizliklari tespit etmek cok zor olurdu.

Enum kullanmanin somut faydalari:

1. **Derleme zamani tip guvenceleri**: `order.setStatus("PENDINGG")` yazamazsiniz — derleyici hata verir. Yalnizca `order.setStatus(OrderStatus.PENDING)` gecerlidir.
2. **JPA uyumu**: `@Enumerated(EnumType.STRING)` anotasyonu ile enum degeri veritabaninda `VARCHAR` olarak saklanir (`"PENDING"`, `"PAID"` gibi). `EnumType.ORDINAL` kullanilsaydi sayisal indeks saklanirdi (0, 1, 2...) ve enum'a yeni bir deger eklemek mevcut verileri bozabilirdi.
3. **switch/case destegi**: PaymentListener, saga event'lerine gore farkli islemler yaptiginda type-safe karar agaci kurar.
4. **Dokumantasyon**: Her enum degerinin yanina yorum yazilabilir — kodun kendisi hangi durumlarin var oldugunu belgeler.

Ayni sekilde `NotificationType` (`WELCOME`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`, `SYSTEM`), `Role` (`USER`, `ADMIN`) ve `PaymentStatus` (`SUCCEEDED`, `FAILED`) enum'lari da projede kullanilmistir.

---

### Soru 8: Neden fiyatlar icin `BigDecimal` kullanildi? `double` neden uygun degil? (Seviye: Orta)

`services/product-service/src/main/java/com/example/product/entity/Product.java` sinifinda fiyat alani `private BigDecimal price` olarak tanimlanmistir. `Order` sinifindaki `totalAmount` ve `CheckoutItemRequest`'teki `productPrice` da BigDecimal'dir. Buna karsilik `rating` alani `double`'dir — aradaki fark bilerek yapilmistir.

`double` ve `float`, IEEE 754 kayan nokta standardini kullanir ve **bazi ondalik sayilari tam olarak temsil edemez**. Ornegin Java'da `0.1 + 0.2` ifadesi `0.30000000000000004` sonucunu verir — `0.3` degil. Bu, gunluk hesaplamalarda genellikle onemli degildir ama para hesaplamalarinda felaket olur. Bir e-ticaret uygulamasinda 10.000 islem sonunda 1 kurusluuk hatalar birikir ve muhasebe uyusmazliklari olusur.

`BigDecimal` ise keyfi hassasiyet (arbitrary precision) ile calisir ve `0.1 + 0.2` her zaman tam olarak `0.3` sonucunu verir. Projede indirimli fiyat hesabi `ProductResponse.from()` metodunda yapilir:

```java
BigDecimal discounted = p.getPrice()
    .multiply(BigDecimal.valueOf(100 - p.getDiscountPercentage()))
    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
```

Burada `RoundingMode.HALF_UP` ile yuvarlama kurali acikca belirlenir — kayan noktada gizli yuvarlama hatalari yoktur. Veritabaninda da `@Column(precision = 12, scale = 2)` ile 12 basamak toplam, 2 basamak ondalik olarak saklanir. `OrderService.checkout()` metodunda toplam tutar server tarafinda hesaplanir — "never trust client totals" prensibi uygulanir.

`rating` alani icin `double` kullanilmasinin sebebi ise rating'in parasal bir deger olmamasi ve kucuk yuvarlama hatalarinin kabul edilebilir olmasidir (4.7 vs 4.700000001 arasindaki fark kullanici acisinden gorulmez).

---

### Soru 9: `@Transactional` nedir ve neden `readOnly = true` kullanildi? (Seviye: Orta)

`services/product-service/src/main/java/com/example/product/service/ProductService.java` sinifi:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    // ...
}
```

`@Transactional` anotasyonu, bir metodun (veya tum sinifin) veritabani islemi sirasinda transaction (islem) icinde calismasini saglar. Transaction, ya tum degisikliklerin basarili olmasini ya da hicbirinin uygulanmamasini garanti eder (ACID prensibinin Atomicity kismi). Ornegin `InventoryService.reserve()` metodunda birden fazla urun icin stok azaltilir ve reservation kayitlari olusturulur — eger herhangi bir urun icin stok yetersizse `OutOfStockException` firlatilir ve transaction geri alinir (rollback), yani o ana kadar yapilmis tum degisiklikler iptal edilir.

`readOnly = true` ise Hibernate'e "bu transaction'da veri degistirmeyecegim" sinyali verir. Bu uc avantaj saglar:

1. **Performans**: Hibernate dirty-checking mekanizmasini devre disi birakir. Normalde Hibernate, transaction sonunda her yonetilen entity'nin degisip degismedigini kontrol eder (flush). `readOnly = true` oldugunda bu kontrol atlanir ve buyuk sonuc setlerinde onemli performans kazanci saglar.
2. **Veritabani optimizasyonu**: PostgreSQL gibi veritabanlari read-only transaction'lari farkli bir izolasyon seviyesinde calistirir ve okuma kilitleri uygulamaz.
3. **Niyet beyan**: Kodun "bu metot sadece okuma yapar" dedigini acikca ifade eder.

Sinif seviyesinde `@Transactional(readOnly = true)` yazildiginda tum metodlar varsayilan olarak salt okunurdur. `OrderService`'teki gibi yazma gerektiren metodlar ise `@Transactional` (readOnly yok, varsayilan false) ile override eder:

```java
@Transactional
public OrderResponse checkout(String userEmail, CheckoutRequest req) { ... }

@Transactional(readOnly = true)
public List<OrderResponse> listForUser(String userEmail) { ... }
```

---

### Soru 10: Repository'lerdeki `Optional` donus tipi neden kullanildi? (Seviye: Baslangic)

`services/product-service/src/main/java/com/example/product/repository/ProductRepository.java`:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
}
```

`Optional<T>`, Java 8 ile gelen ve "deger olabilir veya olmayabilir" durumunu acikca ifade eden bir wrapper sinifidir. `findBySlug("foo")` cagrisi sonucu urun varsa `Optional.of(product)`, yoksa `Optional.empty()` doner.

Optional kullanilmasaydik, metodun donus tipi `Product` olurdu ve sonuc bulunamadiginda ya `null` donecekti ya da exception firlatilacakti. `null` donen durum cok tehlikelidir cunku cagrici taraf `null` kontrolu yapmayi unutabilir ve `NullPointerException` alirsini — Java'nin en yaygin runtime hatasidir. Optional, derleme zamaninda "bu deger bos olabilir, ele alman gerekiyor" sinyali verir.

Projede Optional'in kullanimi su sekilde zincirlenebilir:

```java
public ProductResponse getBySlug(String slug) {
    return productRepository.findBySlug(slug)
        .map(ProductResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + slug));
}
```

Bu zincir su anlama gelir: "slug ile urun ara, bulursan ProductResponse'a donustur, bulamazsan 404 hatasi firlat." Ayni pattern `OrderService`, `AuthService`, `BasketService` gibi tum servislerde tekrarlanir. `Optional` olmadan ayni mantik cok daha verbose ve hataya acik bir if-null kontroluyle yazilirdi.

---

### Soru 11: `@Entity` ile `@Document` arasindaki fark nedir ve ne zaman hangisi kullanilir? (Seviye: Orta)

Projede iki farkli veri saklama katmani vardir ve her biri farkli anotasyonlar kullanir:

**`@Entity`** (JPA): PostgreSQL gibi iliskisel veritabanlarinda saklanan siniflar icin kullanilir. `User`, `Order`, `Product`, `Basket`, `Notification`, `RefreshToken`, `InventoryItem`, `PaymentTransaction`, `Review` — bunlarin hepsi `@Entity` ile isaretlidir. JPA (Java Persistence API) bu anotasyonu gorunde sinifi bir veritabani tablosuna eslestir (map eder) ve SQL sorguları uretir.

**`@Document`** (Spring Data Elasticsearch): Elasticsearch'te saklanan siniflar icin kullanilir. `services/search-service/src/main/java/com/example/search/document/ProductDocument.java`:

```java
@Document(indexName = "products")
public class ProductDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "turkish")
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;
    // ...
}
```

Temel farklar:
- `@Entity` SQL tablolariyla calisir (satirlar ve sutunlar), `@Document` JSON belgeleriyle calisir.
- `@Entity`'de `@Column(nullable = false)` gibi kisitlamalar kullanilir, `@Document`'ta `@Field(type = FieldType.Text, analyzer = "turkish")` gibi indeksleme yapilari kullanilir.
- `@Entity` repository'leri `JpaRepository`'yi extend eder, `@Document` repository'leri `ElasticsearchRepository`'yi extend eder.

Bu projede `ProductDocument`, `Product` entity'sinin **denormalize kopyasidir** — product-service'teki PostgreSQL tablosu source of truth, search-service'teki Elasticsearch index'i ise hizli arama icin bir kopyadir. `ProductIndexer` sinifi uygulama basladiginda product-service'ten urunleri cekip Elasticsearch'e indeksler.

---

### Soru 12: `@Column(nullable = false)` ile Java validation (`@NotBlank`) arasindaki fark nedir? (Seviye: Orta)

Bu ikisi farkli katmanlarda calisan farkli koruma mekanizmalaridir ve birbirinin yerine gecemez:

**`@Column(nullable = false)`**: Veritabani seviyesinde bir kisitlamadir. Flyway migration'lari bu kisitlamayi PostgreSQL tablosuna `NOT NULL` constraint olarak uygular. Eger herhangi bir sekilde (JPA disinda, dogrudan SQL ile veya baska bir uygulama ile) bu sutuna `NULL` yazilmaya calisilirsa, veritabani bunu reddeder. Bu, son savunma hatti — "ne olursa olsun bu alan bos olmayacak" garantisidir.

**`@NotBlank`, `@Email`, `@Min`**: Uygulama seviyesinde Bean Validation kisitlamalaridir. Controller'da `@Valid` anotasyonu ile tetiklenir ve istek veritabanina ulasmadan ONCE dogrulama yapar. Ornegin `RegisterRequest` sinifinda:

```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @StrongPassword String password,
    @NotBlank String fullName
) {}
```

Controller'da `@Valid @RequestBody RegisterRequest request` yazildiginda, Spring FrameWork istegi deserialize ettikten sonra ve metot cagrilmadan ONCE tum validation anotasyonlarini kontrol eder. Basarisiz olursa `MethodArgumentNotValidException` firlatir ve `GlobalExceptionHandler` bunu yakalar:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, ...) {
    Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(fe -> fe.getField(), fe -> fe.getDefaultMessage(), (a, b) -> a));
    ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Dogrulama hatasi", req);
    pd.setProperty("fields", fields);
    return problemResponse(HttpStatus.BAD_REQUEST, pd);
}
```

Iki katmanli koruma nedeni: validation kullaniciya anlamli hata mesajlari doner ("Sifre en az 8 karakter icermeli"), veritabani kisitlamasi ise programlama hatalarina karsi son koruma saglar. Ikisini birlikte kullanmak defense-in-depth prensibidir.

---

### Soru 13: Lombok nedir ve ne gibi kodlar uretir? (Seviye: Baslangic)

Lombok, Java'daki tekrar eden boilerplate kodlari derleme zamaninda otomatik olarak ureten bir kutuphanedir. Projede neredeyse her sinifta Lombok anotasyonlari kullanilir. Iste her anotasyonun ne ureltigi:

- **`@Getter`**: Her alan icin `public T getXxx()` metodu uretir. `BaseEntity` sinifinda kullanilir — `getCreatedAt()` ve `getUpdatedAt()` otomatik uretilir.
- **`@Setter`**: Her alan icin `public void setXxx(T value)` metodu uretir. Entity siniflarinda kullanilir.
- **`@Data`**: `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode` ve `@RequiredArgsConstructor`'un birlesimi. `User` ve `RefreshToken` siniflarinda kullanilir.
- **`@Builder`**: Builder pattern ile nesne olusturma kodu uretir.
- **`@NoArgsConstructor`**: Parametresiz constructor uretir — JPA bunu entity'leri olusturmak icin gerektirir.
- **`@AllArgsConstructor`**: Tum alanlar icin parametreli constructor uretir.
- **`@RequiredArgsConstructor`**: Yalnizca `final` alanlar icin constructor uretir — dependency injection icin kullanilir.
- **`@Slf4j`**: `private static final Logger log = LoggerFactory.getLogger(ClassName.class)` alanini uretir. Projede tum service ve listener siniflarinda kullanilir.
- **`@Builder.Default`**: Builder kullanildiginda alana varsayilan deger atanmasini saglar. Ornegin `@Builder.Default private Set<Role> roles = EnumSet.of(Role.USER)`.

Lombok olmadan `User` sinifi ~120 satir, Lombok ile ~30 satir. Bu, kodun okunabilirligini buyuk olcude arttirir cunku gercek is mantigi, boilerplate arasinda kaybolmaz.

---

### Soru 14: Saga event'lerinde neden `implements Serializable` kullanildi? (Seviye: Orta)

`services/order-service/src/main/java/com/example/order/saga/OrderCreatedEvent.java`:

```java
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String userEmail,
    BigDecimal totalAmount,
    List<Line> items
) implements Serializable {
    public record Line(Long productId, int quantity) implements Serializable {}
}
```

`Serializable` interface'i, bir Java nesnesinin byte dizisine donusturulup (serialization) geri olusturulabilecegini (deserialization) belirten bir isaretci (marker) interface'dir — hicbir metot tanimlamaz.

Saga event'leri RabbitMQ uzerinden servisler arasinda gonderilir. Spring AMQP, varsayilan olarak `SimpleMessageConverter` kullanir ve bu converter, Java nesnelerini RabbitMQ'ya gonderirken Java'nin yerlesik serializasyon mekanizmasini kullanabilir. Ancak projede asil kullanilan serializer `Jackson2JsonMessageConverter`'dir (SagaRabbitConfig siniflarinda tanimlanir) ve bu JSON'a donusturur — bu durumda Serializable teknik olarak zorunlu olmayabilir.

Yine de `Serializable` tutulmasinin sebepleri: (1) ileride mesaj broker degisikligi olursa veya fallback serializer kullanilirsa uyumluluk saglar, (2) event nesneleri JVM sinirlari disinda (disk cache, distributed cache, session replication) kullanilabilir, (3) Java best practice olarak agi veya diski gecebilecek nesnelerin Serializable olmasi onerilir. Bu, savunmaci programlama (defensive programming) yaklisimidir.

---

### Soru 15: Neden `ResponseStatusException` ile ozel exception siniflarinin ikisi de kullanildi? (Seviye: Orta)

Projede iki farkli hata yonetim stratejisi bir arada kullanilir:

**`ResponseStatusException`**: Basit, tek satirlik hatalar icin kullanilir. Ornegin `ProductService.getById()`:

```java
.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + id));
```

Bu, Spring'in yerlesik exception sinifidir ve dogrudan HTTP status kodu + mesaj tasir. Ayri bir exception sinifi olusturmayi gerektirmez.

**Ozel exception siniflarl**: Daha zengin hata bilgisi tasimasi gereken veya domain mantigi iceren durumlar icin kullanilir. auth-service'te su ozel exception'lar vardir:
- `AuthException`: Genel kimlik dogrulama hatasi, HTTP status kodu tasir.
- `UserAlreadyExistsException`: E-posta zaten kayitli oldugunda firlatilir (409 Conflict).
- `TokenException`: Refresh token gecersiz/suresi dolmus/iptal edilmis durumlarinda firlatilir.
- `ResourceNotFoundException`: Kaynak bulunamadii hatasi.

inventory-service'te ise `OutOfStockException` ozel bir exception'dir ve `productId` + `requested` bilgilerini tasir — bu bilgiler saga compensation akisinda kullanilir.

Neden ikisi bir arada? **Pragmatizm**. Basit CRUD servislerinde (product-service, review-service) ozel exception sinifi gereksiz fazlalik olur — `ResponseStatusException` yeterlidir. Ancak karmasik is mantigi olan servislerde (auth-service, inventory-service) ozel exception'lar domain bilgisini tasir ve `GlobalExceptionHandler` tarafindan farkli sekilde ele alinir.

---

### Soru 16: `@RestControllerAdvice` nedir ve `GlobalExceptionHandler` nasil calisir? (Seviye: Orta)

`services/auth-service/src/main/java/com/example/jwtjava/config/GlobalExceptionHandler.java` sinifi `@RestControllerAdvice` ile isaretlidir. Bu anotasyon, tum controller'lardan firlayan exception'lari merkezi bir noktada yakalamak icin kullanilir. Spring MVC, bir controller metodundan exception firladiginda, once o exception'in tipine uyan bir `@ExceptionHandler` metodu arar.

GlobalExceptionHandler sinifinda su handler'lar tanimlidir:

1. `handleAuth(AuthException)` — Domain-specific authentication hatalari
2. `handleBadCredentials(BadCredentialsException)` — Yanlis e-posta/sifre
3. `handleAccessDenied(AccessDeniedException)` — 403 Forbidden
4. `handleUnreadable(HttpMessageNotReadableException)` — Bozuk JSON body
5. `handleMethodNotAllowed(HttpRequestMethodNotSupportedException)` — Yanlis HTTP metodu
6. `handleNotFound(NoResourceFoundException)` — Endpoint bulunamadi
7. `handleValidation(MethodArgumentNotValidException)` — Bean validation hatalari (alan bazli hatalar `fields` property'sine eklenir)
8. `handleGeneric(Exception)` — Catch-all, beklenmeyen hatalar

Her handler, `ProblemDetail` nesnesi dondurererk RFC 7807 standardina uygun hata yaniti uretir. `build()` helper metodu `status`, `detail`, `instance` (istek URI'si) ve `timestamp` bilgilerini set eder. `problemResponse()` metodu ise `Content-Type: application/problem+json` header'iyla yaniti doner.

Bu merkezi yaklasimin faydasi: her controller'da ayri ayri try-catch yazmak yerine, hata yonetimi tek bir yerde tanimlanir. Yeni bir exception tipi eklendiginde sadece bu sinifa bir handler eklenmesi yeterlidir. Tum projede tutarli hata formati garanti edilir.

---

### Soru 17: `ProblemDetail` (RFC 7807) nedir ve neden kullanildi? (Seviye: Orta)

RFC 7807, HTTP API'lerinde hata yanitlarinin standart bir formatta donmesini tanimlayan bir Internet standartdir. Projede tum servislerin `GlobalExceptionHandler`'lari bu standarti kullanir.

Bir RFC 7807 yaniti su JSON yapisina sahiptir:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dogrulama hatasi",
  "instance": "/api/auth/register",
  "timestamp": "2026-04-23T10:15:30Z",
  "fields": {
    "email": "Gecerli bir e-posta adresi giriniz",
    "password": "Sifre en az 8 karakter icermelidir"
  }
}
```

Neden standart bir format? Cunku:

1. **Frontend tutarliligi**: `frontend/src/shared/api/client.ts` dosyasindaki `apiFetch` fonksiyonu, hata yanitlarini `parseProblem()` ile parse eder ve `ApiError` nesnesine donusturur. `frontend/src/shared/api/problem.ts` dosyasindaki `errorMessage()` ve `errorFields()` yardimci fonksiyonlari bu standart yapiyi okur. Eger her servis farkli hata formati donseydi, frontend'te servis basina farkli hata isleme mantigi yazmak gerekirdi.
2. **Content-Type**: `application/problem+json` MIME tipi, istemcilere bu yanitin bir hata belgesi oldugunu bildirir.
3. **Genisletilebilirlik**: `fields` (validasyon hatalari), `retryAfterSeconds` (rate limit) gibi ozel alanlar eklenebilir. RateLimitFilter, `ProblemDetail` nesnesine `retryAfterSeconds` property'si ekler.
4. **Birikte calisabilirlik**: RFC 7807 herhangi bir dilde veya framework'te parse edilebilir — JavaScript, Python, Go gibi istemciler bu formati anlayabilir.

Spring 6 ile birlikte `ProblemDetail` sinifi framework'e dahil oldu ve bu standardin uygulanmasini kolaylastirdi.

---

### Soru 18: `@Valid` nedir ve bean validation nasil calisir? (Seviye: Baslangic)

`@Valid` anotasyonu, bir istek geldikten sonra, controller metodu cagrilmadan ONCE istek nesnesinin tum validation kurallarini kontrol etmesini tetikler. `services/auth-service/src/main/java/com/example/jwtjava/controller/AuthController.java`:

```java
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, ...) {
```

Bu satirda `@RequestBody` JSON body'yi `RegisterRequest` record'una deserialize eder, ardindan `@Valid` devreye girer ve `RegisterRequest` icerisindeki tum validation anotasyonlarini kontrol eder:

- `@NotBlank` — alan bos veya sadece bosluk olmamal
- `@Email` — gecerli e-posta formati kontrol edilir
- `@StrongPassword` — projede ozel olarak yazilmis annotation, `StrongPasswordValidator` sinifini calistirir

`StrongPasswordValidator` (`services/auth-service/src/main/java/com/example/jwtjava/validation/StrongPasswordValidator.java`) regex ile sifre gucunu kontrol eder: en az 8 karakter, buyuk harf, kucuk harf, rakam ve ozel karakter.

Validation basarisiz olursa Spring otomatik olarak `MethodArgumentNotValidException` firlatir. `GlobalExceptionHandler` bu exception'i yakalar ve alan bazli hatalari RFC 7807 formatinda doner. Ornegin e-posta gecersizse ve sifre kisaysa:

```json
{
  "status": 400,
  "detail": "Dogrulama hatasi",
  "fields": {
    "email": "must be a well-formed email address",
    "password": "Sifre en az 8 karakter, bir buyuk harf, bir rakam ve bir ozel karakter icermelidir."
  }
}
```

Bu, validation mantigi controller veya service'te if-else zincirleriyle yazilmak yerine deklaratif (anotasyonlarla) tanimlanir — kodun okunabilirligini ve bakimini kolaylastirir.

---

### Soru 19: `@PageableDefault` neden kullanildi? (Seviye: Baslangic)

`services/product-service/src/main/java/com/example/product/controller/ProductController.java`:

```java
@GetMapping
public Page<ProductResponse> list(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String q,
    @PageableDefault(size = 24) Pageable pageable
) {
    return productService.list(category, q, pageable);
}
```

`@PageableDefault(size = 24)` anotasyonu, istemci sayfalama parametresi gondermedigi durumda varsayilan olarak sayfa boyutunu 24 olarak set eder. Spring Data, URL'deki `?page=0&size=24&sort=name,asc` gibi parametreleri otomatik olarak `Pageable` nesnesine donusturur.

Neden 24? Cunku frontend'teki urun grid'i 4 sutunlu bir layout kullanir ve 24 urun tam 6 satir yapar — gorsel olarak dengeli bir sayfa olusturur. Varsayilan deger olmadanda, Spring Data varsayilan olarak `size=20` kullanirdi ki bu 4'e tam bolunmez.

`Pageable` sayesinde veritabaninden yalnizca istenen sayfa kadar veri cekilir — 200 urununuz varken `LIMIT 24 OFFSET 0` sorgusu calisir, 200 urun'un tamamini cekmek yerine. Bu, buyuk veri setlerinde performans ve bellek acsindan kritik oneme sahiptir. `Page<T>` donusu ise toplam eleman sayisi (`totalElements`), toplam sayfa sayisi (`totalPages`) ve mevcut icerik (`content`) bilgilerini tasir — frontend bu bilgilerle sayfalama kontrollerini olusturur.

---

### Soru 20: `@Cacheable` arka planda ne yapar? (Seviye: Ileri)

`services/product-service/src/main/java/com/example/product/service/ProductService.java`:

```java
@Cacheable(value = "products:byId", key = "#id")
public ProductResponse getById(Long id) {
    return productRepository.findById(id)
        .map(ProductResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + id));
}
```

`@Cacheable` anotasyonu Spring'in AOP (Aspect-Oriented Programming) mekanizmasini kullanir. Spring, `ProductService` sinifinin etrafina bir **proxy** sarar. Bu proxy, `getById()` cagrildikinda su adimlari izler:

1. Oncelikle cache'te `products:byId` isimli cache'in `#id` anahtarinda bir deger var mi kontrol eder.
2. Varsa, **metodu cagirmadan** cache'teki degeri doner — veritabani sorgusu calistirirmaz.
3. Yoksa, gercek `getById()` metodunu calistirir, sonucu cache'e yazar ve sonra doner.

Bu projede cache backend'i Redis'tir. `services/product-service/src/main/java/com/example/product/config/RedisConfig.java` sinifinda cache yapilandirmasi tanimlidir:

```java
return RedisCacheManager.builder(cf)
    .cacheDefaults(defaults)
    .withCacheConfiguration("products:byId", defaults.entryTtl(Duration.ofMinutes(30)))
    .withCacheConfiguration("products:bySlug", defaults.entryTtl(Duration.ofMinutes(30)))
    .withCacheConfiguration("products:categories", defaults.entryTtl(Duration.ofHours(24)))
    .build();
```

Her cache'in farkli TTL (Time-To-Live) suresi vardir: urun detayi 30 dakika, kategoriler 24 saat. `GenericJackson2JsonRedisSerializer` kullanilarak veriler Redis'te JSON formatinda saklanir — bu, Redis CLI ile debug yaparken okunabilirlik saglar.

`CacheConfig` sinifi ise cache hatalari icin tolerans saglar — Redis baglantisi kesilse bile uygulama calismaya devam eder, sadece veritabanindan okur:

```java
public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
    log.warn("Cache GET failed [cache={}, key={}]: {}", cache.getName(), key, e.getMessage());
}
```

---

### Soru 21: `List.of()` neden degismez (immutable) listeler icin kullanildi? (Seviye: Baslangic)

`services/product-service/src/main/java/com/example/product/service/ProductService.java`:

```java
private static final List<CategoryResponse> CATEGORIES = List.of(
    new CategoryResponse("elektronik", "Elektronik", "..."),
    new CategoryResponse("moda", "Moda", "..."),
    // ... 8 kategori
);
```

`List.of()`, Java 9 ile gelen ve degismez (unmodifiable) bir liste olusturan factory metodudur. Bu listeye `add()`, `remove()` veya `set()` cagrisi yapildiginda `UnsupportedOperationException` firlatilir.

Neden degismez liste? Cunku kategoriler uygulama boyunca sabittir ve degismemelidir. `CATEGORIES` alani `private static final` olarak tanimlandiginda, sinif yuklendiginde bir kez olusturulur ve tum istekler ayni listeyi paylasilr. Eger bu liste degisebilir (`new ArrayList<>()`) olsaydi, bir istegin islem sirasinda listeyi degistirmesi diger tum istekleri etkileyebilirdi — bu, cok thread'li ortamda tehlikeli bir yaristir.

`List.of()` ayrica `null` eleman kabul etmez — `List.of(null)` yazarsaniz `NullPointerException` alirsaniz. Bu, beklenmeleyen `null` degerlerin listeye girmesini engeller. JwtService sinifindaki `extractAuthorities()` metodunda da `List.of()` kullanilir: `if (roles == null) return List.of();` — bos ama `null` olmayan bir liste doner.

Alternatif olarak `Collections.unmodifiableList(new ArrayList<>(...))` kullanilabilirdi ancak `List.of()` cok daha kisa ve performanslidir — dahili olarak optimize edilmis immutable list implementasyonlari kullanir.

---

### Soru 22: Method reference (`ProductResponse::from`) nedir? (Seviye: Baslangic)

`services/product-service/src/main/java/com/example/product/service/ProductService.java`:

```java
return page.map(ProductResponse::from);
```

Method reference, Java 8 ile gelen ve lambda ifadelerinin kisaltilmis halidir. `ProductResponse::from` ifadesi, `product -> ProductResponse.from(product)` lambda'sinin kisaltmasidir. Ikisi de ayni seyi yapar: her `Product` nesnesini alip `ProductResponse.from()` statik metoduna gecirirerek donusturur.

`ProductResponse.from()` metodu (`services/product-service/src/main/java/com/example/product/dto/ProductResponse.java`) bir Product entity'sini alir ve tum alanlarini kopyalayarak yeni bir ProductResponse record'u olusturur. Ek olarak indirimli fiyati hesaplar:

```java
public static ProductResponse from(Product p) {
    BigDecimal discounted = p.getPrice()
        .multiply(BigDecimal.valueOf(100 - p.getDiscountPercentage()))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    return new ProductResponse(p.getId(), p.getName(), ...);
}
```

Method reference'in avantajlari: (1) daha kisa ve okunabilir, (2) IDE'de tiklayarak dogrudan hedef metoda gidebilirsiniz, (3) fonksiyonel programlama stiline uygun — map, filter, reduce gibi stream operasyonlarinda cok kullanilir. Projede `OrderResponse::from`, `NotificationResponse::from` gibi pek cok yerde ayni pattern tekrarlanir.

---

### Soru 23: Neden `Page<T>.map()` yerine stream kullanilmadi? (Seviye: Orta)

```java
return page.map(ProductResponse::from);
```

Bu satirda `page` bir `Page<Product>` nesnesidir ve `map()` metodu her elemanit donusturerek yeni bir `Page<ProductResponse>` doner. Bu, Java Stream'in `map()` metoduyla benzer ama onemli bir farkla: **sayfalama meta verisini (totalElements, totalPages, number, size) korur**.

Eger stream kullanilsaydi:

```java
List<ProductResponse> content = page.getContent().stream()
    .map(ProductResponse::from)
    .toList();
```

Bu, yalnizca icerik listesini donusturur ama `totalElements`, `totalPages` gibi meta verileri kaybeder. Bu bilgiler frontend'in sayfalama kontrollerini olusturmasi icin kritiktir. Bu kaybi onlemek icin elle yeni bir `PageImpl` olusturmaniz gerekirdi:

```java
return new PageImpl<>(content, pageable, page.getTotalElements());
```

Bu gereksiz boilerplate. `Page.map()` hem donusturme hem meta veri koruma isini tek satirda yapar. `OrderService.listForUser()` metodunda ise Stream kullanilir cunku donus tipi `List<OrderResponse>`'dir — sayfalama yok, sadece donusum var:

```java
return orderRepository.findByUserEmailOrderByIdDesc(userEmail).stream()
    .map(OrderResponse::from)
    .toList();
```

Kural basit: sayfalama varsa `Page.map()`, yoksa `Stream.map()`.

---

### Soru 24: `@EventListener(ApplicationReadyEvent.class)` ne zaman tetiklenir? (Seviye: Orta)

`services/search-service/src/main/java/com/example/search/service/ProductIndexer.java`:

```java
@EventListener(ApplicationReadyEvent.class)
public void indexOnStartup() {
    log.info("Starting Elasticsearch product index bootstrap from {}", productServiceBaseUrl);
    try {
        indexAll();
    } catch (Exception ex) {
        log.error("Initial index failed; search will be empty until /api/search/reindex is hit", ex);
    }
}
```

`ApplicationReadyEvent`, Spring Boot uygulamasinin tamamen basladigini, tum bean'lerin olusturuldugunu, tum endpoint'lerin hazir oldugunu ve uygulamanin istekleri kabul etmeye basladigini belirten bir olay (event)'dir. Bu event, uygulama yasam dongusundeki son event'tir.

Neden `@PostConstruct` degil de `ApplicationReadyEvent`? Cunku `@PostConstruct` bean olusturulduktan hemen sonra calisir ama bu noktada diger bean'ler henuz hazir olmayabilir — ozellikle `WebClient` veya Eureka kaydi gibi ag bagimli islemler henuz tamamlanmamis olabilir. `ApplicationReadyEvent` ise her sey hazir olduktan sonra calisir.

Ayni projede `DemoUserSeeder` sinifi ise `CommandLineRunner` interface'ini implement eder — bu da benzer bir amaca hizmet eder ve uygulama baslatildiginda `run()` metodunu calistirir. Aradaki fark: `CommandLineRunner` biraz daha erken calisir ve komut satiri argumanlarini alir; `ApplicationReadyEvent` ise tum `Runner`'lardan sonra tetiklenir.

`indexOnStartup()` metodunda exception yakalanip loglanan ama tekrar firlatilmayan dikkat ceker. Bunun sebebi: product-service henuz hazir olmayabilir (docker-compose sirasinda), bu durumda arama servisi bos baslar ama crash etmez. Kullanici daha sonra `POST /api/search/reindex` ile manuel tetikleme yapabilir.

---

### Soru 25: `@SuppressWarnings` neden bazen kullanilir? (Seviye: Baslangic)

`services/inventory-service/src/main/java/com/example/inventory/saga/InventoryListener.java`:

```java
@SuppressWarnings("unchecked")
List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");
```

`@SuppressWarnings("unchecked")`, Java derleyicisinin "unchecked cast" uyarisini susturmak icin kullanilir. Bu uyari, generik tiplerde tip guvenliginin derleme zamaninda dogrulanamadigi durumlarda ortaya cikar.

Burada ozel bir durum vardir: RabbitMQ uzerinden gelen event'ler JSON olarak deserialize edilir ve Java tarafinda `Map<String, Object>` olarak alinir. JSON'daki bir dizi (`items`), Java tarafinda `List<Map<String, Object>>` olarak cast edilir ama Java'nin type erasure mekanizmasi nedeniyle derleme zamaninda bu donusumun guvenli oldugu dogrulanamaz.

`@SuppressWarnings` kullanmak, "bu cast'in guvenli oldugunu biliyorum, derleyici uyarisi gereksiz" demektir. Bu, iki farkli servisin Java siniflarini paylasmadigi (independently deployable) mikroservis mimarisinin bir sonucudur. inventory-service, order-service'in `OrderCreatedEvent` sinifini import etmez — bunun yerine genel bir `Map<String, Object>` olarak alir. Bu, servislerin birbirinden bagimsiz deploy edilebilmesini saglar ama tip guvenligini runtime'a birakir.

Alternatif yaklasim, her serviste ayni event sinifinin bir kopyasini tutmak olurdu (projede auth-service ve basket-service arasinda `UserRegisteredEvent` boyle calisir). Ancak inventory-service, farkli bir strateji olarak `Map` ile calismayi secmistir — bu, event schema degisikliklerine karsi daha esnektir.

---

## Bolum 2: Servis Tasarimi & Mikroservis Mimarisi (Sorular 26-50)

---

### Soru 26: Bu proje icin neden monolit yerine mikroservisler secildi? (Seviye: Baslangic)

Bu proje 10 mikroservisten olusur: auth-service, basket-service, product-service, order-service, payment-service, notification-service, review-service, search-service, inventory-service ve eureka-server. Her biri bagimsiz bir Spring Boot uygulamasi, kendi veritabani ve kendi Dockerfile'ina sahiptir.

Mikroservis mimarisinin bu proje icin secilme sebepleri:

1. **Bagimsiz deploy**: Urun katalogu guncellenmesi gerektiginde sadece product-service yeniden baslatilir — diger servisler etkilenmez. Monolit'te her degisiklik tum uygulamanin yeniden deploy edilmesini gerektirir.
2. **Teknoloji cesitliligi**: product-service Redis cache kullanirken, search-service Elasticsearch kullanir, notification-service WebSocket destegi sunar. Her servis kendi ihtiyacina uygun teknoloji secer.
3. **Hata izolasyonu**: payment-service crash etse bile product-service ve search-service calismaya devam eder. Monolit'te tek bir out-of-memory hatasi tum uygulamayi durdurur.
4. **Olceklenebilirlik**: Black Friday'da siparis yukselirse sadece order-service ve payment-service instance'lari arttirilir — tum uygulamayi coklama gerekli degildir.
5. **Takim organizasyonu**: Her servis kucuk bir takim tarafindan sahiplenilebilir, bagimsiz develop ve test edilebilir.

Ancak, mikroservisler karmasiklik getirir: dagitik transaction'lar (saga pattern), servisler arasi iletisim (RabbitMQ), service discovery (Eureka), merkezi giris noktasi (API Gateway), dagitik loglama (Loki), dagitik tracing (Jaeger). Bu proje, tum bu karmasikliklari ogretmek amaciyla tasarlanmistir.

---

### Soru 27: Neden her servis icin ayri veritabani kullanildi? (Seviye: Orta)

docker-compose.yml'de tek bir PostgreSQL instance'i calisir ama 8 ayri veritabani olusturulur: `authdb`, `basketdb`, `productdb`, `orderdb`, `paymentdb`, `notificationdb`, `reviewdb`, `inventorydb`. Bu, **Database per Service** pattern'idir.

Her servisin kendi veritabanina sahip olmasinin sebepleri:

1. **Gevrek baglanma (loose coupling)**: auth-service `users` tablosuna sahipken, order-service `orders` tablosuna sahiptir. Eger ortak bir veritabani kullanilsaydi, bir servisin schema degisikligi diger servisleri kiracakti. Bagimsiz veritabanlariyla her servis kendi Flyway migration'larini bagimsiz olarak calistirir.
2. **Bagimssiz olcekleme**: Urun sorgusu agir yuk altindaysa sadece productdb buyutulur.
3. **Teknoloji ozgurlugu**: search-service SQL veritabani yerine Elasticsearch kullanir — bu karar diger servisleri hic etkilemez.
4. **Hata izolasyonu**: Bir veritabanindaki kilit (lock) veya performans sorunu diger servislerin veritabanlarini etkilemez.

Dezavantaji ise: servisler arasi veri tutarliligi veritabani transaction'lari ile saglanamaz. Ornegin, siparis olusturulurken stogun ayrilmasi ve odemenin alinmasi farkli veritabanlarinda gerceklesir. Bu sorunu cozmek icin **Saga pattern** kullanilir — RabbitMQ uzerinden koreografi tabanli dagitik transaction yonetimi yapilir.

Docker-compose'daki postgres servisi, `entrypoint` scriptinde `CREATE DATABASE basketdb || true` gibi komutlarla tum veritabanlarini olusturur. `|| true` kismni, veritabani zaten varsa hatayi yutarak idempotent calismayi saglar.

---

### Soru 28: API Gateway nasil calisir ve neden gereklidir? (Seviye: Orta)

`services/api-gateway` dizini, Spring Cloud Gateway tabanli API Gateway'i icerir. `application.yml` dosyasinda rota tanimlari vardir:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**,/api/users/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
```

Gateway, tum disaridan gelen isteklerin tek giris noktasidir (port 8000). Tarayici `/api/products/1` istegi gonderdiginde, gateway bu istegi `lb://product-service` adresine yonlendirir. `lb://` oneki, Eureka service discovery uzerinden product-service'in gercek IP adresini bulmayi saglar.

Gateway neden gereklidir:

1. **Tek giris noktasi**: Frontend yalnizca tek bir adres bilir (localhost:8000). 10 servisin her birinin portunu bilmesi gerekmez.
2. **CORS yonetimi**: Gateway, global CORS yapilandirmasini tek bir yerde tanimlar — her servisin ayri ayri CORS ayari yapmasina gerek kalmaz.
3. **Guvenlik sinirlama**: Mikroservisler Docker agi disina expose edilmez (`expose: "8080"` vs `ports: "8080:8080"`). Yalnizca gateway'in portu (`18000`) disariya aciktir.
4. **Cross-cutting concerns**: Loglama, tracing header'larinin propagasyonu gibi islemler gateway seviyesinde yapilir.
5. **Yuk dengeleme**: Eureka'dan alinan birden fazla instance arasinda load balancing yapilir.

---

### Soru 29: Neden nginx reverse proxy yerine Spring Cloud Gateway secildi? (Seviye: Ileri)

Bu projede hem Spring Cloud Gateway (API gateway olarak) hem de nginx (frontend icin) kullanilir, ancak farkli amaclarla. Mikroservisler arasindaki yonlendirme icin Spring Cloud Gateway secilmistir.

Spring Cloud Gateway'in nginx'e gore avantajlari:

1. **Service Discovery entegrasyonu**: `lb://auth-service` yazilayi, Eureka'dan canli servis listesini alir. nginx'te upstream adresleri statik olarak yazilmali veya ek bir cozum (Consul-template gibi) kullanilmalidir.
2. **Java ekosistemi uyumu**: Gateway, Spring Boot Actuator ile health endpoint'leri acar, Micrometer ile metrikler toplar ve OpenTelemetry ile trace'lere katilir. nginx icin bu entegrasyonlar ayri eklentiler gerektirir.
3. **Programatik filtreleme**: `RequestLoggingGlobalFilter` gibi Java ile yazilmis filtreler eklenebilir — correlation ID propagasyonu, istek loglama gibi islemler Java kodu ile kolayca yapilir.
4. **Reactive stack**: Spring Cloud Gateway, Project Reactor uzerine insa edilmistir ve non-blocking I/O kullanir — yuksek es zamanli baglanti sayisinda performanslidir.

nginx ise frontend'te kullanilir cunku: (1) statik dosya servisi icin optimize edilmistir (gzip, cache header'lari), (2) SPA fallback (`try_files $uri $uri/ /index.html`) icin idealdir, (3) ayni origin uzerinden API proxy'si yapar ve CORS preflight isteklerini ortadan kaldirir.

---

### Soru 30: Saga pattern nedir ve neden orchestration yerine choreography secildi? (Seviye: Ileri)

Saga pattern, mikroservis mimarisinde dagitik transaction'lari yonetmek icin kullanilir. Geleneksel ACID transaction'lari tek bir veritabaninda calisir, ancak her servisin kendi veritabani oldugu bir mimaride bu mumkun degildir. Saga, uzun sureli bir is islemini, her biri lokal bir transaction olan kucuk adimlara boler ve bir adim basarisiz olursa onceki adimlarin etkisini geri alan telafi (compensation) islemleri calistirir.

Iki saga yaklanimi vardir:

**Orchestration**: Merkezi bir "saga orchestrator" servisi, adimlari sirasiyla calistirir ve hata durumunda compensation'lari tetikler. Avantaji: akis tek bir yerde gorulur. Dezavantaji: orchestrator tek nokta arizasidir (single point of failure) ve merkezi karar vericiye bagimlilik olusur.

**Choreography**: Her servis, bir onceki servisin event'ini dinler ve kendi isini yaptiktan sonra bir sonraki event'i yayinlar. Merkezi bir koordinator yoktur. Bu projede choreography secilmistir.

CheckoutSaga akisi:
1. `order-service` siparisi kaydeder ve `order.created` event'ini yayinlar
2. `inventory-service` bu event'i dinler, stok ayirir, `inventory.reserved` yayinlar
3. `payment-service` odemeyi isler, `payment.succeeded` veya `payment.failed` yayinlar
4. `order-service` sonuca gore siparisi gunceller, `order.confirmed` veya `order.cancelled` yayinlar
5. `basket-service` ve `notification-service` son event'leri dinler

Choreography secilmesinin sebepleri:
1. **Tek nokta arizasi yok**: Orchestrator crash etse tum saga durur; choreography'de her servis bagimsiz calisir.
2. **Gevrek baglanma**: Servisler birbirini bilmez, sadece event'leri bilir. Yeni bir servis (orn. loyalty-service) eklenmesi, mevcut servisleri degistirmez.
3. **Basitlik**: Bu proje icin 5-6 adimlik saga choreography ile kolayca yonetilebilir. 20+ adimlik karmasik islemlerde orchestration daha okunabilir olurdu.

---

### Soru 31: JWT kimlik dogrulama servisler arasinda nasil calisir? (Seviye: Orta)

Bu projede tum servisler (auth, basket, order, payment, review, notification) ayni JWT secret'i payrasir. JWT (JSON Web Token) kimlik dogrulama akisi su sekilde calisir:

1. Kullanici `/api/auth/login` ile giris yapar, auth-service JWT access token uretir ve doner.
2. Frontend bu token'i `localStorage`'da saklar (`n11-auth` anahtari altinda, Zustand persist middleware ile).
3. Her API isteginde frontend `Authorization: Bearer <token>` header'i ekler.
4. Hedef servisin (orn. basket-service) `JwtAuthFilter`'i bu header'i okur, JWT'yi cozumler ve `SecurityContext`'e kimlik bilgilerini yerlestirir.

Kritik nokta: **her servisin kendi `JwtAuthFilter` ve `JwtService` sinifi vardir** — servisler, auth-service'e geri cagrida bulunmaz. JWT'nin icindeki `sub` (e-posta) ve `roles` (yetkiler) claim'leri dogrudan token'dan okunur:

```java
// JwtAuthFilter.java
String username = jwtService.extractUsername(token);
List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(username, null, authorities);
SecurityContextHolder.getContext().setAuthentication(authToken);
```

Bu yaklasim **tamamen stateless** kimlik dogrulamadir — servislerin oturum bilgisi saklamasina veya auth-service'e sorgu gondermeye gerek yoktur. Token'in gecerliligi (`iat`, `exp`) ve imzasi (`HMAC-SHA256`) her serviste yerel olarak dogrulanir.

docker-compose.yml'de tum servislere ayni `JWT_SECRET` ortam degiskeni gecilir:
```yaml
JWT_SECRET: ${JWT_SECRET:-3f6a2b8c1d4e5f7a9b0c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a}
```

---

### Soru 32: Neden Keycloak gibi bir auth server yerine payrasilan JWT secret kullanildi? (Seviye: Ileri)

Bu projede bagimsiz bir identity provider (Keycloak, Auth0) yerine, tum servislerin ayni symmetric JWT secret ile calistigi bir yaklasim secilmistir.

Bu secimin sebepleri:

1. **Basitlik**: Keycloak, en az 512 MB RAM gerektiren agir bir Java uygulamasidir. Bu proje zaten 10 mikroservis, RabbitMQ, PostgreSQL, Elasticsearch, Redis, Prometheus, Grafana, Jaeger, Loki ve Promtail calistirmaktadir — toplam ~20 container. Keycloak eklemek kaynak tuketimini daha da artirir.
2. **Ogretici amac**: Bu proje, JWT'nin nasil calistigini OGRETMEK icin tasarlanmistir. `JwtService.generateAccessToken()`, `buildToken()`, `extractClaim()` gibi metodlar JWT uretiminin ve dogrulanmasinin nasil gerceklestigini gosterir. Keycloak kullanilsaydi bu detaylar soyutlanmis olurdu.
3. **Yayin odaklari**: Projenin asil odagi saga pattern, event-driven architecture ve observability'dir — kimlik yonetimi tamamlayici bir bilesendir.

Production dezavantajlari:
- Symmetric secret (HMAC) kullanildigi icin, herhangi bir servisin compromise olmasi tum servislerin guvenligini tehlikeye atar.
- Token revocation yalnizca refresh token'lar icin vardir; access token'lar sureleri dolana kadar gecerlidir.
- SSO, MFA, social login gibi ozellikler yoktur.

Production'da asymmetric key pair (RSA/EC) ile auth-service'in token imzalamasi, diger servislerin yalnizca public key ile dogrulama yapmassi veya Keycloak/Auth0 entegrasyonu onerilir.

---

### Soru 33: CheckoutSaga basarisizliklari nasil yonetir? (Seviye: Ileri)

CheckoutSaga uc acik basarisizlik senaryosuna sahiptir ve her biri icin compensation (telafi) mekanizmasi vardir:

**Senaryo 1: Stok yetersiz**
1. `order-service`: siparis PENDING olarak kaydedilir, `order.created` yayinlanir.
2. `inventory-service`: stok kontrol eder, yetersizse `inventory.out-of-stock` yayinlar.
3. `order-service`: `InventoryOutOfStockListener` event'i dinler, siparisi CANCELLED yapar, `order.cancelled` yayinlar.
4. Odeme hic baslamadigi icin geri alinacak bir sey yoktur, sadece bildirim gonderilir.

**Senaryo 2: Odeme reddedildi**
1. Stok basariyla ayrildiktan sonra `payment-service` odemeyi reddeder (e-posta "fail" iceriyorsa veya tutar > 100.000 TRY).
2. `payment.failed` event'i yayinlanir.
3. `order-service`: siparisi CANCELLED yapar, `order.cancelled` yayinlar.
4. `inventory-service`: `order.cancelled` event'ini dinler ve `release(orderId)` cagirir — ayrilan stok geri verilir.
5. `notification-service`: kullaniciya iptal bildirimi gonderir.

**Senaryo 3: Idempotent retry**
`inventory-service.release()` metodu idempotent olarak tasarlanmistir:

```java
public void release(Long orderId) {
    List<Reservation> existing = reservations.findByOrderId(orderId);
    if (existing.isEmpty()) {
        log.info("release(order={}): nothing to release (already gone or never reserved)", orderId);
        return;
    }
    // ... stok geri ver ve reservation'lari sil
}
```

Eger ayni `order.cancelled` event'i iki kez teslim edilirse (RabbitMQ at-least-once delivery), ikinci cagri reservasyon bulamaz ve no-op olarak doner — stok sayilari bozulmaz.

---

### Soru 34: Saga pattern'inde compensation (telafi) nedir? (Seviye: Orta)

Compensation, bir saga adiminin basarisiz olmassi durumunda onceki adimlarin etkisini geri almak icin yapilan islemdir. Geleneksel veritabani transaction'larinda `ROLLBACK` komutu tum degisiklikleri geri alir. Ancak dagitik sistemlerde her adim farkli bir veritabaninda tamamlanmis bir transaction'dir ve geri alinamaz — bunun yerine yeni bir transaction ile etkisi telafi edilir.

Projede iki compensation ornegi vardir:

**UserRegistrationSaga compensation**:
`services/basket-service/src/main/java/com/example/basket/saga/UserRegisteredListener.java` basarisiz olursa:
```java
rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE,
    SagaTopology.BASKET_FAILED_ROUTING_KEY,
    BasketCreationFailedEvent.of(event.userId(), event.email(), ex.getMessage()));
```
auth-service'teki `BasketFailedListener` bu event'i alir ve yeni olusturulmus kullaniciyi siler — "kaydi geri al" islemi.

**CheckoutSaga compensation**:
Odeme reddedildiginde `order-service` siparisi CANCELLED yapar ve `order.cancelled` yayinlar. `inventory-service` bu event'i dinleyerek ayrilan stoku geri verir:
```java
public void onOrderCancelled(Map<String, Object> event) {
    Long orderId = ((Number) event.get("orderId")).longValue();
    inventoryService.release(orderId);
}
```

Compensation islemleri "semantik geri alma" dir — orjinal islemi tam tersine cevirir. Ancak bazi durumlarda tam geri alma mumkun olmayabilir (orn. gonderilmis bir e-posta geri alinamaz) — bu durumlarda "compensating action" yorum/bildirim gonderme olabilir.

---

### Soru 35: Neden idempotent event consumer'lar onemlidir? (Seviye: Ileri)

Mesaj kuyruklerinde "at-least-once delivery" (en az bir kez teslim) garantisi vardir — RabbitMQ bir mesajin en az bir kez teslim edildigini garanti eder ama ayni mesajin birden fazla kez teslim edilmeyecegini garanti ETMEZ. Ag kesintisi, consumer crash'i veya acknowledgment kaybinda ayni mesaj tekrar teslim edilebilir.

Eger consumer idempotent degilse, ayni event'in iki kez islenmesi yanlis sonuclara yol acar. Ornegin: stok azaltma islemini idempotent olmasaydi, ayni `order.created` event'i iki kez teslim edildiginde stok iki kez azaltilirdi.

Projede idempotency su sekillerde saglanir:

1. **inventory-service release idempotency**: `release(orderId)` metodu, reservation'lar zaten silinmisse no-op doner:
```java
List<Reservation> existing = reservations.findByOrderId(orderId);
if (existing.isEmpty()) { return; } // idempotent
```

2. **basket-service idempotency**: `createEmptyBasketFor(email)` metodu, sepet zaten varsa yeniden olusturmaz.

3. **Event ID**: Her event'in bir `eventId` (UUID) alani vardir. Gercek bir production sisteminde consumer bu ID'yi bir "processed events" tablosunda saklayarak ayni event'in tekrar islenmesini onleyebilir.

4. **Dogal idempotency**: `order.setStatus(OrderStatus.CANCELLED)` islemi zaten idempotent'dir — ayni siparisi iki kez CANCELLED yapmak sonucu degistirmez.

---

### Soru 36: Servisler arasi iletisim nasil yapilandirildi? Senkron mu, asenkron mu? (Seviye: Orta)

Projede her iki iletisim modeli de kullanilir:

**Senkron (HTTP)**: Istemciden (browser veya baska servis) gelen istekler senkron olarak islenir. Ornegin:
- Frontend, gateway uzerinden `GET /api/products/1` istegi gonderir — product-service senkron olarak yanit verir.
- search-service, product-service'ten urunleri cermek icin `WebClient` ile HTTP istegi gonderir (bootstrap indexing sirasinda).

**Asenkron (RabbitMQ)**: Servisler arasi saga event'leri asenkron olarak gonderilir. `order-service` siparisi kaydettikten sonra `order.created` event'ini RabbitMQ'ya yayinlar ve hemen istemciye `202 Accepted` doner. inventory-service bu event'i kendi hizinda isler.

Asenkron iletisimin avantajlari:
1. **Temporal decoupling**: order-service, inventory-service'in ayakta olmasina bagimli degildir — mesaj RabbitMQ'da kuyrukta bekler.
2. **Yuk dengeleme**: Ani siparis artisinda mesajlar kuyrukta birikir ve consumer'lar kendi hizlarinda isler.
3. **Hata toleransi**: Consumer crash etse bile mesajlar kaybolmaz; yeniden basladiginda kaldigi yerden devam eder.

Senkron iletisimin kullanildigi yer: Kullanicinin aninda yanit bekledigi durumlar (urun listesi, sepet goruntuleme). Asenkron iletisimin kullanildigi yer: Arkaplan islemleri (odeme isleme, stok ayirma, bildirim gonderme).

---

### Soru 37: Neden RabbitMQ topic exchange kullanildi? (Seviye: Orta)

Projede tek bir topic exchange tanimlidir: `saga.exchange`. Tum saga event'leri bu exchange uzerinden routing key ile yonlendirilir.

RabbitMQ'da dort exchange tipi vardir:
- **Direct**: Mesaj tam olarak eslesen routing key'e sahip queue'ya gider.
- **Topic**: Mesaj, wildcard pattern ile eslesen routing key'lere sahip queue'lara gider.
- **Fanout**: Mesaj, tum baglanmis queue'lara gider (routing key dikkate alinmaz).
- **Headers**: Mesaj, header degerlerine gore yonlendirilir.

Topic exchange secilmesinin sebepleri:

1. **Esneklik**: `order.confirmed` routing key'i hem `basket.order-confirmed.queue` hem de `notification.order-confirmed.queue`'ya gidebilir — ayni event'i birden fazla consumer dinleyebilir. Direct exchange'te bu mumkundur ama topic exchange gelecekte wildcard kullanimi olanagi saglar (orn. `order.*` ile order'la ilgili tum event'leri dinleme).

2. **Tek exchange**: Tum saga event'leri tek bir exchange uzerinden akar. Farkli exchange'ler yerine tek bir exchange + farkli routing key'ler daha yonetilebilir bir topoloji olusturur.

3. **Self-documenting**: `SagaTopology` sinifi tum routing key'leri ve queue adlarini sabitler olarak tanimlar. Bu sinif, her serviste bagimsi olarak kopyalanir — servisler birbirinin kodunu import etmez ama ayni kontrata uyar.

Routing key ornekleri: `user.registered`, `order.created`, `inventory.reserved`, `payment.succeeded`, `order.confirmed`, `order.cancelled`.

---

### Soru 38: Eventual consistency (nihai tutarlilik) nedir? (Seviye: Ileri)

Geleneksel monolitik uygulamalarda ACID transaction'lari ile **anlattik tutarlilik (strong consistency)** saglanir — bir transaction tamamlandiginda tum okuyucular en guncel veriyi gorur. Ancak mikroservis mimarisinde her servisin kendi veritabani oldugu icin, servisler arasinda anlatik tutarliliik saglanamaz.

Eventual consistency, "tum guncelemelerin sonunda tum servislere yayilacagi ve sistemin tutarli bir duruma gelecegi" garantisidir — ama bu yayilma anlatti degil, bir sure alinabilir.

Projede eventual consistency ornekleri:

1. **Siparis akisi**: `order-service` siparisi PENDING olarak kaydeder ama odeme henuz islenmemistir. Birkan saniye icinde payment-service odemeyi isler ve order-service siparisi PAID'e gunceller. Bu arada siparis durumu PENDING olarak gorulur — bu, tutarsiz ama "eventually consistent" bir durumdur.

2. **Arama indeksi**: product-service'teki urunler Elasticsearch'te kopyalanir. Urun eklendiginde ES indeksi aninda guncellenmez — search-service `ProductIndexer` ile periyodik olarak senkronize eder. Bu arada yeni urun aramada gorulmeyebilir.

3. **Sepet olusturma**: Kullanici kaydolur, auth-service kullaniciyi kaydeder ve `user.registered` event'i yayinlar. basket-service bu event'i alip bos sepeti olusturur. Bu arada (birkan milisaniye) kullanici kayitli ama sepetsizdir.

Frontend bu durumu su sekilde ele alir: `/orders/:id` sayfasi siparisi 2 saniyede bir pollayarak PENDING -> PAID gecisini canli gosterir. Kullanici "siparisim isleniyor" mesajini gorur ve gecis gerceklesince sayfa otomatik guncellenir.

---

### Soru 39: search-service, product-service ile nasil senkronize olur? (Seviye: Orta)

`services/search-service/src/main/java/com/example/search/service/ProductIndexer.java` sinifi bu senkronizasyondan sorumludur. Uygulama basladiginda (`@EventListener(ApplicationReadyEvent.class)`) product-service'ten tum urunleri HTTP ile sayfa sayfa ceker ve Elasticsearch'e toplu indeksler:

```java
ProductPage pageData = client.get()
    .uri(uri -> uri.path("/api/products")
        .queryParam("page", currentPage)
        .queryParam("size", size)
        .build())
    .retrieve()
    .bodyToMono(ProductPage.class)
    .block(Duration.ofSeconds(15));

List<ProductDocument> docs = pageData.content().stream().map(this::toDocument).toList();
repo.saveAll(docs);
```

Bu yaklasim **pull-based reconciliation** olarak adlandirilir. Avantajlari:
1. **Basitlik**: product-service'in event yayinlamasina gerek yoktur.
2. **Yeniden indekslenebilirlik**: ES volume silindiginde `POST /api/search/reindex` ile tam senkronizasyon tetiklenebilir.
3. **Bootstrap**: Ilk acilista tum verilerin cekilmesi icin idealdir.

Dezavantaji: gercek zamanli degil — product-service'te bir urun guncellenirse ES indeksi hemen guncellenmez. README'de belirtildigi gibi, gelecekte product-service `product.created/updated/deleted` event'leri yayinlayabilir ve search-service bunlari `@RabbitListener` ile dinleyerek gercek zamanli senkronizasyon saglayabilir. Bu durumda pull-based approach bootstrap fallback olarak kalir.

docker-compose.yml'de search-service, product-service'in healthy olmasini bekler: `depends_on: product-service: condition: service_healthy`. Bu, indexer'in product-service'ten veri cekebilmesini garanti eder.

---

### Soru 40: Arama icin neden PostgreSQL `LIKE` sorgusu yerine Elasticsearch kullanildi? (Seviye: Orta)

product-service zaten PostgreSQL'de basit metin aramasi saglar: `findByNameContainingIgnoreCase(String q, Pageable pageable)`. Bu, SQL'de `WHERE LOWER(name) LIKE '%query%'` sorgusuna donusur. Peki neden ayri bir search-service ve Elasticsearch gerekli?

1. **Full-text search**: PostgreSQL `LIKE` sorgusu kelimelerin iceriginde arar ama kikleme (stemming) yapmaz. "Telefonlar" aradigignizda "telefon" kelimesini icerien urunleri bulamaz. Elasticsearch'in `turkish` analyzer'i otomatik kokleme yapar: "telefonlar" -> "telefon".

2. **Fuzzy matching**: Kullanici "telefoon" (yazim hatasi) yazdiginda ES `fuzziness: AUTO` ile en yakin eslesen sonuclari bulur. SQL LIKE bunu yapamaz.

3. **Relevance scoring**: ES, `multi_match` sorgusuyla `name^3, brand^2, description` agirliklarini kullanarak sonuclari ilgi duzeyine gore siralar. Urun adindaki eslesme, aciklamadaki eslesmeden 3 kat daha onemlidir. SQL'de bu agirlikli siralama cok zordur.

4. **Faceted search**: Her sorgu sonucu, marka bazinda, kategori bazinda urun sayilarini ve fiyat araligini doner (aggregations). Frontend'teki sol panel bu bilgilerle canli filtreler olusturur. SQL'de bu, her facet icin ayri `COUNT` + `GROUP BY` sorgusu gerektirir ve performans sorunlari olusur.

5. **Performans**: ES, ters indeks (inverted index) kullansarak milyonlarca kayit uzerinde milisaniye mertebesinde arama yapar. SQL LIKE ise tablo taramasi (full table scan) gerektirir ve buyuk veri setlerinde yavaslar.

---

### Soru 41: Neden Redis cache yalnizca product-service'te kullanildi? (Seviye: Orta)

product-service, Redis cache kullanan tek servistir. `RedisConfig` sinifinda uc cache tanimlidir:
- `products:byId` — 30 dakika TTL
- `products:bySlug` — 30 dakika TTL
- `products:categories` — 24 saat TTL

Diger servislerde Redis kullanilmamasinin sebepleri:

1. **Okuma yogunlugu**: Urun sayfasi en cok ziyaret edilen sayfadir — her urun gosterimi `getBySlug()` cagirir. Bu yuk altinda her seferinde PostgreSQL'e gitmek gereksizdir. Diger servisler (order, payment, notification) cogunlukla yazma agirliklidir ve okuma yuku dusuktur.

2. **Veri degisim hizi**: Urun katalogu nadiren degisir (gunluk veya haftalik) ama siparisler surekli olusturulur. Cache, sik okunan ama nadiren degisen veriler icin en etkilidir. Siparis durumunu cache'lemek, saga event'leriyle surekli invalidasyon gerektirirdi — bu, cache'in faydasini azaltir.

3. **Tutarlilik gereksinimleri**: Siparis durumunun (PENDING -> PAID) aninda guncellenmesi gerekir — stale cache kabul edilemez. Urun fiyatinin 30 dakika gecmeli olmasi ise kabul edilebilir.

4. **Altyapi karmasikligi**: Her servise Redis eklemek, her birinin cache invalidasyon stratejisini yonetmek gereksiz karmasiklik olusturur. YAGNI (You Aren't Gonna Need It) prensibi uygulanmistir.

docker-compose.yml'de Redis `--maxmemory 128mb --maxmemory-policy allkeys-lru` ile calisir — bellek dolsogunda en az kullanilan anahtarlar silinir.

---

### Soru 42: `CacheErrorHandler` pattern'i neden kullanildi? (Seviye: Ileri)

`services/product-service/src/main/java/com/example/product/config/CacheConfig.java`:

```java
@Override
public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
        @Override
        public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache GET failed [cache={}, key={}]: {}", cache.getName(), key, e.getMessage());
        }
        // ... put, evict, clear icin de benzer handler'lar
    };
}
```

Bu pattern, **cache-aside with graceful degradation** olarak bilinir. Redis baglantisi kesildiginde veya Redis crash ettiginde, varsayilan davranis olarak Spring exception firlatir ve istek basarisiz olur. `CacheErrorHandler` ile bu exception yakalanir, loglanir ve istek normal sekilde veritabanina yonlendirilir.

Bu neden onemlidir:
1. **Yuksek kullanilabilirlik**: Redis teknik olarak bir "cache"'tir — source of truth degil. Redis duserse uygulama yavaslar ama calismaya devam etmelidir.
2. **Production guvenligi**: Redis bellek limiti asildiginda, ag sorunu oldigunda veya Redis yeniden baslatildiginda uygulama 500 hatasi dondurmemelidir.
3. **Gozlemlenebilirlik**: `log.warn()` ile cache hatalari logllanir ve Loki/Grafana uzerinden izlenebilir. Bu, Redis sorunlarinin fark edilmesini saglar.

Bu yaklasima "cache is optional, database is mandatory" denir. Cache hit olursa hizli yanit, miss olursa veya hata olursa yavas ama dogru yanit garanti edilir.

---

### Soru 43: auth-service'teki rate limiting nasil calisir? (Seviye: Orta)

`services/auth-service/src/main/java/com/example/jwtjava/filter/RateLimitFilter.java` sinifi, Bucket4j kutuphanesi ile IP bazli rate limiting uygular:

```java
private static final int      CAPACITY      = 10;
private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
```

Her IP adresi icin bir "bucket" (kova) olusturulur. Bu kova 10 token kapasitesine sahiptir ve dakikada 10 token ile doldurulur (greedy refill). Her istek 1 token tuketir. Token kalmdiginda istek reddedilir ve RFC 7807 formattinda `429 Too Many Requests` yaniti donulur:

```java
ProblemDetail pd = ProblemDetail.forStatusAndDetail(
    HttpStatus.TOO_MANY_REQUESTS, "Cok fazla istek. Lutfen 1 dakika bekleyin.");
pd.setProperty("retryAfterSeconds", REFILL_PERIOD.getSeconds());
response.setHeader("Retry-After", String.valueOf(REFILL_PERIOD.getSeconds()));
```

`shouldNotFilter()` metodu yalnizca `/api/auth/` ile baslayan istekleri filtreler — urun listeleme, arama gibi acik endpoint'ler rate limit'e tabi degildir. Bunun sebebi: login ve register endpoint'leri brute-force saldirilarinin hedefidir.

`resolveClientIp()` metodu once `X-Forwarded-For` header'ina bakar (gateway arkasindan gercek IP'yi almak icin), yoksa `request.getRemoteAddr()` kullanir.

`ConcurrentHashMap` kullanilmasinin sebebi: bircok thread (HTTP istegi) ayni anda farkli IP'ler icin bucket'lara erisebilir — thread-safe bir veri yapisi gereklidir.

---

### Soru 44: `@AuthenticationPrincipal String` neden veritabaninden kullanici cekmeye tercih edildi? (Seviye: Orta)

Projede controller'lar kullanici bilgisini JWT'den alir, veritabanindan degil. Ornegin order-service'teki `OrderController`:

```java
@PostMapping("/checkout")
public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal String email,
                                               @Valid @RequestBody CheckoutRequest req) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderService.checkout(email, req));
}
```

`@AuthenticationPrincipal`, `SecurityContextHolder`'daki authentication nesnesinin principal kismini inject eder. `JwtAuthFilter`'da bu principal, JWT'nin `sub` claim'i olan e-posta adresidir:

```java
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(username, null, authorities);
```

Burada `username` (e-posta) principal olarak set edilir.

Bu yaklasimin avantajlari:
1. **Sifir veritabani cagrisi**: Her HTTP isteginde kullaniciyi DB'den cekme maliyeti yoktur. Token'dan okunan e-posta yeterlidir.
2. **Servis bagimsizligi**: order-service, auth-service'in veritabanina erismez — sadece JWT'yi okur.
3. **Performans**: Veritabani cagrisi 5-50ms surerken, JWT parse etmek <1ms surer.
4. **Stateleless**: Herhangi bir server instance'i, herhangi bir istegi isleyebilir — oturum bilgisi saklama gerekli degildir.

Dezavantaji: Kullanici hesabi silinse veya devre disi birakilssa, access token suresi dolana kadar gecerli kalir. Bu, access token'in kisa omurlu olmasi (15 dakika) ile dengelenir.

---

### Soru 45: CORS nedir ve gateway nasil yonetiyor? (Seviye: Orta)

CORS (Cross-Origin Resource Sharing), tarayicilarin farkli origin'lerden (protokol + domain + port) gelen HTTP isteklerini engellemesini kontrol eden bir guvenlik mekanizmasidir. Ornegin `http://localhost:3000` (frontend) adresinden `http://localhost:8000` (gateway) adresine istek gondermek farkli origin'dir.

`services/api-gateway/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

Bu yapilandirma, gateway'in tum origin'lerden gelen isteklere izin vermesini saglar. `allowCredentials: true` cookie'lerin isteklerle birlikte gonderilmesine izin verir — refresh token HttpOnly cookie olarak tasinir ve bu ayar gereklidir.

Ancak projede production'da CORS preflight istekleri tamamen ortadan kaldirilmistir. Bunun sebebi: frontend nginx container'i, `/api/` isteklerini ayni origin uzerinden gateway'e proxy'ler:

```nginx
location /api/ {
    proxy_pass $gateway$request_uri;
}
```

Tarayici acisindarn `http://localhost:13000/api/products` istegi ayni origin'den gelir (frontend de ayni adreste) — bu nedenle CORS preflight tetiklenmez. Bu, her API isteginde ek bir OPTIONS isteginin onune gecer ve performans kazanci saglar.

---

### Soru 46: Neden entity dogrudan dondurmek yerine ayri request/response DTO'lari kullanildi? (Seviye: Orta)

Projede her servisin ayri request DTO'lari (`RegisterRequest`, `CheckoutRequest`, `AddItemRequest`) ve response DTO'lari (`AuthResponse`, `ProductResponse`, `OrderResponse`) vardir. Entity'ler hicbir zaman dogrudan API yaniti olarak donmez.

Bu ayirimin sebepleri:

1. **Guvenlik**: `User` entity'sinde `password` alani vardir. Eger entity dogrudan donulse, sifre hash'i JSON yanitinda gorunurdu. `AuthResponse` ise yalnizca `accessToken`, `tokenType` ve `expiresIn` alanlarini icerir.

2. **API kontrati stabilitesi**: Entity'ye yeni bir alan eklendiginde (orn. `lastLoginAt`), API yaniti degismemelidir. DTO, entity'den bagimsiz olarak API kontratini kontrol eder.

3. **Hesaplanmis alanlar**: `ProductResponse`'daki `discountedPrice` alani veritabaninda yoktur — `from()` metodunda fiyat ve indirim oranindan hesaplanir. Entity'de bu alan saklamak gereksiz veri tekrari olurdu.

4. **Validation ayirimi**: `RegisterRequest`'te `@NotBlank`, `@Email`, `@StrongPassword` gibi validation anotasyonlari vardir. Bunlarin entity'de olmasi JPA ile cakismalara neden olurdu ve domain modelini kirletirdi.

5. **Servisler arasi izolasyon**: `OrderCreatedEvent` record'u, saga iletisiminde kullanilir. Bu, entity'nin ic yapisini diger servislere acinmamasi icin bir kalkansdir.

---

### Soru 47: JwtAuthFilter ve RequestLoggingFilter zinciri nasil calisir? (Seviye: Orta)

Projede her serviste iki ozel filter vardir ve bunlar belirli bir sirada calisir:

**1. RequestLoggingFilter** (`@Order(1)` — once calisir):
- Gelen istegin `X-Correlation-Id` header'ini okur (yoksa UUID uretir).
- Correlation ID'yi SLF4J MDC'ye koyar — boylece bu istek sirasinda uretilen tum log satirlarinda gorunur.
- Istek baslangicinidda `-> POST /api/orders/checkout` seklinde log yazar.
- Istek tamamlandiginda `<- POST /api/orders/checkout status=202 | 434 ms` seklinde log yazar.
- Correlation ID'yi response header'ina ekler.

**2. JwtAuthFilter** (sonra calisir):
- `Authorization: Bearer <token>` header'ini okur.
- JWT'yi parse eder, `sub` (e-posta) ve `roles` claim'lerini cikarir.
- `SecurityContext`'e `UsernamePasswordAuthenticationToken` olarak yerlastirir.
- Token suresi dolmussa veya gecersizse 401 hatasi doner.

auth-service'te ek olarak **3. RateLimitFilter** vardir — `/api/auth/` isteklerinde IP bazli rate limiting uygular.

Filter sirasi kritiktir: loglama once calisir boylece kimlik dogrulama hatalari da loglanir. JWT filtresi auth'tan sonra calisir cunku once correlation ID set edilmeli ki JWT hatalari da dogru correlation ID ile loglansin.

```java
@Component
@Order(1)   // run before JwtAuthFilter
public class RequestLoggingFilter extends OncePerRequestFilter { ... }
```

---

### Soru 48: Correlation ID propagasyonu nasil calisir? (Seviye: Ileri)

Correlation ID, bir istegin tum servisler boyunca izlenmesini saglayan benzersiz bir tanimlayicidir. Akis su sekilde calisir:

1. **Gateway**: `RequestLoggingGlobalFilter` gelen istekde `X-Correlation-Id` header'i yoksa UUID uretir ve downstream servislere iletir.
2. **Her servis**: `RequestLoggingFilter` gelen `X-Correlation-Id` header'ini alir (gateway tarafindan set edilmistir), MDC'ye koyar ve response header'ina ekler.
3. **Log formati**: Tum servisler ayni log pattern'ini kullanir:
```
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{correlationId:-},%X{traceId:-}] %logger{36} - %msg%n
```
`%X{correlationId:-}` MDC'den correlation ID'yi okur.

4. **Frontend**: `X-Correlation-Id` header'i response'ta geri doner — frontend veya curl ile loglarda bu ID ile arama yapilabilir.

Ornek log:
```
2026-04-22 14:23:45.678 INFO [b4a9...,1a2b3c...] c.e.order.OrderController - -> POST /api/orders/checkout
```

Grafana Loki'de `{correlationId="b4a9..."}` sorgusguyla bu istegin tum servislerdeki loglari filtrelenebilir. `traceId` ise Micrometer/OpenTelemetry tarafindan atanir ve Jaeger'daki distributed trace ile eslestir — boylece log'dan trace'e ve trace'den log'a cift yonlu gecis mumkundur.

---

### Soru 49: Neden her serviste healthcheck tanimli? (Seviye: Baslangic)

docker-compose.yml'de her servis icin healthcheck tanimlidir:

```yaml
auth-service:
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
    interval: 15s
    timeout: 5s
    retries: 6
    start_period: 40s
```

Spring Boot Actuator, her serviste `/actuator/health` endpoint'i saglar. Bu endpoint, uygulamanin saglikli olup olmadigini kontrol eder — veritabani baglantisi, RabbitMQ baglantisi, disk alani gibi bilesenler dahil.

Healthcheck'lerin uc kritik rolu vardir:

1. **Baslatis sirasi (depends_on: condition: service_healthy)**: basket-service, postgres ve rabbitmq'nun healthy olmasini bekler. Healthcheck olmadan servis, bagimlik hazir olmadan baslar ve baglanti hatalari alir. `start_period: 40s` ise servisin ilk baslatilma zamanini tanir — bu sure icinde basarisiz healthcheck'ler sayilmaz.

2. **Otomatik yeniden baslatma**: `restart: on-failure` ile birlikte healthcheck, crash eden servislerin Docker tarafindan otomatik olarak yeniden baslatilmasini saglar.

3. **Service discovery**: Eureka, saglikli olmayan servisleri registry'den cikarir ve gateway bu servislere trafik yonlendirmez.

Ozel healthcheck ornekleri: Elasticsearch `_cluster/health?wait_for_status=yellow` ile kontrol edilir cunku tek node'lu cluster'da `green` yerine `yellow` normal durumdur (replika yok). Redis ise `redis-cli ping` ile kontrol edilir.

---

### Soru 50: Eureka ile service discovery nasil calisir? (Seviye: Orta)

`services/eureka-server/src/main/java/com/example/eureka/EurekaServerApplication.java` Netflix Eureka Server'i icerir. Her mikroservis, basladiginda kendini Eureka'ya kaydeder:

```yaml
# Her servisin application.yml'inde:
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
```

Eureka, bir **service registry** (servis kayit defteri)'dir. Calisma prensibi:

1. **Register**: auth-service basladiginda Eureka'ya "Ben auth-service'im, IP adresim 172.18.0.5, portum 8080" bilgisiyle kaydolur.
2. **Heartbeat**: Her 30 saniyede Eureka'ya "hala buradayim" sinyali gonderir.
3. **Discovery**: API Gateway, `lb://auth-service` adresini cozumlemek icin Eureka'ya "auth-service nerede?" diye sorar ve gercek IP:port bilgisini alir.
4. **Eviction**: Eureka, 90 saniye iceinde heartbeat gelmeyen servisleri listeden cikarir (`eviction-interval-timer-in-ms: 5000`).

`prefer-ip-address: true` ayari, Docker container'lari icinde hostname yerine IP adresi kullanilamasini saglar — container hostname'leri tahmin edilemez olabilir.

Eureka Server kendi kendini kaydetmez ve registry fetch etmez:
```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

API Gateway'in `lb://` prefix'i Spring Cloud LoadBalancer'i tetikler — Eureka'dan alinan instance listesi uzerinde round-robin yuk dengeleme yapar. Bu sayede ayni servisin birden fazla instance'i calistirildiginda trafik otomatik olarak dagitilir.

---

## Bolum 3: Altyapi & DevOps (Sorular 51-75)

---

### Soru 51: Docker nedir ve neden container'lama yapildi? (Seviye: Baslangic)

Docker, uygulamalari ve tum bagimlilikklarini bir "container" icerisinde paketleyerek, herhangi bir ortamda ayni sekilde calismasini saglayan bir platformdur. Bu projede 10 mikroservis, PostgreSQL, RabbitMQ, Redis, Elasticsearch, Eureka, Prometheus, Grafana, Jaeger, Loki, Promtail ve frontend olmak uzere ~20 container Docker Compose ile yonetilir.

Container'lamanin sebepleri:

1. **"Bende calisiyor" sorunu**: Java 21, Maven, Node 20, PostgreSQL 16, RabbitMQ, Elasticsearch 8, Redis 7 — bunlarin tamamini her gelisitiricinin makinesine kurmaak ve surumlerini uyumlu tutmak kabus olurdu. `docker compose up --build` komutu ile tek seferde her sey ayaga kalkar.

2. **Izolasyon**: Her container kendi dosya sistemi, ag arayuzu ve process space'ine sahiptir. Elasticsearch'in bellek kullanimi PostgreSQL'i etkilemez.

3. **Tekrarlanabilirlik**: `Dockerfile` ve `docker-compose.yml` dosyalari, ortamin tam tarifini icerir. Ayni dosyalarla CI/CD pipeline'inda, staging'de veya production'da birebir ayni ortam olusturulabilir.

4. **Kaynak yonetimi**: `mem_limit: 512m` (RabbitMQ) ve `ES_JAVA_OPTS=-Xms256m -Xmx256m` (Elasticsearch) ile kaynak limitleri belirlenir.

5. **Ag yonetimi**: Docker Compose otomatik olarak bir bridge network olusturur. Servisler birbirine container adi ile erisir (orn. `jdbc:postgresql://postgres:5432/authdb`). Disariya yalnizca gateway ve frontend portlari acilir.

---

### Soru 52: Multi-stage Docker build nedir ve neden kullanildi? (Seviye: Orta)

`services/auth-service/Dockerfile`:

```dockerfile
# Stage 1: Dependencies (cached layer)
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository mvn dependency:go-offline -q

# Stage 2: Build
FROM deps AS builder
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn package -DskipTests -q

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Multi-stage build, bir Dockerfile icerisinde birden fazla `FROM` kullanarak farkli asamalar tanimlamayi saglar. Her asamanin kendi base image'i vardir ve yalnizca son asamadaki dosyalar final image'a dahil edilir.

Neden multi-stage:

1. **Image boyutu**: Build asamasinda Maven (JDK + Maven + tum dependency'ler) ~800MB'dir. Runtime asamasinda yalnizca JRE Alpine (~80MB) + uygulama JAR'i (~50MB) vardir. Final image ~130MB olur, tek stage olsa ~800MB olurdu.

2. **Guvenlik**: Build araclari (Maven, JDK complier) runtime image'da bulunmaz. Saldirgan erisilse bile compiler veya build araclari mevcut degildir.

3. **Katman onbellekleme**: Stage 1 yalnizca `pom.xml` kopyalar ve dependency'leri indirir. Kaynak kodu degistiginde ama `pom.xml` degismediyinde bu katman onbellekten gelir — rebuild saniyeler icerir.

Frontend icin de benzer bir yaklasim vardir: Node ile build, nginx ile serve.

---

### Soru 53: `--mount=type=cache,target=/root/.m2/repository` ne yapar? (Seviye: Ileri)

```dockerfile
RUN --mount=type=cache,target=/root/.m2/repository mvn dependency:go-offline -q
```

Bu, Docker BuildKit'in **cache mount** ozelligidir. Normal bir Docker build'de her `RUN` komutu yeni bir katman olusturur ve onceki katmanin dosya sistemi uzerinde calisir. Maven dependency'leri `~/.m2/repository` altina indirilir ve bu katman image'in bir parcasi olur.

Cache mount ise farkli davranir: belirtilen dizini (`/root/.m2/repository`) build'ler arasinda payrasilan bir cache volume'una baglar. Bu, su faydaları saglar:

1. **Build'ler arasi cache**: Ilk build'de tum Maven dependency'leri indirilir (~5-10 dakika). Ikinci build'de cache'teki dependency'ler kullanilir — `mvn dependency:go-offline` saniyeler iceinde tamamlanir.

2. **Image boyutunda tasarruf**: Cache mount'taki veriler image katmanina yazilmaz — final image'a dahil edilmez. Normalde `.m2/repository` ~500MB yer kaplar.

3. **CI/CD performansi**: GitHub Actions'ta her push'ta tum dependency'lerin yeniden indirilmesi 5+ dakika surer. Cache mount ile bu sure dramatik olarak azalir.

`Dockerfile` basindaki `# syntax=docker/dockerfile:1.6` satiri, BuildKit ozelliklerini aktive eden direktiftir. Bu soz dizimi olmadan `--mount` kullanilmaz.

---

### Soru 54: Runtime image olarak neden `eclipse-temurin:21-jre-alpine` secildi? (Seviye: Orta)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
```

Bu image secimi uc kritere dayanir:

1. **Eclipse Temurin**: Adoptium (eski AdoptOpenJDK) tarafindan saglanan, TCK sertifikali, production-grade OpenJDK dagittimidir. Oracle JDK'nin ticari lisans kisitlamalari yoktur.

2. **JRE (Java Runtime Environment)**: JDK yerine JRE kullanilir cunku runtime'da compiler gerekli degildir. JDK ~300MB iken JRE ~80MB'dir — %73 daha kucuk.

3. **Alpine Linux**: Standart Debian/Ubuntu tabanli image'lar ~200MB iken Alpine ~5MB'dir. Alpine, musl libc ve BusyBox kullanan minimalist bir Linux dagitimidir. Bu, final image'in toplam boyutunu ~130MB'e dusurur.

Kucuk image boyutunun faydalar:
- **Daha hizli deploy**: Container registry'den cekilmesi saniyeler icerisinde tamamlanir.
- **Daha kucuk saldiri yuzeyi**: Daha az kurulu paket = daha az potansiyel zafiyet.
- **Daha az disk kullanimi**: 20 container x 130MB = 2.6GB. Buyuk image'larla bu 20GB'i asabilirdi.

---

### Soru 55: Container'larda neden root olmayan kullanici (`appuser`) kullanildi? (Seviye: Orta)

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

Docker container'lari varsayilan olarak `root` kullanicisi ile calisir. Eger bir saldirgan container icerisindeki uygulamada bir zafiyet bulursa (orn. RCE — Remote Code Execution), root olarak calistigi icin container icerisindeki tum dosyalara erisebilir ve potansiyel olarak container'dan cikis (container escape) yapabilir.

`appuser` ile uygulamanin yalnizca kendi dosyalarina erisimi vardir. Root yetkisi gerektiren islemler (port 80'den dinleme, sistem dosyalarini degistirme, paket kurma) yapilamaz.

`-S` flagi "system user" olusturur (login shell ve home directory olmadan) — bu, interaktif erisimi daha da kisitlar.

Projede Promtail istisnai olarak `user: root` ile calisir cunku Docker socket'ine (`/var/run/docker.sock`) ve container log dosyalarina (`/var/lib/docker/containers`) erismesi gerekir — bu dosyalar yalnizca root tarafindan okunabilir.

Bu uygulama "principle of least privilege" (en az yetki ilkesi) guvenlik prensibinin somut uygulamasidir.

---

### Soru 56: Docker Compose nedir ve `depends_on` nasil calisir? (Seviye: Baslangic)

Docker Compose, bircok container'i tek bir YAML dosyasiyla tanimlamanizi ve yonetmenizi saglayan bir aractir. `docker-compose.yml` dosyasi, tum servislerin image'larini, ortam degiskenlerini, port eslemelerini, volume'larini ve bagimlilik siralamasini tanimlar.

`depends_on` ise servislerin baslatis sirasini kontrol eder:

```yaml
auth-service:
  depends_on:
    postgres:
      condition: service_healthy
    rabbitmq:
      condition: service_healthy
    eureka-server:
      condition: service_healthy
```

`condition: service_healthy` ile auth-service, PostgreSQL, RabbitMQ ve Eureka'nin healthcheck'lerini gecmesini BEKLER. Healthcheck olmadan `depends_on` yalnizca container'in baslamasini bekler — icerideki uygulamanin hazir olmasini BEKLEMEZ. Ornegin PostgreSQL container'i baslamis olabilir ama veritabani henuz baglanti kabul etmiyordur.

API Gateway yalnizca Eureka Server'a bagimlidir — diger servisleri Eureka uzerinden dinamik olarak kesfeder. Bu, gateway'in tum servislerin baslamasini beklemeden ayaga kalkmasini saglar.

`depends_on` kullanimi olmadan tum container'lar es zamanli baslar ve birbirine baglanmaya calistiginda baglanti hatalari alir. `restart: on-failure` bu hatalari hafifletir (servis yeniden baslar) ama healthcheck-based dependency daha guvenilirdir.

---

### Soru 57: Eureka Server ve service discovery nasil calisir? (Seviye: Orta)

Eureka Server, Netflix tarafindan gelistirilen ve Spring Cloud ile entegre olan bir service registry'dir. Projedeki rolu, mikroservislerin birbirini IP adresi ve port yerine isim ile bulmaaini saglamaktir.

`services/eureka-server/src/main/java/com/example/eureka/EurekaServerApplication.java`:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication { ... }
```

Calima prensibi:
1. Eureka Server port 8761'de baslar ve bir web dashboard sunar (http://localhost:18761).
2. Her mikroservis Spring Cloud Eureka Client dependency'si ile baslar ve kendini register eder.
3. Eureka, tum registered servislerin listesini, IP adreslerini ve port bilgilerini saklar.
4. API Gateway `lb://auth-service` adresi cozumlerken Eureka Client'i Eureka Server'a sorar.
5. Eureka, 30 saniyede bir heartbeat kontrol eder; yanit almazsa servisi listeden cikarir.

`enable-self-preservation: false` ayari, development ortami icin self-preservation modunu kapatir. Normalde Eureka, ag sorunlarinda servisleri agressif olarak cikarmasmasi icin self-preservation moduna girer — ancak development'ta bu, kapalln servislerin listede kalmasina neden olur.

Service discovery olmadan her servisin diger servislerin IP adreslerini bilmesi gerekirdi. Docker Compose icerisinde DNS ile bu mumkun olsa da, production'da birden fazla instance, olcekleme ve dinamik IP degisiklikleri nedeniyle service discovery kritik bir altyapi bileaenidir.

---

### Soru 58: Prometheus nedir ve metric toplama nasil calisir? (Seviye: Orta)

Prometheus, zaman serisi veritabani ve metrik toplama sistemidir. **Pull-based** calisir — hedef uygulamalara periyodik olarak HTTP istegi gonderir ve metrikleri toplar.

`infra/prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: n11-services
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - auth-service:8080
          - basket-service:8081
          # ... 10 servis
    relabel_configs:
      - source_labels: [__address__]
        target_label: service
        regex: '([^:]+):\d+'
        replacement: '$1'
```

Calisma prensibi:
1. Her Spring Boot servisi `micrometer-registry-prometheus` dependency'si ile `/actuator/prometheus` endpoint'ini acar.
2. Micrometer, JVM heap, thread sayisi, HTTP istek suresi, hata orani, RabbitMQ consumer metrikleri gibi bilgileri toplar.
3. Prometheus her 15 saniyede (`scrape_interval: 15s`) 10 servise HTTP istegi gonderir ve metrikleri ceker.
4. `relabel_configs` ile `auth-service:8080` adresinden `auth-service` label'i cikarilir — PromQL sorgularinda temiz `service` label'i kullanilir.
5. Metrikler 3 gun saklanir (`storage.tsdb.retention.time=3d`).

Ornek PromQL sorgusu:
```promql
sum(rate(http_server_requests_seconds_count{service="order-service",status=~"5.."}[1m]))
```
Bu, order-service'in son 1 dakikadaki 5xx hata hizini hesaplar.

---

### Soru 59: Grafana nedir ve dashboard'lar nasil provision ediliyor? (Seviye: Orta)

Grafana, Prometheus, Loki ve Jaeger gibi veri kaynaklarini gorsellestirenm bir dashboard aracididir. Bu projede Grafana, hazir dashboard ve veri kaynaklariyla otomatik olarak yapilandirilir (provisioning).

docker-compose.yml'deki volume mount'lar:
```yaml
volumes:
  - ./infra/grafana/provisioning:/etc/grafana/provisioning:ro
  - ./infra/grafana/dashboards:/var/lib/grafana/dashboards:ro
```

`infra/grafana/provisioning/datasources/datasources.yml` dosyasi uc veri kaynagini otomatik olarak tanimlar:
1. **Prometheus**: Metrik sorgulaari icin (varsayilan veri kaynagi).
2. **Jaeger**: Distributed trace goruntuleme icin. `tracesToLogsV2` yapilandirmasi ile bir span'dan Loki logarina gecis yapilabilir.
3. **Loki**: Log sorgulama icin. `derivedFields` ile log satarindaki `traceId`'den Jaeger span'ina link olusturulur.

Bu cift yonlu baglanti (trace -> log, log -> trace) sayesinde, bir performans sorunu tespit edildiginde Jaeger'dan baslayarak ilgili loglara, veya bir hata logundak baslayarak trace waterfall'una gecis yapilabilir — bu, "three pillars of observability" (metrics, traces, logs) arasindaki korelasyondur.

Grafana anonim erisim icin `Viewer` rolu ile ayarlidir — `docker compose up` sonrasi dogrudan dashboard'a erisebilirsiniz. Edit icin `admin/admin` kimlik bilgileri kullanilir.

---

### Soru 60: Jaeger ve distributed tracing nedir? (Seviye: Orta)

Jaeger, dagitik tracing sistemidir — bir istegin birden fazla servis uzerinden gectigi yolu gorsellestirir. Monolitik bir uygulamada bir istegin izlenmesi basittir cunku her sey tek bir process icerisindedir. Mikroservis mimarisinde ise bir `POST /api/orders/checkout` istegi 8 servisten gecer ve RabbitMQ uzerinden asenkron mesajlar icerir.

Jaeger, bu karmasik akisi "trace waterfall" olarak gorsellestirir:

```
browser -> gateway -> order-service (HTTP)
                    -> RabbitMQ publish (order.created)
                       -> inventory-service (AMQP consume)
                       -> PostgreSQL (reserve + insert)
                       -> RabbitMQ publish (inventory.reserved)
                          -> payment-service
                          -> RabbitMQ publish (payment.succeeded)
                             -> order-service
                             -> RabbitMQ publish (order.confirmed)
                                -> basket, notification
```

Her servis, `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` dependency'leri ile span'lar uretir ve Jaeger'a gonderir. docker-compose.yml'deki yapilandirma:

```yaml
MANAGEMENT_TRACING_SAMPLING_PROBABILITY: "1.0"
MANAGEMENT_OTLP_TRACING_ENDPOINT: http://jaeger:4318/v1/traces
```

`1.0` sampling orani, %100 isteklerin trace edildigini belirtir — demo icin idealdir, production'da `0.1` (%10) onerilir.

Jaeger UI'da (http://localhost:26686) `service = order-service` secilerek son trace'ler listelenir ve herbiri timeline gorunumunde incelenir.

---

### Soru 61: OpenTelemetry nedir ve trace propagasyonu nasil calisir? (Seviye: Ileri)

OpenTelemetry (OTel), traces, metrics ve logs icin vendor-neutral bir gozlemlenebilirlik standardtir. Bu projede OpenTelemetry yalnizca trace exporting icin kullanilir — metrikler Micrometer, loglar SLF4J uzerinden akar.

Trace propagasyonu su sekilde calisir:

1. Gateway bir istegi aldiginda, Micrometer bir `traceId` ve `spanId` uretir.
2. Bu ID'ler, downstream servislere HTTP header'lari olarak iletilir (`traceparent` header'i, W3C Trace Context standardi).
3. Her servis gelen `traceparent` header'ini okur ve ayni trace'e yeni span'lar ekler.
4. RabbitMQ mesajlarinda trace context, mesaj header'larina eklenir — boylece asenkron iletisim de trace'e dahil olur.
5. Her servis span'lari OTLP HTTP uzerinden Jaeger'a gonderir (`http://jaeger:4318/v1/traces`).

Micrometer'in `micrometer-tracing-bridge-otel` modulu, Micrometer trace API'sini OpenTelemetry exporter'ina kopruler. Bu sayede uygulama kodu Micrometer API'sini kullanir (Spring ekosistemiyle uyumlu) ama export formatini OTel standardi uzerinden yapar.

Log'larda `traceId` MDC'ye otomatik olarak konur:
```
[correlationId, traceId]
```
Bu sayede bir log satirindaki `traceId` ile Jaeger'da ilgili trace aranabilir.

---

### Soru 62: Loki nedir ve Elasticsearch'ten log icin farki nedir? (Seviye: Ileri)

Grafana Loki, Grafana Labs tarafindan gelistirilen bir log agregasyon sistemidir. Bu projede Loki, tum mikroservislerin loglarini toplar ve Grafana uzerinden aranabilir kilar.

Loki ile Elasticsearch arasindaki temel fark: **indeksleme stratejisi**.

**Elasticsearch** (projede urun arama icin kullanilir): Log iceriginin tamamini indeksler (full-text indexing). Her kelime inverted index'e eklenir. Bu, herhangi bir kelimeyle arama yapmayi hizlandirir ama cok fazla bellek ve disk tuketir. 10 servisin loglarini Elasticsearch'e gondermek GB mertebesinde depolama ve onemli RAM gerektirir.

**Loki**: Yalnizca **label'lari** indeksler (service, level, correlationId, traceId), log icerigini indekslemez. Log icerigi sikistirilmis olarak saklanir ve sorgu zamaninda taranir. Bu yaklasim:
- 10-100x daha az kaynak kullanir
- Daha ucuz depolama
- Label bazli sorgular cok hizli: `{service="order-service", level="ERROR"}`
- Tam metin aramasi daha yavas (grep-like tarama)

Bu proje icin Loki ideal bir secimdir cunku: (1) kaynak kisitli bir demo ortamidir (Elasticsearch zaten urun arama icin calisir), (2) loglar cogunlukla label (service, level, correlationId) ile filtrelenir, (3) Grafana ile dogal entegrasyon saglar.

---

### Soru 63: Promtail nedir ve neden root erisim gerektirir? (Seviye: Orta)

Promtail, Grafana Loki'nin log toplama agentidir. Docker container'larindan loglari toplayip Loki'ye gonderir.

`infra/promtail/promtail-config.yaml`:

```yaml
scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
```

Promtail, Docker socket'i (`/var/run/docker.sock`) uzerinden canli container'lari kesfeder ve log dosyalarini okur. docker-compose.yml'de:

```yaml
promtail:
  user: root
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
    - /var/lib/docker/containers:/var/lib/docker/containers:ro
```

`user: root` gerekliligi: Docker socket ve container log dosyalari (`/var/lib/docker/containers/`) root'a aittir. Normal bir kullanici bu dosyalara eriseemez. Alternatif olarak Docker group'una ekleme yapilabilir ama container icerisinde bu karmasiktir.

`pipeline_stages` ile log satirlari parse edilir:
```yaml
- regex:
    expression: '^(?P<ts>...)\s+(?P<level>[A-Z]+)\s+\[(?P<correlationId>[^,]*),(?P<traceId>[^\]]*)\]...'
```

Bu regex, Spring Boot log formatini parse ederek `level`, `correlationId` ve `traceId` degerlerini Loki label'i olarak cikarir. Boylece Grafana'da `{correlationId="abc123"}` sorgusguyla belirli bir istegin tum servislerdeki loglari filtrelenebilir.

`relabel_configs` ile infra container'lari (postgres, rabbitmq, redis vb.) filtrelenir — yalnizca uygulama servisllerinin loglari Loki'ye gonderilir.

---

### Soru 64: Flyway nedir ve neden veritabani migration'lari kullanildi? (Seviye: Orta)

Flyway, veritabani sema degisikliklerini version kontolu altinda yoneten bir migration aractidir. Her servisin `src/main/resources/db/migration/` dizininde SQL migration dosyalari bulunur.

Ornegin auth-service'in migration'lari:
- `V1__create_users_table.sql` — users tablosu
- `V2__create_refresh_tokens_table.sql` — refresh_tokens tablosu
- `V3__create_user_roles_table.sql` — user_roles join tablosu
- `V4__refresh_tokens_allow_multiple_per_user.sql` — unique constraint degisikligi

Flyway calisma prensibi:
1. Uygulama basladiginda Flyway, veritabaninda `flyway_schema_history` tablosunu kontrol eder.
2. Hangi migration'larin uygulandigini gorur ve henuz uygulanmamis migration'lari sirayla calistirir.
3. Her migration basarili oldugunda `flyway_schema_history`'ye kayit ekler.
4. Eger bir migration basarisiz olursa uygulama baslamaz — bozuk sema ile calismak yerine erken baarisiz olur.

Migration dosya adlari `V{n}__{description}.sql` formatindadir: `V` sabit prefix, `{n}` surum numarasi, `__` ayirici, `{description}` aciklama. Flyway, surum numaralarina gore siralama yapar.

`application.yml`'de Hibernate'in `ddl-auto: validate` olarak set edilmesi kritiktir:
```yaml
jpa:
  hibernate:
    ddl-auto: validate
```

Bu, Hibernate'in semalari otomatik olusturmasini veya degistirmesini ENGELLER — yalnizca mevcut semanin entity siniflaryla uyumlu oldugunu dogrular. Sema degisiklikleri YALNIZCA Flyway migration'lariyla yapilir.

---

### Soru 65: RabbitMQ nedir? Exchange, queue, binding ve routing key kavramlari (Seviye: Orta)

RabbitMQ, AMQP protokolunu uygulayan bir mesaj broker'idir. Servisler arasi asenkron iletisimi saglar.

Temel kavramlar:

**Exchange**: Mesajlarin ilk vardigi nokta. Producer, mesajlari exchange'e gonderir. Bu projede tek bir topic exchange vardir: `saga.exchange`.

**Queue**: Mesajlarin consumer'a teslim edilmek uzere bekledigi kuyruk. Her consumer kendi queue'sunu dinler. Ornegin: `basket.user-registered.queue`, `order.payment-succeeded.queue`.

**Binding**: Exchange ile queue arasindaki baglanti. Bir binding, "bu exchange'e bu routing key ile gelen mesajlari bu queue'ya yonlendir" kuralini tanimlar.

**Routing Key**: Mesajin hangi queue'lara gidecegini belirleyen etiket. Ornegin `order.created` routing key'i ile gonderilen mesaj, `inventory.order-created.queue`'ya yonlendirilir (cunku bu queue `order.created` routing key'i ile `saga.exchange`'e bind edilmistir).

Projede akis ornegi:
1. `OrderService.checkout()`, siparisi kaydeder.
2. `SagaEventPublisher.publishOrderCreated()`, `rabbitTemplate.convertAndSend(EXCHANGE, ORDER_CREATED_ROUTING_KEY, event)` cagirir.
3. RabbitMQ, `saga.exchange`'e gelen `order.created` mesajini eslesen queue'lara yonlendirir.
4. `inventory-service`'teki `InventoryListener.onOrderCreated()`, `@RabbitListener(queues = ORDER_CREATED_QUEUE)` ile mesaji alir.

Her servis kendi `SagaRabbitConfig` sinifinda exchange, queue ve binding tanimlarini yapar — servisler birbirinin konfigurasyonuna bagimli degildir.

---

### Soru 66: Elasticsearch nedir? Index, document, mapping ve analyzer kavramlari (Seviye: Orta)

Elasticsearch, dagitik bir arama ve analitik motorudur. Bu projede urun arama ve faceted search icin kullanilir.

**Index**: Benzer belgelerin depolandigi koleksiyon — iliskisel veritabanindaki tabloya karsili gelir. Bu projede `products` adinda tek bir index vardir.

**Document**: Index icerisindeki tek bir kayit — tablodaki satira karsilik gelir. JSON formatindadir. Her urun bir document'tir.

**Mapping**: Document'teki alanlarin tiplerini ve nasil indekslenecegini tanimlar — sema (schema) karsiligidir. `ProductDocument` sinifindaki `@Field` anotasyonlari mapping'i tanimlar:

```java
@Field(type = FieldType.Text, analyzer = "turkish")
private String name;    // full-text aranabilir, Turkish kokleme ile

@Field(type = FieldType.Keyword)
private String category; // tam eslesme filtresi icin (term query)

@Field(type = FieldType.Double)
private double price;    // range filtresi ve siralama icin
```

**Analyzer**: Metni indeksleme ve arama oncesinde isle. Uc adimdan olusuuur: character filter, tokenizer, token filter. `turkish` analyzer'i: "Akilli Telefonlar" -> ["akilli", "telefon"] seklinde kokleeme yapar. Bu sayede "telefon" arayan kullanici "Telefonlar" iceregini de bulur.

**`Text` vs `Keyword`**: `Text` alanlari full-text arama icin analiz edilir (kokleme, kucuk harfe cevirme). `Keyword` alanlari tam eslesme icin saklanir (aggregation, filtre, siralama). `category` alani `Keyword` cunku "elektronik" ile tam esleme araniur, kokleme istenilmez.

---

### Soru 67: Turkish analyzer nedir ve neden kullanildi? (Seviye: Orta)

`ProductDocument` sinifinde metin alanlari `analyzer = "turkish"` ile isaretlenmistir:

```java
@Field(type = FieldType.Text, analyzer = "turkish")
private String name;
```

Elasticsearch'in built-in Turkish analyzer'i su islemleri yapar:

1. **Lowercase**: "SAMSUNG" -> "samsung"
2. **Turkish-specific lowercase**: "I" -> "i" (ingilizce) yerine "I" -> "i", "I" -> "i" (Turkce). Bu, Turkce'nin buyuk I harfinin kucugunun "i" degil "i" olmasi sorununu cozer.
3. **Stemming (Kokleme)**: "telefonlar" -> "telefon", "bilgisayarlarin" -> "bilgisayar". Snowball stemmer'in Turkce versiyonu kullanilir.
4. **Stop words**: "ve", "bir", "de" gibi yaygin Turkce kelimeler indeksten cikarilir.

Bu neden onemlidir:
- Kullanici "Akilli Telefonlar" arar, urun adi "Samsung Galaxy Telefon" -> Kokleme sayesinde "telefonlar" ve "telefon" eslenir.
- "KOZMETIK" arandiginda "kozmetik" kategorisindeki urunler bulunur.
- Diacritik karakterler (c, g, i, o, s, u) dogru islenir.

Elasticsearch'in standart analyzer'i (Ingilizce) Turkce kokleme yapmaz ve "telefonlar" ile "telefon" eslemez. Bu nedenle Turkce bir e-ticaret uygulamasi icin `turkish` analyzer kullanmak zorunludur.

`fuzziness: AUTO` ise yazim hatalarina tolerans saglar: "telefoon" aramassi "telefon" sonuclarini doner. Bu, SearchService'teki multi_match sorgusunda yapilandirilir.

---

### Soru 68: Redis nedir ve LRU eviction policy nedir? (Seviye: Orta)

Redis, bellek ici (in-memory) anahtar-deger deposudur. Bu projede product-service'in cache backend'i olarak kullanilir.

docker-compose.yml'deki Redis yapilandirmasi:
```yaml
command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
```

**maxmemory 128mb**: Redis en fazla 128MB bellek kullanabilir. Bu siniira ulasildiginda eviction (tahliye) politikasi devreye girer.

**allkeys-lru**: LRU (Least Recently Used — En Az Kullanilan) politikasi. Bellek doloygunsa, en uzun suredir erisisiilmemis anahtar silinir ve yerine yeni veri yazilir.

Neden `allkeys-lru`:
1. **Cache-friendly**: Cache'in dogasi geregi eski verilerin onemini kaybetmesi normaldir. Son 30 dakikada erisiilmeyen bir urun detayi, dusuk onceliklidir.
2. **Otomatik yonetim**: Manuel cache temizleme kodu yazmaya gerek yoktur — Redis kendi kendini yonetir.
3. **Memory-safe**: Bellek asla tasimaz; her zaman 128MB sinirinda kalir.

Alternatif politikalar: `volatile-lru` (yalnizca TTL set edilmis anahtarlari tahliye eder), `allkeys-random` (rastgele tahliye), `noeviction` (bellek doluysa yeni yazma komutlarini reddeder). `allkeys-lru` cache kullanimi icin en yaygin ve en uygun politikadir.

Redis'teki veriler `GenericJackson2JsonRedisSerializer` ile JSON formatinde saklanir — bu, Redis CLI ile debug yaparken okunabilirlik saglar ve Java siniflarinin degismesine karsi daha dayaniklidir (Java serialization'in aksine).

---

### Soru 69: CI/CD nedir ve GitHub Actions bu projede nasil calisir? (Seviye: Orta)

CI (Continuous Integration), her kod degisikliginde otomatik olarak build ve test calistirmadir. CD (Continuous Deployment/Delivery), basarili build'lerin otomatik olarak deploy edilmesidir. Bu projede yalnizca CI uygulanir.

`.github/workflows/ci.yml`:

```yaml
jobs:
  backend:
    name: ${{ matrix.service }} (mvn test)
    strategy:
      fail-fast: false
      matrix:
        service:
          - auth-service
          - basket-service
          # ... 11 servis
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - name: Build & test
        working-directory: services/${{ matrix.service }}
        run: mvn -B -ntp verify

  frontend:
    steps:
      - run: npm run build   # tsc -b dahil
```

Kilit noktalar:

1. **Matrix build**: 11 servis paralel olarak build ve test edilir. `fail-fast: false` ile bir servisin basarisiz olmasi digelerini durdurmaz.
2. **Maven cache**: `cache: maven` ile dependency'ler GitHub Actions cache'inde saklanir — her run'da yeniden indirilmez.
3. **`mvn verify`**: Test suite'ini calistirir ve Spring Boot JAR paketlenmesini dogrular.
4. **Frontend type-check**: `npm run build` icinde `tsc -b` calisir — TypeScript tip hatalari pipeline'i dusurur.
5. **Concurrency group**: Ayni branch'e yeni push geldiginde eski run iptal edilir — kaynak israfini onler.

Bu CI pipeline, her push ve pull request'te calisir ve kodun derlendigini, testlerin gectigini garanti eder.

---

### Soru 70: Healthcheck nedir ve `service_healthy` condition neden onemlidir? (Seviye: Baslangic)

Healthcheck, bir container'in icerisindeki uygulamanin saglikli olup olmadigini duzenli olarak kontrol eden mekanizmadir. docker-compose.yml'de iki healthcheck ornegi:

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 10s

auth-service:
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
```

`interval`: Kontrol arasindaki sure (her 10 veya 15 saniyede bir).
`timeout`: Kontrolun en fazla ne kadar surecegi.
`retries`: Kac ardisik basarisizliktan sonra "unhealthy" olarak isaretlenecegi.
`start_period`: Uygulamanin baslamasi icin tantinan sure — bu sure icerisinde basarisizliklar sayilmaz.

`service_healthy` condition'in onemi: `depends_on` ile birlikte, bagimli servisin baslamasini saglar. Ornegin auth-service, `postgres: condition: service_healthy` ile PostgreSQL'in `pg_isready` kontrolunu gecmesini bekler. Bu olmadan auth-service, PostgreSQL henuz baglanti kabul etmeden baslar ve `Connection refused` hatasi alir.

Farkli servisler farkli healthcheck'ler kullanir:
- PostgreSQL: `pg_isready -U postgres`
- RabbitMQ: `rabbitmq-diagnostics check_port_connectivity`
- Redis: `redis-cli ping`
- Elasticsearch: `curl _cluster/health?wait_for_status=yellow`
- Spring Boot servisleri: `wget http://localhost:{port}/actuator/health`

---

### Soru 71: Redis icin neden `allkeys-lru` eviction policy secildi? (Seviye: Orta)

Redis'in eviction policy'si, bellek sinirinia ulasildiginda hangi anahtarlarin silinecegini belirler. Proje `allkeys-lru` kullanir.

Mevcut politikalar ve farklari:

| Politika | Davranis |
|----------|----------|
| `noeviction` | Yeni yazma reddedilir (hata doner) |
| `allkeys-lru` | Tum anahtarlar arasinda en az kullanilani silinir |
| `volatile-lru` | Yalnizca TTL set edilmis anahtarlardan en az kullanilani silinir |
| `allkeys-random` | Rastgele anahtar silinir |
| `volatile-ttl` | En kisa TTL'li anahtar silinir |

`allkeys-lru` secilmesinin sebebi: Bu projede Redis tamamen cache amacli kullanilir — source of truth degil. Her anahtar (urun detayi, slug esilesmesi, kategori listesi) veritabanindan yeniden olusturulabilir. LRU, en yakin zamanda erisiilmemis verileri siler — bu, cache icin dogal bir davranistir cunku sik erisiilen veriler (populer urunler) bellekte kalir, nadir erisilen veriler (eski urunler) tahliye edilir.

`noeviction` secilseydi, bellek doldugunda Redis yazma islemlerini reddederdi ve uygulama cache'e yazamazdi — bu, `CacheErrorHandler` tarafindan yakalanir ama gereksiz hata loglarina neden olurdu. `allkeys-lru` ile bu hicbir zaman olmaz.

---

### Soru 72: `GenericJackson2JsonRedisSerializer` nedir? (Seviye: Ileri)

`services/product-service/src/main/java/com/example/product/config/RedisConfig.java`:

```java
var jsonSerializer = new GenericJackson2JsonRedisSerializer();
var defaults = RedisCacheConfiguration.defaultCacheConfig()
    .serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
```

Redis, verileri byte dizisi olarak saklar. Java nesnelerini byte dizisine donusturmek icin bir serializer gereklidir. Uc seceenek vardir:

1. **JdkSerializationRedisSerializer** (varsayilan): Java'nin native serialization'ini kullanir. Dezavantajlari: (a) Redis'te okunamaz binary veri, (b) sinif adi degisikligi eski cache'i bozar, (c) Java-specifik — baska dillerden okunamaz.

2. **StringRedisSerializer**: Yalnizca String degerleri saklar — karmasik nesneler icin uygun degildir.

3. **GenericJackson2JsonRedisSerializer**: Jackson ile JSON formatinde serialiize eder. Redis'te `{"@class":"com.example.product.dto.ProductResponse","id":1,"name":"Samsung Galaxy","price":12999.99,...}` seklinde saklanir.

JSON serializer'in avantajlari:
- **Okunabilirlik**: `redis-cli GET products:byId::1` komutu ile cache icerigini dogrudan gorebilirsiniz.
- **Sinif uyumlulugu**: `@class` meta verisi sayesinde polimorfik deserialization mumkundur.
- **Dil bagigmsizligi**: Baska bir dilde yazilmis bir uygulama da ayni cache'i okuyabilir.
- **Debug kolayligi**: Production'da bir cache sorununu debug ederken JSON veriyi okumak binary veriden cok daha kolaydir.

---

### Soru 73: `spring.jpa.open-in-view: false` ne yapar? (Seviye: Ileri)

```yaml
spring:
  jpa:
    open-in-view: false
```

Open Session in View (OSIV), Spring Boot'un varsayilan olarak ACIK biraktigi tartismali bir ozelliktir. OSIV acikken, Hibernate Session (EntityManager) bir HTTP isteginint basindan sonuna kadar acik kalir — controller, template engine ve hatta view katmaninda bile lazy-loaded iliskilere erismek mumkundur.

`false` yapilmasinin sebepleri:

1. **N+1 sorgu onleme**: OSIV acikken, controller'da `order.getItems()` cagrildiginda her item icin ayri bir SQL sorgusu tetiklenir (lazy loading). Gelistirici bunu fark etmeyebilir cunku hata alinmaz — sadece performans durugur. OSIV kapatildiginda, service katmaninda `@Transactional` disinda lazy-loading denenilirse `LazyInitializationException` alinir — bu, sorunu erken tespit ettirir.

2. **Veritabani baglanti suresi**: OSIV ile veritabani baglantisi tum HTTP istegi boyunca tutulur. Agir istek yukunde bagglanti havuzu tukenebilir. Kapatildiginda baglanti yalnizca `@Transactional` metot suresi boyunca tutulur.

3. **Net katman ayirimi**: Service katmani disinda veri erisimi yapilmamasini zorlar. Bu, projedeki DTO pattern'ini destekler — entity'ler service katmaninda DTO'ya donusturulur ve controller'a DTO olarak gecer.

Projede tum servislerin `application.yml` dosyalarinda `open-in-view: false` set edilmistir. Entity iliskillerinde `fetch = FetchType.EAGER` kullanilarak lazzy loading ihtiyaci ortadan kaldirilmistir (orn. `Order.items` ve `Basket.items`).

---

### Soru 74: Micrometer nedir ve Prometheus/Jaeger'a nasil kopruler? (Seviye: Ileri)

Micrometer, JVM uygulamalari icin bir metrik ve tracing facade'idir — SLF4J'nin loglama icin yaptikini, Micrometer metrik ve tracing icin yapar. Uygulama kodu Micrometer API'sini kullanir, gercek backend (Prometheus, Datadog, Jaeger, Zipkin) yapilandirma ile belirlenir.

Bu projede Micrometer iki farkli backend'e kopru kurar:

**Metrikler: Micrometer -> Prometheus**
`micrometer-registry-prometheus` dependency'si ile Spring Boot Actuator metrikleri Prometheus formatinda export edilir. `/actuator/prometheus` endpoint'i, Prometheus'un anlayacagi formatta metrikler sunar:
```
http_server_requests_seconds_count{method="POST",uri="/api/orders/checkout",status="202"} 42
http_server_requests_seconds_sum{method="POST",uri="/api/orders/checkout",status="202"} 18.56
```

**Trace'ler: Micrometer -> OpenTelemetry -> Jaeger**
`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` dependency'leri ile Spring Web/WebClient/RabbitTemplate gibi bilesenlerin otomatik olarak ureltigi span'lar OTLP formatinda Jaeger'a gonderilir.

Micrometer'in faydasi: Uygulama kodu hicbir Prometheus veya Jaeger sinifini import etmez — yalnizca Spring Boot starter dependency'leri ve birkan yapilandirma satirii yeterlidir. Backend degistirildiginde (orn. Datadog'a gecis) uygulama kodu degismez, yalnizca dependency ve yapilandirma degisir.

---

### Soru 75: Neden Hibernate `ddl-auto: create` yerine Flyway kullanildi? (Seviye: Orta)

Hibernate'in `ddl-auto` secenekleri: `create` (her baslatmada tabloları sil-olustur), `create-drop` (kapanissta sil), `update` (farklari uygula), `validate` (yalnizca dogrula), `none` (hicbir sey yapma).

Bu projede `ddl-auto: validate` kullanilir — Hibernate yalnizca entity siniflarinin mevcut sema ile uyumlu oldugunu dogrular, semayi degistirmez. Tum sema degisiklikleri Flyway migration'lariyla yapilir.

Neden `create` veya `update` degil:

1. **Veri kaybi**: `create` her baslatmada tum tablolari siler — production'da felaket.
2. **Kontrol eksikligi**: `update` otomatik olarak sutun ekler ama hicbir zaman sutun silmez, index olusturmaz, veri migrate etmez. Karmasik sema degisiklikleri (sutun yeniden adlandirma, veri tasiima, constraint ekleme) icin yetersizdir.
3. **Tekrarlanabilirlik**: Flyway migration'lari version kontrol altindadir — hangi sema degisikliginin ne zaman uygulandigini tam olarak bilirsiniz. `update` ile sema tarihcesi yoktur.
4. **Takim calismasi**: Iki gelistirici ayni anda farkli entity degisiklikleri yaparsa `update` cakismalar cikarabilir. Flyway migration dosyalari siralanmis ve belirlidir.
5. **Seed data**: `V2__seed_products.sql` ve `V2__seed_reviews.sql` gibi migration'lar demo verileri ekler. Bu, Hibernate `ddl-auto` ile mumkun degildir.

`DemoUserSeeder` sinifi bir istisnadir — BCrypt hash runtime'da hesaplanir cunku hash icindeki salt rastgeledir. Bu sebeple kullanici seed'i SQL migration yerine Java kodu ile yapilir.

---

## Bolum 4: Frontend & Entegrasyon (Sorular 76-100)

---

### Soru 76: Neden React + Vite + TypeScript secildi? (Seviye: Baslangic)

Frontend stack'i uc teknolojiden olusur:

**React**: Bilesen (component) tabanli UI kutuphanesi. Projedeki `ProductCard`, `CategoryBar`, `FacetSidebar`, `NotificationBell` gibi yeniden kullanilabilir bilesenlerin olusturulmasini saglar. Virtual DOM ile verimli UI guncelemeleri yapar.

**Vite**: Gelistirme sunucusu ve build araci. `Create React App` (Webpack tabanli) yerine secilmistir cunku:
1. **HMR (Hot Module Replacement)**: Dosya kaydettigindede milisaniyeler icinde tarayici guncellenir — Webpack'te bu saniyeler surerdi.
2. **ESM tabanli**: Native ES modulleri kullanir, bundle'lama gerekmez — dev server aninda baslar.
3. **Proxy desteigi**: `vite.config.ts`'deki `proxy` yapilandirmassi, gelistirme ortaminda `/api` isteklerini gateway'e yonlendirir.

**TypeScript**: JavaScript'e statik tip sistemi ekler. Avantajlari:
1. Tip hatalari derleme zamaninda yakalanir — runtime `undefined is not a function` hatalari azalir.
2. API yanitlarinin tipi tanimlanir (`Product`, `Order`, `AuthTokens`) — IDE otomatik tamamlama saglar.
3. CI pipeline'inda `npm run build` ile tip kontrol edilir — tip hatalari merge'i engeller.

Bu uc teknoloji birlikte, modern, hizli ve guvenilir bir frontend gelistirme deneyimi sunar.

---

### Soru 77: Tailwind CSS nedir ve neden kullanildi? (Seviye: Baslangic)

Tailwind CSS, utility-first CSS framework'udur. Geleneksel CSS'te `.product-card { border-radius: 8px; padding: 12px; }` seklinde siniflar yazilir. Tailwind'de ise dogrudan HTML/JSX icerisinde utility siniflari kullanilir:

```jsx
<div className="bg-white rounded-lg border border-gray-200 hover:shadow-lg transition-shadow p-3">
```

Projede Tailwind kullanilmasinin sebepleri:

1. **Hizli gelistirme**: CSS dosyasi olusturmaya, sinif adlandirmaya, specificity sorunlariyla ugragmaya gerek yoktur. Bilesen icerisinde dogrudan stil yazilir.
2. **Tutarli tasarim**: Tailwind'in renk paleti, spacing olcegi ve tipografi sistemi projedeki tum bilesenlerde tutarli gorunum saglar.
3. **Kucuk bundle boyutu**: Tailwind, yalnizca kullanilan utility siniflarini final CSS'e dahil eder (tree-shaking). Projede ozel renkler de tanimlanmistir: `n11-purple`, `n11-green`, `n11-red`.
4. **Responsive tasarim**: `md:grid-cols-3 lg:grid-cols-4` gibi responsive breakpoint'ler tek satirda uygulanir.
5. **Dark mode, hover, focus**: `hover:shadow-lg`, `disabled:opacity-50` gibi durumsal stiller kolayca eklenir.

Projede ozel utility ornekleri: `line-clamp-2` (urun adini 2 satir ile sinirlar), `animate-pulse` (yuklenme skeleton animasyonu), `category-chip` (ozel kategori chip stili).

---

### Soru 78: Zustand nedir ve neden Redux yerine tercih edildi? (Seviye: Orta)

Zustand, minimalist bir React state management kutuphanesidir. Bu projede iki store vardir:

**Auth store** (`frontend/src/features/auth/store.ts`):
```typescript
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      email: null,
      setTokens: (tokens, email) => set({ accessToken: tokens.accessToken, email }),
      clear: () => set({ accessToken: null, email: null }),
      isAuthenticated: () => !!get().accessToken,
    }),
    { name: 'n11-auth' },
  ),
);
```

**Basket store** (`frontend/src/features/basket/store.ts`):
```typescript
export const useBasketStore = create<BasketState>((set) => ({
  basket: null,
  loading: false,
  setBasket: (basket) => set({ basket }),
  setLoading: (loading) => set({ loading }),
}));
```

Redux yerine Zustand secilmesinin sebepleri:

1. **Minimal boilerplate**: Redux'ta store, reducer, action creator, action type, slice tanimlari gerekir. Zustand'da bir `create()` cagrisi yeterli — yukaridaki auth store toplam 15 satir.
2. **Provider gerekmez**: Redux `<Provider store={store}>` ile uygulamayi sarmalar. Zustand hook olarak calisir — hicbir Provider gerekmez.
3. **Persist middleware**: `persist({ name: 'n11-auth' })` ile state otomatik olarak `localStorage`'a kaydedilir ve sayfa yenilendiginde geri yuklenir. Redux'ta bunu basarmak icin `redux-persist` paketi ve yapilandirmasi gerekir.
4. **Store disinda erisim**: `useAuthStore.getState().accessToken` ile React disinda (orn. `apiFetch` fonksiyonunda) store'a erisebilirsiniz. Redux'ta bu icin store'u export etmek ve `store.getState()` kullanmak gerekir.
5. **Proje boyutu**: 2 store, 5-6 alan — Redux'un karmasikligi bu olcekte gereksiz.

---

### Soru 79: `apiFetch` fonksiyonu hatalari (RFC 7807) nasil isler? (Seviye: Orta)

`frontend/src/shared/api/client.ts`:

```typescript
export async function apiFetch<T>(path: string, init: RequestInit = {}, opts = { auth: true }): Promise<T> {
  // ... header'lar ve auth token eklenir

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });

  if (res.status === 204) return undefined as T;

  if (!res.ok) {
    const problem = await parseProblem(res);
    throw new ApiError(res.status, problem?.detail ?? res.statusText, problem);
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
```

Hata isleme zinciri:

1. `apiFetch` HTTP yaniti basarisiz oldugunda (`!res.ok`), once `parseProblem()` ile yanit body'sini parse etmeye calisir.
2. `parseProblem()`, `Content-Type` header'ina bakar: `problem+json` veya `application/json` ise JSON olarak parse eder.
3. Parse edilen `ProblemDetail` nesnesi, `ApiError` exception'ina eklenir.
4. Cagrici taraf (orn. `SearchPage`) `useApi` hook'u uzerinden hatayo yakalar.
5. `errorMessage(err)` fonksiyonu, hatadan kullaniciya gosterilecek mesaji cikarir — once `problem.detail`'e, sonra `error.message`'a bakar.
6. `errorFields(err)` fonksiyonu, validasyon hatalarinda alan bazli hatalari (`fields`) cikarir.

Bu yapi, backend'in RFC 7807 formatinde dondugu tum hatalari frontend'te tutarli sekilde isle. Ornegin kayit sirasinda e-posta zaten kullaniliyorsa:
```json
{"status": 409, "detail": "Bu e-posta zaten kayitli: user@n11demo.com"}
```
Frontend bu `detail` mesajini dogrudan kullaniciya gosterir.

---

### Soru 80: `useApi` hook pattern'i nedir? (Seviye: Orta)

`frontend/src/shared/hooks/useApi.ts` dosyasi, veri cekme (data fetching) mantiktini bilesenllerden soyutlayan ozel bir React hook'dur:

```typescript
export function useApi<T>(
  fn: () => Promise<T>,
  deps: React.DependencyList = [],
  options: { enabled?: boolean } = {},
): UseApiState<T> {
```

Bu hook, her sayfanin tekrarladigi `data`, `loading`, `error` uclusunu tek bir yerde yonetir. Kullanimi:

```typescript
const { data, loading, error } = useApi(
  () => searchApi.search(parsed),
  [parsed.q, parsed.category, parsed.brand, ...],
);
```

Onemli ozellikleri:

1. **Race condition onleme**: `activeCallIdRef` ile paralel cagrilarda yalnizca en son cagrintin sonucu kabul edilir. Kullanici hizla filtre degistirdiginde eski yanitlar yok sayilir.

2. **Otomatik tetikleme**: `useEffect` ile hook mount oldugunada ve dependency'ler degistiginde otomatik olarak cagirilir.

3. **Manuel yenileme**: `refetch()` fonksiyonu ile veri yeniden cekilebilir.

4. **Enabled flag**: `{ enabled: false }` ile cagri gecici olarak durdurulabilir — ornegin kullanici giris yapmamisssa sepet verisini cekmemek icin.

5. **setData**: Optimistic update icin kullanilir — sunucu yanitini beklemeden UI'i guncellemek icin.

Bu pattern, `SWR` veya `TanStack Query` gibi kutuphanelerin basitlestirilmis bir versiyonudur. Proje boyutu icin ozel bir hook yeterlidir; agir bir kutuphaneeye gerek yoktur.

---

### Soru 81: Neden feature-based klasor yapisi kullanildi? (Seviye: Orta)

```
frontend/src/
├── app/                  # router + ErrorBoundary
├── layout/               # Navbar + Layout
├── shared/               # api, hooks, ui, utils
└── features/
    ├── auth/             # store, api, Login/RegisterPage
    ├── products/         # HomePage, ProductDetailPage, ProductCard
    ├── basket/           # store, api, BasketPage
    ├── orders/           # api, CheckoutPage, OrdersPage
    ├── search/           # SearchPage, FacetSidebar
    ├── reviews/          # ReviewList
    └── notifications/    # NotificationBell
```

Alternatif yaklasim olan **type-based** yapida dosyalar turlerine gore gruplanir: `components/`, `hooks/`, `services/`, `pages/`. Buyuk projelerde bu yaklasim sorunludur cunku bir ozellik uzerinde calisirken ilgili dosyalar farkli klasorlere dagilmis olur.

Feature-based yapinin avantajlari:

1. **Cohesion (baglasiklik)**: Auth ile ilgili tum dosyalar (`store.ts`, `api.ts`, `LoginPage.tsx`, `RegisterPage.tsx`) ayni klasordedir. Bir ozellik uzerinde calisirken tum ilgili dosyalari tek bir yerde bulursunuz.
2. **Team scalability**: Farkli gelistiriciler farkli feature klasorlerinde cakismadan calisabilir.
3. **Silinebilirlik**: Bir ozellik kaldirilacaksa, klasoru silmek yeterlidir — baska klasorlerdeki dosyalari aramaya gerek yoktur.
4. **Mikroservis uyumu**: Backend'teki mikroservis ayrimi (auth-service, product-service, order-service) ile frontend'teki feature ayrimi (auth, products, orders) dogal olarak eslesiir.

`shared/` klasoru ise tum feature'lar tarafindan kullaniilan ortak bilesenlerdir: `apiFetch`, `useApi`, `Button`, `Card`, `Spinner`, `formatTRY` gibi.

---

### Soru 82: Frontend'te JWT token yonetimi nasil calisir? (Seviye: Orta)

JWT yonetimi uc katmanda gerceklesir:

**1. Access Token (localStorage)**: Auth store'da `accessToken` olarak saklanir ve `persist` middleware ile `localStorage`'a yazilir. Her API isteginde `apiFetch` fonksiyonu bu token'i `Authorization: Bearer` header'i olarak ekler:

```typescript
if (opts.auth) {
  const token = useAuthStore.getState().accessToken;
  if (token) headers.set('Authorization', `Bearer ${token}`);
}
```

**2. Refresh Token (HttpOnly cookie)**: auth-service, refresh token'i `Set-Cookie: refresh_token=...; HttpOnly; Path=/api/auth` header'i ile gonderir. JavaScript bu cookie'ye eriiemez (XSS korumasi). `credentials: 'include'` ayari ile her `/api/auth/` isteginde tarayici bu cookie'yi otomatik olarak gonderir.

**3. Token yenileme**: Access token suresi dolugdunda (15 dakika), frontend `POST /api/auth/refresh` istegi gonderir. Refresh token cookie olarak otomatik dahil edilir. Sunucu yeni access token + yeni refresh token doner (rotation).

**Cikis**: `POST /api/auth/logout` istegi gonderilir, sunucu refresh token'i revoke eder ve cookie'yi temizler. Frontend'te `useAuthStore.getState().clear()` ile access token silinir.

Bu yaklasim, access token'in kisa omurlu (15dk) olmasi ve refresh token'in HttpOnly cookie ile korumasi ile guvenlik ile kullanicilik dengesi saglar. XSS saldirisi ile access token calginsa bile 15 dakika sonra gecersiz olur; refresh token ise JavaScript'den erisilemez.

---

### Soru 83: Basket store Zustand ile nasil calisir? (Seviye: Baslangic)

`frontend/src/features/basket/store.ts`:

```typescript
export const useBasketStore = create<BasketState>((set) => ({
  basket: null,
  loading: false,
  setBasket: (basket) => set({ basket }),
  setLoading: (loading) => set({ loading }),
}));
```

Bu store, sepet verisini global olarak yonetir. `Navbar`'daki sepet ikonu ve `BasketPage` ayni store'u okur — sepete urun eklendiginde her ikisi de otomatik guncellenir.

Auth store'dan farki: `persist` middleware kullanilMAZ. Bunun sebebi: sepet verisi sunucuda (basket-service) canonical olarak saklanir ve her sayfa acilisinda API'den cekilir. `localStorage`'a kaydetmek, sunucu ile uyumsuzluk riskii olusturur (orn. baska bir cihazda sepet degistiginde). Auth store ise `persist` kullanir cunku access token sunucuya her istekte gonderilir ve gecerliligi sunucu tarafinda dogrulanir.

Sepet akisi: `BasketPage` mount oldugunda `basketApi.get()` cagrilir, yanit `setBasket()` ile store'a yazilir, Navbar'daki urun sayisi store'dan okunur. Urun eklendiginde `basketApi.addItem()` cagrilir, sunucu yanitini `setBasket()` gunceller, tum dinleyen bilesenler otomatik re-render olur.

---

### Soru 84: React Router ve protected routing nasil calisir? (Seviye: Orta)

`frontend/src/app/router.tsx`:

```typescript
function RequireAuth({ children }: { children: JSX.Element }) {
  const authed = useAuthStore((s) => s.isAuthenticated());
  return authed ? children : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'search', element: <SearchPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'basket', element: <RequireAuth><BasketPage /></RequireAuth> },
      { path: 'checkout', element: <RequireAuth><CheckoutPage /></RequireAuth> },
      { path: 'orders', element: <RequireAuth><OrdersPage /></RequireAuth> },
    ],
  },
]);
```

`RequireAuth` bileseni, auth store'dan `isAuthenticated()` degerini kontrol eder. Kullanici giris yapmissa children'i render eder, yapmamissa `/login` sayfasina yonlendirir. `replace` parametresi, tarayici gecmisinden `/basket` giriisini kaldirir — boylece kullanici login'den sonra "geri" dugmesine bastiginda tekrar login sayfasina donmez.

Acik sayfalar: `HomePage`, `SearchPage`, `LoginPage`, `RegisterPage`, `ProductDetailPage` — kimlik dogrulama gerektirmez.

Korunmus sayfalar: `BasketPage`, `CheckoutPage`, `OrdersPage`, `OrderDetailPage` — kullanici bilgisi gerektiren islemler icerir.

React Router `createBrowserRouter` ile HTML5 History API kullanilir — URL'ler `/product/samsung-galaxy` seklinde temizdir. nginx'te `try_files $uri $uri/ /index.html` ayari, tum rotalarin `index.html`'e yonlendirilmesini saglar — boylece sayfa yenilendiginde veya dogrudan URL'e gidildiginde React Router calismaya devam eder.

---

### Soru 85: Arama sayfasinda URL neden tek dogru kaynak (single source of truth) olarak kullanildi? (Seviye: Orta)

`frontend/src/features/search/SearchPage.tsx`:

```typescript
const [params, setParams] = useSearchParams();

const parsed: SearchParams = {
  q: params.get('q') ?? undefined,
  category: params.get('category') ?? undefined,
  brand: params.get('brand') ?? undefined,
  // ...
};

const { data, loading, error } = useApi(
  () => searchApi.search(parsed),
  [parsed.q, parsed.category, parsed.brand, ...],
);
```

Filtre durumu, React state'i yerine URL query string'inde saklanir (`?q=telefon&category=elektronik&sort=price_asc`). Bu yaklasimin avantajlari:

1. **Deep link**: URL paylasilabilir — biri `?q=telefon&minPrice=500` linkini paylastiginda alici ayni sonuclari gorur.
2. **Geri tusu**: Tarayicinin geri tusu filtre gecmisinde gezinmeyi saglar. React state ile geri tusu filtreleri degistirmez.
3. **Sayfa yenileme**: F5'e basildiginda filtreler korunur. React state kaybolurdu.
4. **Tek dogru kaynak**: Filtre durumu hem URL'de hem React state'inde saklansa uyumsuzluk riski olurdu. URL tek kaynak oldugunda tutarsizlik imkansizdir.

`FacetSidebar` bileseni de ayni prensibi izler:
```typescript
const setParam = (key: string, value: string | undefined) => {
  const next = new URLSearchParams(params);
  if (value === undefined) next.delete(key);
  else next.set(key, value);
  next.delete('page'); // filtre degisikliginde sayfa sifirlanir
  setParams(next);
};
```

Bir facet tiklandiginda URL guncellenir -> `useSearchParams` degisikligi tespit eder -> `useApi` dependency'leri degisir -> yeni arama tetiklenir -> sonuclar guncellenir. Bu, tek yonlu veri akisinin (unidirectional data flow) guzel bir ornegidir.

---

### Soru 86: WebSocket STOMP nedir ve bildirimler nasil calisir? (Seviye: Ileri)

Projede gercek zamanli bildirimler WebSocket + STOMP protokolu ile gonderilir.

**WebSocket**: HTTP'nin aksine, istemci ve sunucu arasinda kalici, cift yonlu bir iletisim kanali acar. HTTP'de her istek-yanut dongusu yeni bir baglanti gerektirir; WebSocket'te tek bir baglanti uzerinden surekli veri akar.

**STOMP (Simple Text Oriented Messaging Protocol)**: WebSocket uzerinde calissan bir mesajlasma protokoludur. Mesajlara `destination`, `content-type`, `subscription` gibi yapilar ekler — raw WebSocket'in aksine yapilandirilmis mesajlasma saglar.

Backend yapilandirmasi (`notification-service`):

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
```

Frontend baglantisi (`useNotificationSocket.ts`):

```typescript
const client = new Client({
  brokerURL: wsUrl,
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    client.subscribe('/user/queue/notifications', (message) => {
      const notification: Notification = JSON.parse(message.body);
      callbackRef.current(notification);
    });
  },
});
```

Akis: Saga event'i (orn. `order.confirmed`) notification-service'e gelir -> `NotificationEventListener` bildirimi veritabanina kaydeder -> `NotificationPushService.pushToUser()` WebSocket uzerinden kullaniciya gonderir -> frontend'teki `useNotificationSocket` hook'u mesaji alir ve `NotificationBell` bilesenini gunceller.

---

### Soru 87: Frontend production'da neden nginx kullanir? (Seviye: Orta)

`frontend/Dockerfile`:

```dockerfile
FROM node:20-alpine AS builder
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

React uygulamasi, build sonrasi statik dosyalara (HTML, JS, CSS) donusur. Bu dosyalari sunmak icin bir web sunucusu gerekir. nginx bu gorev icin idealdir:

1. **Performans**: nginx, C ile yazilmis, event-driven mimarili bir web sunucusudur. Statik dosya servisi icin son derece optimize edilmistir — Node.js'in `express.static()`'inden kat kat hizlidir.

2. **API proxy**: `nginx.conf`'taki `location /api/` blogu, API isteklerini gateway'e proxy'ler. Bu, frontend ve API'nin ayni origin'den sunulmasini saglar ve CORS preflight isteklerini ortadan kaldirir.

3. **SPA fallback**: `try_files $uri $uri/ /index.html` kuralti, dosya bulunamayan tum rotalari `index.html`'e yonlendirir. Bu, React Router'in client-side routing yapmasini saglar — `/orders/42` gibi bir URL'e dogrudan gidildiginde nginx 404 donmez, `index.html`'i doner ve React Router dogru sayfayi render eder.

4. **Cache yonetimi**: HTML dosyalari `no-cache` ile sunulur (her zaman guncel), JS/CSS dosyalari 1 yil `immutable` cache ile sunulur (dosya adi hash icerdigi icin degisiklikte yeni ad alinir).

5. **Gzip**: `gzip on` ile CSS, JS, JSON dosyalari siklstirilarak gonderilir — band genisligi tasarrufu saglar.

---

### Soru 88: Vite proxy gelistirme ortaminda nasil calisir? (Seviye: Baslangic)

`frontend/vite.config.ts`:

```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
});
```

Gelistirme ortaminda (`npm run dev`), Vite dev server port 5173'te calisir. Frontend kodu `/api/products` istegi gonderdiginde, Vite bu istegi `http://localhost:8000/api/products` adresine (gateway) yonlendirir.

Bu proxy'nin amaci: Tarayici, `http://localhost:5173`'ten (frontend) `http://localhost:8000`'e (gateway) dogrudan istek gonderdiginde CORS kisitlamasina takilir cunku farkli origin'lerdir. Vite proxy'si bu sorunu cozer — istek oncelikle ayni origin'e (5173) gider, Vite sunucu tarafinda gateway'e iletir ve yaniti geri doner. Tarayici acisiindan her sey ayni origin'den geliyor gibi gorunur.

Production'da nginx ayni rolu ustlenir (`location /api/ { proxy_pass ... }`). Her iki ortamda da frontend kodu yalnizca `/api/...` seklinde relative path kullanir — ortam farki frontend kodunu etkilemez.

`changeOrigin: true` ayari, Vite'in gateway'e gonderilen isteklerdeki `Host` header'ini `localhost:8000` olarak degistirmesini saglar — bazi backend'ler `Host` header'ini kontrol eder.

---

### Soru 89: `credentials: 'include'` nedir ve neden gerekli? (Seviye: Orta)

`frontend/src/shared/api/client.ts`:

```typescript
const res = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
```

`credentials: 'include'`, tarayiciya "bu isteage cookie'leri de dahil et" der. Varsayilan olarak `fetch()` cross-origin isteklerde cookie gonderMEZ (`credentials: 'same-origin'`).

Bu projede neden gerekli:

Refresh token, `Set-Cookie: refresh_token=...; HttpOnly; Path=/api/auth; SameSite=Lax` header'i ile sunucudan tarayiciya gonderilir. Bu cookie, HttpOnly oldugu icin JavaScript ile okunamaz — yalnizca tarayicinin kendisi HTTP isteklerine dahil edebilir. Ancak tarayici, cookie'yi yalnizca `credentials: 'include'` set edildiginde gonderir.

`/api/auth/refresh` istegi gonderildiginde, tarayici `refresh_token` cookie'sini otomatik olarak ekler. Sunucu bu cookie'den refresh token'i okur, yeni token pair uretir ve geri doner. `credentials: 'include'` olmadan cookie gonderilmez ve refresh islemi basarisiz olur.

`SameSite=Lax` ayari: Cookie, yalnizca ayni site'in navigasyon isteklerinde ve GET isteklerinde gonderilir — ucuncu parti site'lerden gelen isteklerde gonderilmez (CSRF korumassi). Bu projede frontend ve API ayni origin'den sunuldugu icin sorun yoktur.

---

### Soru 90: RFC 7807 ProblemDetail yanittlari nedir? (Seviye: Orta)

RFC 7807, HTTP API'lerinde hata yanitlarinin standart formatini tanimlar. Bu standart `application/problem+json` Content-Type ile doner ve su alanlari icerir:

| Alan | Aciklama | Ornek |
|------|----------|-------|
| `type` | Hata tipi URI'si | `about:blank` |
| `title` | Kisaa baslik | `Bad Request` |
| `status` | HTTP status kodu | `400` |
| `detail` | Detayli aciklama | `Dogrulama hatasi` |
| `instance` | Hata olusan endpoint | `/api/auth/register` |

Projede eklenen ozel alanlar:
- `timestamp` — hatanin olustugu zaman
- `fields` — validasyon hatalarinda alan bazli mesajlar
- `retryAfterSeconds` — rate limit durumunda bekleme suresi

Frontend tarafinda `ProblemDetail` tipi tanimlidir ve `apiFetch` fonksiyonu hata yanitlarini otomatik olarak bu tiple parse eder. `errorMessage()` fonksiyonu `detail` alanini, `errorFields()` fonksiyonu `fields` alanini cikarir.

Bu standardin faydasi: hem basit hatalar ("Urun bulunamadi") hem de karmasik hatalar (alan bazli validasyon hatalari) ayni yapiyla doner. Frontend tek bir hata isleme mantigi ile tum servislerin tum hatalarini ele alir.

---

### Soru 91: Faceted search sidebar, Elasticsearch aggregations ile nasil calisir? (Seviye: Ileri)

`SearchService.addAggregations()` metodu, her arama sorgusuna uc aggregation ekler:

```java
qb.withAggregation("brands", Aggregation.of(a -> a.terms(t -> t.field("brand").size(30))));
qb.withAggregation("categories", Aggregation.of(a -> a.terms(t -> t.field("category").size(30))));
qb.withAggregation("price_min", Aggregation.of(a -> a.min(m -> m.field("discountedPrice"))));
qb.withAggregation("price_max", Aggregation.of(a -> a.max(m -> m.field("discountedPrice"))));
```

Bu aggregation'lar, sorgu sonuclari uzerinde istatistikler hesaplar:
- `brands`: Her markanin kac urun icerdigi (orn. Samsung: 12, Apple: 8)
- `categories`: Her kategorinin kac urun icerdigi
- `price_min/max`: Sonuclardaki en dusuk ve en yuksek fiyat

`SearchResponse.Facets` record'u bu bilgileri JSON olarak frontend'e doner:

```java
public record Facets(
    Map<String, Long> brands,
    Map<String, Long> categories,
    PriceStats price
) {}
```

Frontend'teki `FacetSidebar` bileseni bu verileri render eder:

```typescript
{categoryEntries.map(([name, count]) => (
  <label key={name}>
    <input type="radio" checked={activeCategory === name}
           onChange={() => setParam('category', ...)} />
    <span>{name}</span>
    <span>{count}</span>  {/* Kategori basi urun sayisi */}
  </label>
))}
```

Onemli nokta: facet sayilari mevcut filtreleri yansitir. "elektronik" kategorisini sectikten sonra brands aggregation'u yalnizca elektronik kategorisindeki markalari ve sayilarini gosterir — boylece kullanici hangi filtrelerin sonuc dondurdugunu gorur.

---

### Soru 92: Sayfalama (pagination) frontend'te nasil ele aliniyor? (Seviye: Baslangic)

Backend, `Page<T>` formatinda sayfalama bilgisi doner: `content` (urunler), `totalElements` (toplam urun sayisi), `totalPages` (toplam sayfa sayisi), `page` (mevcut sayfa), `size` (sayfa boyutu).

Frontend'te `SearchPage` bileseni:

```typescript
{data && data.totalPages > 1 && (
  <div className="flex items-center justify-center gap-2">
    <button onClick={() => setPage(Math.max(0, (parsed.page ?? 0) - 1))}
            disabled={(parsed.page ?? 0) === 0}>
      ← Onceki
    </button>
    <span>Sayfa {data.page + 1} / {data.totalPages}</span>
    <button onClick={() => setPage(Math.min(data.totalPages - 1, (parsed.page ?? 0) + 1))}
            disabled={(parsed.page ?? 0) >= data.totalPages - 1}>
      Sonraki →
    </button>
  </div>
)}
```

Sayfa degisikligi URL query string'ine yazilir (`?page=2`), `useApi` hook'u bu degisikligi tespit eder ve yeni veri ceker. Bu yaklasim sayesinde:
- Sayfa numarasi URL'de gorunur ve payllasilabilir
- Tarayici geri tusu onceki sayfaya doner
- F5 ile yenilendiginde ayni sayfa gosterilir

`disabled` attribute'u ilk sayfada "Onceki" dugmesini, son sayfada "Sonraki" dugmesini devre disi birakir. `Math.max(0, ...)` ve `Math.min(totalPages - 1, ...)` ile gecersiz sayfa numaralaari onlenir.

---

### Soru 93: `formatTRY` helper fonksiyonu neden kullanildi? (Seviye: Baslangic)

`frontend/src/shared/utils/format.ts`:

```typescript
const TRY_FORMATTER = new Intl.NumberFormat('tr-TR', {
  style: 'currency',
  currency: 'TRY',
  maximumFractionDigits: 2,
});

export const formatTRY = (n: number): string => TRY_FORMATTER.format(n);
```

Bu fonksiyonun varlik sebebi:

1. **Tutarlilik**: Projedeki 10+ yerde fiyat gosteriliir (ProductCard, SearchResultCard, BasketPage, OrdersPage, CheckoutPage, FacetSidebar). Her birinde `n.toLocaleString('tr-TR', ...)` yazmak yerine tek bir `formatTRY(price)` cagrisi kullanilir. Format degisikligi gerektiginde tek bir yer duzenlenir.

2. **Dogru Turkce formatlama**: `Intl.NumberFormat` ile `12999.99` degeri "12.999,99 TL" olarak formatlanir — Turkce binlik ayirici (nokta), ondalik ayirici (virgul) ve para birimi kullanilir.

3. **Singleton formatter**: `TRY_FORMATTER` modul seviyesinde bir kez olusturulur ve yeniden kullanilir. Her caggirida yeni `Intl.NumberFormat` olusturmak gereksiz maliyet olurdu.

Inline formatlama yerine helper kullanmak `formatDateTime` icin de uygulanir — tarih/saat gosterimi de Turkce lokalizasyonla tek bir yerden yonetilir.

---

### Soru 94: Gorsel lazy loading (`loading="lazy"`) neden kullanildi? (Seviye: Baslangic)

`frontend/src/features/products/ProductCard.tsx`:

```typescript
<img src={product.imageUrl} alt={product.name} loading="lazy" ... />
```

`loading="lazy"` HTML attribute'u, tarayiciya "bu gorseli hemen yukleme, kullanici goruntuleme alanina (viewport) yaklastiginda yukle" der. Bu, native lazy loading olarak bilinir.

Neden onemlidir:

1. **Ilk yukleme performanssi**: Urun listesi 24 urun gosterir. `loading="lazy"` olmadan tum 24 gorsel aninda yuklenir — ekranda yalnizca 8-12 tanesi gorunse bile. Lazy loading ile yalnizca gorunen gorseller yuklenir, asagidakiler scroll edildiginde yuklenir.

2. **Bant genisligi tasarrufu**: Kullanici sayfayi asagi scroll etmezse alt kisimdaki gorseller hic yuklenmez — mobil kullanicilar icin ozellikle onemlidir.

3. **Algilanan hiz**: Sayfa daha hizli `interactive` duruma gecer cunku tarayici daha az kaynak indirir.

4. **Sifir JavaScript**: `loading="lazy"` native tarayici ozelligidir — Intersection Observer veya scroll event listener yazmaya gerek yoktur.

Bu ozellik tum modern tarayicilarda desteklenir (Chrome, Firefox, Safari, Edge). Eski tarayicilarda `loading` attribute'u yok sayilir ve gorsel normal olarak yuklenir — graceful degradation.

---

### Soru 95: Toast bildirim sistemi nasil calisir? (Seviye: Orta)

`frontend/src/shared/providers/ToastProvider.tsx`:

```typescript
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const toast = useCallback((type: ToastType, message: string) => {
    const id = nextId.current++;
    setToasts((list) => [...list, { id, type, message }]);
    window.setTimeout(() => {
      setToasts((list) => list.filter((t) => t.id !== id));
    }, 4000);
  }, []);
```

Toast sistemi React Context + Provider pattern'i ile calisiir:

1. `ToastProvider` uygulamanin en ustunde render edilir ve tum bilesen agacina `toast()` fonksiyonunu saglar.
2. Herhangi bir bilesen `useToast()` hook'u ile toast context'ine erisir.
3. `toast.success("Urun sepete eklendi")` cagrildiginda yeni bir toast state'e eklenir.
4. Toast, ekranin sag altinda animasyonlu olarak gorunur.
5. 4 saniye sonra `setTimeout` ile otomatik olarak kaldirilir.
6. Her toast benzersiz bir `id`'ye sahiptir — dogru toast'un silinmesini garanti eder.

Uc toast tipi vardir:
- `success`: Yesil arka plan (basarili islem)
- `error`: Kirmizi arka plan (hata)
- `info`: Mavi arka plan (bilgi)

Tailwind siniflari ile toast gorsel olarak stillendirilir:
```typescript
t.type === 'success' ? 'bg-green-50 border-green-200 text-green-800' : ...
```

Bu, kendi toast sistemini yazmak yerine bir kutuphane (react-toastify gibi) kullanmak yerine secinten hafif bir yaklasimdir — proje boyutuna uygundur.

---

### Soru 96: React'te ErrorBoundary nedir? (Seviye: Orta)

`frontend/src/app/ErrorBoundary.tsx`:

```typescript
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught', error, info);
  }

  render() {
    if (this.state.error) {
      return (
        <div>
          <h1>Bir seyler ters gitti</h1>
          <p>{this.state.error.message}</p>
          <button onClick={() => window.location.reload()}>Sayfayi yenile</button>
        </div>
      );
    }
    return this.props.children;
  }
}
```

ErrorBoundary, React'in hata siniri mekanizmasidir. Bir bilesen render sirasinda hata firlatirsa, tum uygulama beyaz ekrana doner (white screen of death). ErrorBoundary bu hatayi yakalar ve kullaniciya anlamli bir hata mesaji gosterir.

Neden class component: React, error boundary islevselligini yalnizca class component'lerde destekler — `getDerivedStateFromError` ve `componentDidCatch` lifecycle metodlari fonksiyonel component'lerde kullanilaszmaz (React 19'a kadar).

ErrorBoundary'nin yakaladigi hatalar: render sirasinda olusan hatalar (orn. `undefined.map()`, tip hatalari). YAKALAMADIIGI hatalar: event handler'lardaki hatalar, async kodundaki hatalar (bunlar try-catch ile yakalanir) ve ErrorBoundary'nin kendisindeki hatalar.

Projede `App` bileseni ErrorBoundary ile sarmalanir — uygulamanin herhangi bir yerinde beklenmeyen bir render hatasi olusa, kullanici "Bir seyler ters gitti" mesajini gorur ve sayfayi yenileme dugmesini kullanabilir.

---

### Soru 97: `line-clamp-2` neden urun adlari icin kullanildi? (Seviye: Baslangic)

`frontend/src/features/products/ProductCard.tsx`:

```typescript
<div className="text-sm text-gray-900 line-clamp-2 min-h-[2.5rem]">{product.name}</div>
```

`line-clamp-2`, CSS ile metni 2 satirla sinirlar ve tashan kismi "..." ile keser. Bu, Tailwind CSS'in bir utility sinifidir ve arka planda su CSS'i uygular:

```css
display: -webkit-box;
-webkit-line-clamp: 2;
-webkit-box-orient: vertical;
overflow: hidden;
```

Neden gereklidir:

1. **Gorsel tutarlilik**: Urun grid'inde her kart ayni yukseklikte olmalidir. Bir urun adi 5 kelime, digeri 25 kelime olabilir — `line-clamp-2` ile tum kartlar ayni metin yuksekligine sahip olur.

2. **Layout bozulmassi onleme**: Uzun urun adlari olmadan kartlar farkli yuksekliklerde olur ve grid duzenini bozar. `min-h-[2.5rem]` ile minimum yukseklik de set edilerek kisa adlarin da ayni alanl kaplamassi saglanir.

3. **Kullanici deneyimi**: Kullanici, kisa bir onizlemede urun adini gorebilir ve detay icin tiklar. Tum adi gostermeye gerek yoktur.

Benzer yaklasim e-ticaret sitelerinde (Amazon, n11, Trendyol) standarttir — urun kartlarinda baslik her zaman sinirlidir.

---

### Soru 98: CategoryBar bileseni URL parametreleri ile nasil calisir? (Seviye: Orta)

`frontend/src/features/products/CategoryBar.tsx`:

```typescript
export function CategoryBar({ categories }: { categories: Category[] }) {
  const [params] = useSearchParams();
  const active = params.get('category');
  return (
    <div className="flex gap-2 overflow-x-auto">
      <Link to="/search" className={`... ${!active ? 'ring-2 ring-n11-purple' : ''}`}>
        Tumu
      </Link>
      {categories.map((c) => (
        <Link key={c.slug} to={`/search?category=${c.slug}`}
              className={`... ${active === c.slug ? 'ring-2 ring-n11-purple' : ''}`}>
          {c.icon} {c.name}
        </Link>
      ))}
    </div>
  );
}
```

CategoryBar, her kategori icin `/search?category=elektronik` seklinde URL link'leri olusturur. Aktif kategori URL'den (`useSearchParams`) okunarak vurgulanir.

Kilit tasarim kararlari:

1. **`/search` rotasina yonlendirme**: Kategori tikllamasi, ana sayfadaki basit PostgreSQL filtrelemesi yerine Elasticsearch uzerinden calisan `/search` sayfasina gider. Boylece faceted search, fuzzy matching ve aggregation ozellikleri kullanilir.

2. **URL-driven**: Aktif kategori state'te degil URL'de saklanir. Sayfa yenileme, geri tusu ve deep link dogal olarak calisir.

3. **Yatay scroll**: `overflow-x-auto` ile 8 kategori chip'i yatay olarak kaydiirilabilir — ozellikle mobil cihazlarda onemlidir.

4. **Gorsel geri bildirim**: Aktif kategori `ring-2 ring-n11-purple` ile mor cizgiyle vurgulanir — kullanici hangi kategoride oldugunu gorebilir.

---

### Soru 99: CORS preflight nedir ve bu projede neden gerekli degil? (Seviye: Ileri)

CORS preflight, tarayicinin gercek istekten once gonderdiigi bir OPTIONS istegiidir. Tarayici, sunucunun bu cross-origin istegge izin verip vermedigini kontrol eder. Preflight su durumlarda tetiklenir:

- HTTP metodu GET/HEAD/POST disindaysa (orn. PUT, DELETE, PATCH)
- `Content-Type` header'i `application/json` ise (simple request degil)
- Custom header'lar varsa (orn. `Authorization`)

Bu projede her API isteginde `Authorization: Bearer <token>` header'i ve `Content-Type: application/json` kullanilir — normalde bu, her istekte ek bir OPTIONS preflight istegi tetiklerdi.

Ancak projede CORS preflight ortadan kaldirilmistir. Bunun sebebi: **same-origin proxy**.

**Production'da**: nginx, `/api/` isteklerini gateway'e proxy'ler. Tarayici acisindarn istek `http://localhost:13000/api/products`'dir ve sayfa da `http://localhost:13000`'den yuklenmistir — ayni origin. Ayni origin'de CORS kisitlamasi yoktur.

**Development'ta**: Vite dev server, `/api` isteklerini `http://localhost:8000`'e proxy'ler. Tarayici acisindarn istek `http://localhost:5173/api/products`'dir — yine ayni origin.

Bu yaklasim her API isteginde 1 yerine 2 HTTP istegi yapilmasini onler — ozellikle arama sayfasinda her filtre degisikliginde yeni istek tetikledigi icin performans kazanci onemlidir. Gateway'deki global CORS yapilandirmasi (`allowedOriginPatterns: "*"`) yine vardir — dogrudan gateway'e erisim durumunda (orn. Swagger UI) CORS sorun cikmaz.

---

### Soru 100: Tarayicidan veritabanina ve geri: tam istek akisi nasil calisir? (Seviye: Ileri)

Bir kullanicinin urun arama yaptigini varsayalim. Akis su adimlarla gerceklesir:

**1. Tarayici (React)**
Kullanici arama kutusuna "telefon" yazar ve Enter'a basar. React Router URL'i `http://localhost:13000/search?q=telefon` olarak gunceller. `SearchPage` bileseni `useSearchParams` ile `q=telefon`'u okur. `useApi` hook'u `searchApi.search({ q: "telefon", ... })` cagiirir.

**2. Frontend HTTP istegi**
`apiFetch('/search?q=telefon&page=0&size=24')` cagirilir. Auth store'dan JWT token alinir ve `Authorization: Bearer <token>` header'ina eklenir. `credentials: 'include'` ile cookie'ler dahil edilir.

**3. nginx proxy**
`http://localhost:13000/api/search?q=telefon` istegi, nginx'in `location /api/` blogu tarafindan `http://api-gateway:8000/api/search?q=telefon` adresine proxy'lenir.

**4. API Gateway**
Spring Cloud Gateway, `Path=/api/search/**` kuralini eslestirir ve `lb://search-service`'e yonlendirir. Eureka'dan search-service'in IP:port bilgisini alir (orn. `172.18.0.12:8087`). `RequestLoggingGlobalFilter` correlation ID uretir. Tracing header'lari (traceparent) eklenir.

**5. search-service**
`RequestLoggingFilter` correlation ID'yi MDC'ye koyar ve istegi loglar. `SearchController.search()` metodu parametreleri alir. `SearchService.doSearch()` Elasticsearch sorgusunu olusturur: multi_match query + aggregations.

**6. Elasticsearch**
NativeQuery, ES'e HTTP uzerinden gonderilir. ES, `products` index'inde Turkish analyzer ile "telefon" kokune eslesen belgeleri bulur, fuzziness ile yazim hatalarini tolere eder, `name^3` agirligi ile ilgili sonuclari siralar. Ayni anda brands, categories ve price aggregation'larini hesaplar.

**7. Geri donus**
ES sonuclari `SearchHits<ProductDocument>` olarak doner -> `SearchService` bunlari `SearchResponse` record'una donusturur (icerik + facets) -> `SearchController` JSON olarak doner -> Gateway yaniti frontend'e iletir -> nginx proxy yaniti tarayiciya doner.

**8. Frontend render**
`useApi` hook'u, gelen veriyi `data` state'ine yazar. `SearchPage` bileseni re-render olur: urun grid'i olusturulur (`SearchResultCard`), sol panelde facet'ler guncellenir (`FacetSidebar`), sayfalama kontrolleri gosterilir. Tum bu akis, kullanicinin "telefon" yazmasindan sonucilarin ekranda gormesine kadar tipik olarak 200-500ms surer.

Bu akis sirasinda Prometheus metrikleri toplanir, Jaeger'da trace span'lari olusturulur ve RequestLoggingFilter'lar her serviste log yazar — tum bunlar Grafana uzerinden goruntulenir.
