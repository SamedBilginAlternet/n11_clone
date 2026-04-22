# ── Stage 1: Dependencies (cached layer) ─────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS deps

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q

# ── Stage 2: Test ─────────────────────────────────────────────────────────────
FROM deps AS test

COPY src ./src
# Uses application-test.yml (H2 in-memory) — no external DB needed
RUN mvn test -Dspring.profiles.active=test

# ── Stage 3: Build ────────────────────────────────────────────────────────────
FROM deps AS builder

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 4: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
