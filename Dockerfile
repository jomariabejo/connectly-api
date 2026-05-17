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

# Create non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Set JVM memory limits and enable GC logging
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Profile from SPRING_PROFILES_ACTIVE (set in docker-compose)
ENTRYPOINT ["java", "-jar", "app.jar"]
