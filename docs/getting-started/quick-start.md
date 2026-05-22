# Quick start — Docker Compose

**Phase 1: Local Foundation ✅ COMPLETE**

For full local setup (including `./gradlew bootRun`), see **[Local Development](local-development.md)**.

---

## 🚀 Start All 3 Environments (Docker Compose)

```bash
cd connectly-api

docker compose up -d --build

# Wait for Spring Boot (~30s) before curling
docker compose wait api-stage api-demo api-prod

docker compose ps   # APIs should show (healthy)
```

---

## ✅ Verify It Works

### Health Check Each Environment
```bash
# Use timeouts so curl does not hang while apps are still starting
curl -s --connect-timeout 3 --max-time 10 http://localhost:8081/api/actuator/health | python3 -m json.tool
curl -s --connect-timeout 3 --max-time 10 http://localhost:8082/api/actuator/health | python3 -m json.tool
curl -s --connect-timeout 3 --max-time 10 http://localhost:8083/api/actuator/health | python3 -m json.tool

# Expected: {"status":"UP"}
```

### View Application Metrics
```bash
# Prometheus metrics
curl http://localhost:8081/api/actuator/metrics | jq

# Specific metric
curl http://localhost:8081/api/actuator/metrics/jvm.memory.used
```

### Test an API Endpoint (Example: Create a Test User)
```bash
# Register a test account
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Test",
    "lastName":"User",
    "email":"test@example.com",
    "password":"TestPassword123!"
  }'

# Should return: {"id":"...", "email":"test@example.com", "firstName":"Test", "lastName":"User"}
```

### View Mail Testing Interface
```bash
# Mailpit UI (all 3 environments share this)
# Visit: http://localhost:8025
# Watch test emails being "sent" in development
```

### Connect to Database (Example: Stage)
```bash
# Connect to stage PostgreSQL database
docker exec -it connectly-postgres-stage psql -U postgres -d connectly_db_stage

# List tables
\dt

# View Flyway migrations
SELECT * FROM flyway_schema_history;

# Exit
\q
```

---

## 📜 Environment Profiles

Each environment runs with different configurations:

| Environment | Port | Database | Profile | Use Case |
|---|---|---|---|---|
| **Stage** | 8081 | postgres-stage:5433 | `spring.profiles.active=stage` | Pre-prod testing, automatic deployment |
| **Demo** | 8082 | postgres-demo:5434 | `spring.profiles.active=demo` | Feature showcase, manual trigger |
| **Prod** | 8083 | postgres-prod:5435 | `spring.profiles.active=prod` | Customer-facing, approval-gated |

Each has separate database, environment variables, logging levels.

---

## 🛑 Stop All Services

```bash
# Stop containers (keeps data volumes)
docker-compose down

# Stop and delete everything (removes data volumes)
docker-compose down -v

# Stop specific container
docker-compose stop api-stage

# Restart specific container
docker-compose up -d api-stage
```

---

## 📖 View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-stage
docker-compose logs -f postgres-stage

# Last 50 lines
docker-compose logs --tail=50 api-stage
```

---

## 🔧 Common Tasks

### Rebuild Images (if you modified code)
```bash
docker-compose build --no-cache
docker-compose up -d
```

### Run Gradle Tests Locally (without Docker)
```bash
./gradlew test
```

### Access Gradle Build Artifacts
```bash
# Built JAR file
ls -lh build/libs/connectly-api-*.jar

# Check dependencies
./gradlew dependencies
```

### Clean Everything and Start Fresh
```bash
# Remove all images and volumes
docker-compose down -v
docker system prune -a

# Rebuild and restart
docker-compose build
docker-compose up -d
```

---

## 🌐 Available Endpoints (for Testing)

All endpoints require Auth (except registration and webhooks):

```bash
# ========== AUTHENTICATION ==========
POST   /api/v1/auth/register              # Register new user
POST   /api/v1/auth/login                 # Login (returns JWT token)
POST   /api/v1/auth/refresh               # Refresh JWT token

# ========== USERS ==========
GET    /api/v1/user/{id}                  # Get user profile
PUT    /api/v1/user/{id}                  # Update profile
GET    /api/v1/user/search?email=...      # Search user by email

# ========== POSTS ==========
GET    /api/v1/posts                      # List all posts
POST   /api/v1/posts                      # Create new post
GET    /api/v1/posts/{id}                 # Get post details
PUT    /api/v1/posts/{id}                 # Update post
DELETE /api/v1/posts/{id}                 # Delete post

# ========== ORDERS ==========
GET    /api/v1/orders                     # List orders
POST   /api/v1/orders                     # Create order
GET    /api/v1/orders/{id}                # Order details
PUT    /api/v1/orders/{id}                # Update order

# ========== PAYMENTS ==========
POST   /api/v1/payments                   # Create payment
GET    /api/v1/payments/{id}              # Payment status
POST   /api/v1/payments/webhooks/...      # Payment provider webhooks (public)

# ========== ADMIN ENDPOINTS ==========
GET    /api/v1/admin/users                # List all users (ADMIN only)
GET    /api/v1/admin/orders               # List all orders (ADMIN only)

