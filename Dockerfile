# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Runtime (Eclipse Temurin 17 JRE)
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Copy built JAR from build stage
COPY --from=builder /app/build/libs/*-all.jar ./rtg.jar

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar rtg.jar"]