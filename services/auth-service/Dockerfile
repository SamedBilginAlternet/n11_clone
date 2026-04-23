# syntax=docker/dockerfile:1.6

# ── Stage 1: Dependencies (cached layer) ─────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS deps

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository mvn dependency:go-offline -q

# ── Stage 2: Build ────────────────────────────────────────────────────────────
FROM deps AS builder

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn package -DskipTests -q

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
