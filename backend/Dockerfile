# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Instalar dependencias
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-spa \
    tesseract-ocr-data-eng \
    ghostscript \
    wget \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Usuario no root
RUN addgroup -S smartfinance && adduser -S smartfinance -G smartfinance

# Copiar jar
COPY --from=builder /app/target/finance-0.0.1-SNAPSHOT.jar /app/app.jar

# Permisos
RUN chown smartfinance:smartfinance /app/app.jar

USER smartfinance

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