# ========== HEALTH & MONITORING ==========
GET    /api/actuator/health               # Application health
GET    /api/actuator/metrics              # All metrics
GET    /api/actuator/metrics/...          # Specific metric
GET    /api/actuator/info                 # Application info
```

**Example workflow:**

```bash
# 1. Register
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"Pass123!"}'

# 2. Login (copy token from response)
TOKEN=$(curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Pass123!"}' | jq -r '.accessToken')

# 3. Get profile with token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/v1/user/profile

# 4. Create a post
curl -X POST http://localhost:8081/api/v1/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello World","content":"My first post"}'
```

---

## 📊 What's Configured

✅ **Multi-Environment Profiles**
- Dev: DEBUG logging, local database, MailHog
- Stage: INFO logging, Actuator enabled, JSON logs
- Demo: INFO logging, Actuator enabled, JSON logs  
- Prod: WARN logging, Datadog metrics, Actuator restricted

✅ **Docker & Orchestration**
- Multi-stage build (compile → lightweight runtime)
- ~300MB final image per environment
- Healthchecks for all services
- Persistent volumes for database data
- Network isolation per environment

✅ **Monitoring & Metrics**
- Spring Boot Actuator (`/api/actuator/*`)
- Prometheus metrics format
- Micrometer registry (Datadog-ready)
- Structured JSON logging with logback
- Custom metrics hooks in place

✅ **Security**
- Non-root user in containers (uid: 1000)
- Spring Security with JWT auth
- Role-based endpoints (USER, ADMIN)
- CSRF protection enabled

---

## 🎯 Next Phase: GitHub Actions CI/CD

After confirming local setup works, see [Deployment](../deployment/deployment.md) > **Phase 3: GitHub Actions CI/CD** to:

1. Create `.github/workflows/build-and-test.yml`
2. Create `.github/workflows/build-docker.yml`
3. Create `.github/workflows/deploy.yml`
4. Push to GitHub → auto-build & deploy to DigitalOcean

---

## ⚠️ Troubleshooting

### Containers won't start
```bash
# Check logs
docker-compose logs api-stage

# Common causes:
# - Port already in use: lsof -i :8081
# - Database initialization failed: check postgres logs
# - Out of disk space: docker system df

# Fix: Restart Docker
docker-compose down -v
docker-compose up -d
```

### Database connection error
```bash
# Wait for database to be ready (sometimes takes 30s)
docker-compose logs postgres-stage | grep ready

# Manually test connection
docker exec connectly-postgres-stage pg_isready -U postgres
```

### API not responding
```bash
# Check if container is running
docker-compose ps api-stage

# View application logs
docker-compose logs -f api-stage

# Rebuild if code changed
docker-compose build api-stage
docker-compose up -d api-stage
```

### Out of disk space from Docker layers
```bash
# Clean up unused images/containers
docker system prune -a

# Remove specific environment
docker-compose down connectly-api_api-stage
```

---

## 📁 Project Structure

```
connectly-api/
├── src/main/java/com/jomariabejo/connectly_api/  # Java source
├── src/main/resources/
│   ├── application.properties                     # Base config (override in profiles)
│   ├── application-dev.properties                 # Dev environment
│   ├── application-stage.properties               # Stage environment
│   ├── application-demo.properties                # Demo environment
│   ├── application-prod.properties                # Prod environment
│   ├── logback-spring.xml                         # Logging config
│   ├── db/migration/                              # Flyway migrations
│   └── docs/http-template/                        # REST API docs
├── Dockerfile                                      # Multi-stage build
├── docker-compose.yml                             # Local orchestration
├── .dockerignore                                  # Docker build context
├── .env.example                                   # Environment variables template
├── build.gradle                                   # Dependencies & build config
├── docs/                                          # Documentation (this guide lives here)
│   ├── getting-started/                           # Local dev, debugging, quick start
│   └── deployment/                                # Deploy & Datadog guides
└── docker-compose.yml                             # Local orchestration
```

---

## 🎓 Learning Resources

- [Spring Boot Profiles - Official Docs](https://spring.io/blog/2015/06/08/accessing-application-properties-in-the-spring-boot-environment)
- [Docker Multi-Stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker Compose - Getting Started](https://docs.docker.com/compose/gettingstarted/)
- [Spring Boot Actuator Guide](https://spring.io/guides/gs/actuator-service/)
- [Datadog Java Tracing](https://docs.datadoghq.com/tracing/trace_collection/automatic_instrumentation/java/)

---

## ✨ Next Steps

1. ✅ **Local Testing** - Run `docker-compose up` and test endpoints
2. 📝 **Create GitHub Actions Workflows** — see [Deployment](../deployment/deployment.md) Phase 3
3. ☁️ **Deploy to DigitalOcean** — see [Deployment](../deployment/deployment.md) Phase 4
4. 📊 **Setup Datadog Monitoring** — see [Datadog](../deployment/datadog.md)

---

**Happy Deploying! 🚀**

Questions? See [Debugging](debugging.md) for local issues or [Deployment](../deployment/deployment.md) for DevOps practices.

[← Documentation index](../README.md)
