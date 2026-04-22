FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q

FROM deps AS builder
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
