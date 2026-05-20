# Multi-stage Dockerfile for Connectly API
# Stage 1: Build
FROM gradle:8.13-jdk17 AS builder

WORKDIR /build

# Copy build files
COPY build.gradle settings.gradle gradle* ./
COPY gradle gradle/

# Copy source code
COPY src src/

# Build the application
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17.0.11_9-jre-alpine

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy JAR from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

COPY render-entrypoint.sh /app/render-entrypoint.sh
RUN chmod +x /app/render-entrypoint.sh

# Create non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser \
    && chown appuser:appuser /app/app.jar /app/render-entrypoint.sh
USER appuser

# Render sets PORT; local Docker defaults to 8080
EXPOSE 8080

# Health check (PORT is set on Render; default 8080 for local compose)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "curl -f \"http://127.0.0.1:$${PORT:-8080}/api/actuator/health\" || exit 1"]

# Set JVM memory limits and enable GC logging
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# DATABASE_URL (Render) is converted to JDBC in render-entrypoint.sh; optional args e.g. --server.port=$PORT
ENTRYPOINT ["/app/render-entrypoint.sh"]
