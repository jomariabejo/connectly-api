# Connectly API - Deployment Guide

**Status:** 🚀 Phase 1 Complete (Local Foundation)

This guide covers deploying the Connectly API to Datadog with multi-environment support (stage, demo, prod) using Docker, Docker Compose, GitHub Actions, and DigitalOcean.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Phase 1: Local Development Setup](#phase-1-local-development-setup) — see also [Local Development](../getting-started/local-development.md)
3. [Phase 2: Datadog Integration](#phase-2-datadog-integration)
4. [Phase 3: GitHub Actions CI/CD](#phase-3-github-actions-cicd)
5. [Phase 4: DigitalOcean Infrastructure](#phase-4-digitalocean-infrastructure)
6. [Phase 5: Monitoring & Alerts](#phase-5-monitoring--alerts)
7. [Troubleshooting](#troubleshooting)
8. [DevOps Best Practices](#devops-best-practices)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                          GitHub Repository                       │
│  ├─ source code (Java/Spring Boot)                              │
│  ├─ Dockerfile (multi-stage build)                              │
│  ├─ docker-compose.yml (local orchestration)                    │
│  └─ .github/workflows/ (CI/CD pipelines)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ (Git push)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GitHub Actions (CI/CD)                        │
│  ├─ build-and-test.yml (compile, unit tests)                   │
│  ├─ build-docker.yml (push to ghcr.io)                         │
│  └─ deploy.yml (deploy to DigitalOcean)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ (Docker image)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              DigitalOcean (Single Droplet)                       │
│  ├─ Stage Environment (port 8081)                              │
│  │  ├─ api-stage container                                    │
│  │  ├─ postgres-stage (port 5433)                            │
│  │  └─ [Spring Profile: stage]                               │
│  ├─ Demo Environment (port 8082)                              │
│  │  ├─ api-demo container                                    │
│  │  ├─ postgres-demo (port 5434)                            │
│  │  └─ [Spring Profile: demo]                                │
│  ├─ Prod Environment (port 8083)                              │
│  │  ├─ api-prod container                                    │
│  │  ├─ postgres-prod (port 5435)                            │
│  │  └─ [Spring Profile: prod]                                │
│  │                                                             │
│  └─ Datadog Agent (APM + Logs)                                │
│     ├─ Listens on port 8126 (APM traces)                     │
│     └─ Collects logs and metrics                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ (Traces, Metrics, Logs)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Datadog (Monitoring)                          │
│  ├─ APM Traces (request flows, latency)                         │
│  ├─ Metrics (JVM, business metrics)                             │
│  ├─ Logs (structured JSON logs)                                 │
│  ├─ Dashboards (custom visualizations)                          │
│  └─ Alerts (Slack, PagerDuty)                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Local Development Setup

**Full instructions:** [Local Development](../getting-started/local-development.md) (minimal `bootRun` + Docker Compose, verification, troubleshooting).

### ✅ Completed Files

| File | Purpose |
|------|---------|
| [Local Development](../getting-started/local-development.md) | Local run guide (`bootRun` + Docker Compose) |
| `src/main/resources/application-dev.properties` | Local development profile |
| `src/main/resources/application-stage.properties` | Staging environment config |
| `src/main/resources/application-demo.properties` | Demo environment config |
| `src/main/resources/application-prod.properties` | Production environment config |
| `build.gradle` | Actuator, Micrometer, Prometheus |
| `Dockerfile` | Multi-stage build (Gradle → Alpine JRE) |
| `docker-compose.yml` | Orchestrates 3 environments locally |
| `.dockerignore` | Optimizes Docker build context |
| `.env.example` | Template for environment variables |

### Quick reference

**Minimal (single dev environment):**

```bash
# Requires PostgreSQL on localhost:5432, database connectly_db
./gradlew bootRun
# → http://localhost:8080/api/v1/public/hello
```

**Full stack (stage + demo + prod in Docker):**

```bash
docker compose up -d --build
docker compose wait api-stage api-demo api-prod   # or: sleep 35
curl -s http://localhost:8081/api/actuator/health  # stage
```

| Environment | API URL | DB port (host) |
|-------------|---------|----------------|
| **dev** (`bootRun`) | http://localhost:8080/api | 5432 (local Postgres) |
| **stage** | http://localhost:8081/api | 5433 |
| **demo** | http://localhost:8082/api | 5434 |
| **prod** (local) | http://localhost:8083/api | 5435 |

See [Local Development](../getting-started/local-development.md) for prerequisites, Mailpit, logs, and [Debugging](../getting-started/debugging.md) for troubleshooting.

---

## Database Initialization

Each Docker environment has its own PostgreSQL container and volume. On **first** volume creation, `schema.sql` is loaded via `docker-entrypoint-initdb.d`. Compose sets `SPRING_JPA_HIBERNATE_DDL_AUTO=update` so the schema can stay aligned with JPA entities during local testing.

For `./gradlew bootRun`, the `dev` profile uses Hibernate `create-drop` against `connectly_db` on localhost.

```bash
# Connect to stage database
docker exec -it connectly-postgres-stage psql -U postgres -d connectly_db_stage

\dt
\q
```

---

## Phase 2: Datadog Integration

### 📋 Overview

Phase 2 adds **Datadog APM tracing, metrics, and structured logging**:

| Component | File | Status |
|-----------|------|--------|
| Actuator Endpoints | `build.gradle` | ✅ Added |
| Micrometer Prometheus | `build.gradle` | ✅ Added |
| Micrometer Datadog | `build.gradle` | ✅ Added |
| Structured Logging | `src/main/resources/logback-spring.xml` | ✅ Created |
| Metrics Configuration | `MetricsConfiguration.java` | ✅ Created |

### Setup Datadog Agent Locally

For local development with Datadog, modify `docker-compose.yml` to add Datadog agent:

```yaml
datadog:
  image: gcr.io/datadog-api/agent:latest
  container_name: connectly-datadog-agent
  environment:
    DD_API_KEY: ${DD_API_KEY}  # Add your Datadog API key
    DD_SITE: datadoghq.com      # Change to datadoghq.eu if EU
    DD_LOGS_ENABLED: "true"
    DD_APM_ENABLED: "true"
    DD_AGENT_HOST: datadog
  ports:
    - "8126:8126"  # APM traces
    - "8125:8125"  # Metrics
  networks:
    - connectly-stage-network
    - connectly-demo-network
    - connectly-prod-network
    - connectly-mail-network
```

### Enable Datadog Metrics in `application-stage.properties`

```properties
# Datadog APM Configuration
management.metrics.export.datadog.enabled=true
management.metrics.export.datadog.api-key=${DD_API_KEY}
management.metrics.export.datadog.application-key=${DD_APP_KEY}
management.metrics.tags.service=connectly-api
management.metrics.tags.env=stage
```

### Custom Metrics Example

Add to any `@Service` class:

```java
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final MeterRegistry meterRegistry;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public Order createOrder(Order order) {
        // Create order...
        
        // Increment custom metric
        meterRegistry.counter("connectly.orders.created", 
            "environment", "stage").increment();
        
        return order;
    }
}
```

Then query in Datadog dashboard:
```
avg:connectly.orders.created{service:connectly-api}
```

---

## Phase 3: GitHub Actions CI/CD

### 📋 Workflows to Create

Three GitHub Actions workflows automate the entire deployment pipeline:

#### 1. **build-and-test.yml** (Compile & Test)

```yaml
name: Build and Test

on:
  push:
    branches: [ main, stage, demo, develop ]
  pull_request:
    branches: [ main, stage, demo ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      - run: ./gradlew clean build
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/test-results/
```

#### 2. **build-docker.yml** (Build & Push Image)

```yaml
name: Build Docker Image

on:
  push:
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository }}:${{ github.ref_name }}
```

#### 3. **deploy.yml** (Deploy to DigitalOcean)

```yaml
name: Deploy

on:
  push:
    branches: [ stage ]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        default: 'stage'
        type: choice
        options:
          - stage
          - demo
          - prod

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy via SSH
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.DIGITALOCEAN_HOST }}
          username: root
          key: ${{ secrets.DIGITALOCEAN_SSH_KEY }}
          script: |
            cd /opt/connectly
            docker-compose pull
            docker-compose up -d
            sleep 10
            curl http://localhost:8081/api/actuator/health || exit 1
```

### Add GitHub Secrets

In GitHub repo → Settings → Secrets → New repository secret:

| Secret | Value |
|--------|-------|
| `DIGITALOCEAN_HOST` | Your droplet IP (e.g., `123.45.67.89`) |
| `DIGITALOCEAN_SSH_KEY` | Private SSH key for DigitalOcean droplet |
| `DD_API_KEY` | Datadog API key (for production monitoring) |

---

## Phase 4: DigitalOcean Infrastructure

### 📋 One-Time Setup

#### 1. Get GitHub Education Pack Credit

Visit: https://education.github.com/pack → DigitalOcean → Claim $100 credit

#### 2. Create DigitalOcean Droplet

```bash
# Configuration:
# - Image: Docker (pre-installed)
# - Size: 2GB RAM / 1 vCPU ($12/month)
# - Region: closest to you (e.g., $nyc3)
# - Backups: Enable (recommended)
```

#### 3. SSH into Droplet

```bash
# From your local machine
ssh -i ~/.ssh/id_rsa root@YOUR_DROPLET_IP

# Test connection
curl https://api.digitalocean.com/v2/account -H "Authorization: Bearer $TOKEN" | jq .
```

#### 4. Setup Deployment Directories

```bash
# On DigitalOcean droplet
mkdir -p /opt/connectly/{stage,demo,prod}

# Create environment files
cat > /opt/connectly/.env.stage << EOF
SPRING_PROFILE=stage
DB_PASSWORD=your-secure-password
JWT_SECRET=$(openssl rand -base64 32)
MAIL_HOST=mailpit
MAIL_PORT=1025
EOF

# Repeat for demo and prod...
chmod 600 /opt/connectly/.env.*
```

#### 5. Configure SSH Key for GitHub Actions

```bash
# Generate new SSH key for GitHub Actions
ssh-keygen -t rsa -b 4096 -f /root/.ssh/github_deploy -C "github-actions"

# Add public key to authorized_keys
cat /root/.ssh/github_deploy.pub >> /root/.ssh/authorized_keys

# Copy private key to GitHub Secrets (DIGITALOCEAN_SSH_KEY)
cat /root/.ssh/github_deploy
```

#### 6. Deploy Application

```bash
# Copy docker-compose to droplet
scp docker-compose.yml root@YOUR_DROPLET_IP:/opt/connectly/

# SSH in and start services
ssh root@YOUR_DROPLET_IP
cd /opt/connectly
docker-compose up -d

# Verify
docker-compose ps
curl http://localhost:8081/api/actuator/health
```

---

## Phase 5: Monitoring & Alerts

### Datadog Dashboards

#### Create Dashboard: Application Performance

```json
{
  "title": "Connectly API - Performance",
  "widgets": [
    {
      "type": "timeseries",
      "queries": [
        {
          "metric": "trace.web.request.duration",
          "aggregator": "p99"
        }
      ],
      "title": "Request Latency (p99)"
    },
    {
      "type": "query_value",
      "queries": [
        {
          "metric": "trace.web.request.errors",
          "aggregator": "sum",
          "filters": ["service:connectly-api"]
        }
      ],
      "title": "Error Count (24h)"
    }
  ]
}
```

#### Create Alerts

```bash
# Alert: High Error Rate
Threshold: trace.web.request.errors{service:connectly-api} > 5% over 5 minutes
Notification: Slack @team #alerts

# Alert: High Latency
Threshold: trace.web.request.duration{service:connectly-api,resource_name:POST /api/orders} > 1000ms over 5 minutes
Notification: Slack @devops
```

---

## Troubleshooting

### Issue: Containers won't start

```bash
# Check logs
docker-compose logs api-stage

# Common causes:
# 1. Port already in use: lsof -i :8081
# 2. Database not ready: docker-compose logs postgres-stage
# 3. Image build failed: docker-compose build --no-cache
```

### Issue: Database connection refused

```bash
# Verify database is running
docker-compose ps postgres-stage

# Check database logs
docker-compose logs postgres-stage

# Manual connection test
docker exec -it connectly-postgres-stage psql -U postgres -d connectly_db_stage -c "SELECT 1"
```

### Issue: No traces in Datadog

```bash
# Verify Datadog agent running
docker-compose logs datadog

# Check agent connectivity
curl -X GET http://localhost:8126/v1/config 2>/dev/null | cat

# Verify Datadog API key
echo $DD_API_KEY  # Should not be empty
```

### Issue: Application won't compile

```bash
# Clear Gradle cache
./gradlew clean

# Rebuild
./gradlew build -x test

# If still fails, check Java version
java -version  # Should be 17.x
```

---

## DevOps Best Practices

### 1. **Never Commit Secrets**

Use `.env` files (git-ignored) and GitHub Secrets:

```bash
# ❌ DON'T
docker run -e JWT_SECRET=my-secret-key ...

# ✅ DO
# In .env (git-ignored)
JWT_SECRET=...
# Load via docker-compose.yml
environment:
  JWT_SECRET: ${JWT_SECRET}
```

### 2. **Use Spring Profiles Consistently**

```bash
# Profile naming: application-{profile}.properties
# - dev: local development
# - stage: pre-production testing
# - demo: feature showcase
# - prod: customer-facing
```

### 3. **Database Backups**

```bash
# Daily backup script for PostgreSQL
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec connectly-postgres-prod pg_dump -U postgres connectly_db_prod > \
  /backups/connectly_db_prod_$DATE.sql
```

### 4. **Monitor Resource Usage**

```bash
# Check container resource limits
docker stats connectly-api-prod

# Update docker-compose.yml for limits
services:
  api-prod:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 5. **Log Aggregation Best Practices**

- Use **structured logging** (JSON format) for easy parsing
- Include **request IDs** in all logs for traceability
- Correlate logs with traces via `dd.trace_id`
- Separate **application logs** from **infrastructure logs**

### 6. **Deployment Windows**

- **Stage**: Auto-deploy on every commit (continuous deployment)
- **Demo**: Deploy on tag creation (e.g., `git tag v1.0.0`)
- **Prod**: Manual approval required + add health check delay

### 7. **Security Hardening**

- [ ] Enable HTTPS/SSL in production
- [ ] Use strong database passwords
- [ ] Restrict SSH access to known IPs
- [ ] Enable Datadog security monitoring (CSM)
- [ ] Rotate JWT secrets regularly
- [ ] Use managed secrets (AWS Secrets Manager, Vault)

---

## Summary: Phase 1 Completed ✅

You now have:
- ✅ Multi-environment Spring Boot configuration
- ✅ Docker containerization (multi-stage build)
- ✅ Local orchestration with docker-compose
- ✅ Actuator endpoints for monitoring
- ✅ Structured logging (logback-spring.xml)

**Next Steps:**
1. Test locally: `docker-compose up`
2. Create Phase 2 workflows (see Phase 3 section above)
3. Deploy to DigitalOcean Droplet (Phase 4)
4. Configure Datadog dashboards (Phase 5)

---

## Additional Resources

- [Spring Boot Profiles](https://spring.io/blog/2015/06/08/accessing-application-properties-in-the-spring-boot-environment)
- [Docker Multi-Stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Datadog Java Guide](https://docs.datadoghq.com/tracing/trace_collection/automatic_instrumentation/java/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [DigitalOcean Documentation](https://docs.digitalocean.com/)

---

## Questions or Issues?

Refer to [Troubleshooting](#troubleshooting) section or check logs:

```bash
docker-compose logs -f [service-name]
```

**Happy Deploying! 🚀**

[← Documentation index](../README.md) | [Datadog guide](datadog.md) | [Local development](../getting-started/local-development.md)
