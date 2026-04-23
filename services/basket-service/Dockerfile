# syntax=docker/dockerfile:1.6

# --- deps: pre-cache maven deps
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository mvn -B -q dependency:go-offline

# --- builder: package jar
FROM deps AS builder
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn -B -q -DskipTests package

# --- runtime: small non-root image
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
