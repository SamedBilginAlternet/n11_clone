# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

FROM deps AS builder
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER app
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
