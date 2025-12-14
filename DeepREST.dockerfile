# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS java-builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Runtime (Stable Baslines 3 Python + RestTestGen Java)
FROM python:3.12-slim-bookworm AS runtime

WORKDIR /app

# Copy built JAR from build stage
COPY --from=java-builder /app/build/libs/*-all.jar ./rtg.jar

# Copy Python source from host
COPY ./src/main/python/deeprest /app

RUN apt-get update && apt-get install -y openjdk-17-jre-headless && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir -r requirements.txt

RUN chmod +x entrypoint.sh

RUN mkfifo /app/j2p /app/p2j

ENTRYPOINT ["./entrypoint.sh"]