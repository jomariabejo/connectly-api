# Local development guide

How to run the API on your machine: **minimal** (Gradle only) or **full stack** (Docker Compose with stage, demo, and prod).

---

## Prerequisites

| Tool | Version | Used for |
|------|---------|----------|
| **Java** | 17 | `./gradlew bootRun` |
| **PostgreSQL** | 14+ | Database for `bootRun` (host `localhost:5432`) |
| **Docker & Docker Compose** | v2+ | Multi-environment stack (optional) |

---

## Option 1: Minimal — `./gradlew bootRun`

Best for day-to-day coding: one JVM, one database, fast reload with DevTools.

### 1. Start PostgreSQL

Create the database (once):

```bash
# Using psql
psql -U postgres -c "CREATE DATABASE connectly_db;" 2>/dev/null || true
```

Default connection (from `application-dev.properties`):

| Setting | Value |
|---------|-------|
| Host | `localhost:5432` |
| Database | `connectly_db` |
| User / password | `postgres` / `admin` |

### 2. (Optional) Mailpit for email testing

```bash
docker run -d --name connectly-mailpit \
  -p 1025:1025 -p 8025:8025 \
  axllent/mailpit:latest
```

UI: http://localhost:8025

### 3. Run the application

```bash
cd connectly-api

./gradlew bootRun
```

- **Profile:** `dev` (default via `spring.profiles.default=dev` in `application.properties`)
- **Port:** `8080`
- **Context path:** `/api` (all URLs are under `/api/...`)
- **Schema:** Hibernate `create-drop` — tables are created automatically on startup

Wait until the log shows:

```text
Started ConnectlyApiApplication in ... seconds
```

`BUILD SUCCESSFUL` alone is not enough — the process must keep running.

### 4. Verify

```bash
curl http://localhost:8080/api/v1/public/hello
# Hello, Connectly API is running!

curl -s http://localhost:8080/api/actuator/health | python3 -m json.tool
# {"status":"UP", ...}
```

### Override profile

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

---

## Option 2: Full stack — Docker Compose

Runs **three isolated environments** (stage, demo, prod) plus shared Mailpit — useful for integration testing and parity with deployment.

### 1. Start everything

```bash
cd connectly-api

# First run builds images (~1–5 min); later runs are faster
docker compose up -d --build
```

### 2. Wait for APIs to be ready

Compose marks containers **Started** when Postgres is healthy (~11s), but Spring Boot needs **~25–35s** more.

```bash
# Preferred: wait until health checks pass
docker compose wait api-stage api-demo api-prod

# Or poll until status shows (healthy)
docker compose ps

# Or fixed delay
sleep 35
```

Do **not** curl immediately after `up -d` — you may see `HTTP 000`, empty JSON, or `Connection reset by peer` while the JVM is still starting.

### 3. Verify

```bash
curl -s --connect-timeout 3 --max-time 10 \
  http://localhost:8081/api/actuator/health | python3 -m json.tool

curl -s --connect-timeout 3 --max-time 10 \
  http://localhost:8082/api/actuator/health | python3 -m json.tool

curl -s --connect-timeout 3 --max-time 10 \
  http://localhost:8083/api/actuator/health | python3 -m json.tool

curl http://localhost:8081/api/v1/public/hello
```

Expected: `{"status":"UP"}` on each health URL and `Hello, Connectly API is running!` on hello.

### Environment map

| Environment | API port (host) | Postgres port (host) | Spring profile |
|-------------|-----------------|----------------------|----------------|
| **Stage** | 8081 | 5433 | `stage` |
| **Demo** | 8082 | 5434 | `demo` |
| **Prod (local)** | 8083 | 5435 | `prod` |

| Service | URL |
|---------|-----|
| Stage API base | http://localhost:8081/api |
| Demo API base | http://localhost:8082/api |
| Prod API base | http://localhost:8083/api |
| Mailpit UI | http://localhost:8025 |

### 4. Useful commands

```bash
# Status
docker compose ps

# Logs (one service)
docker compose logs -f api-stage

# Stop (keep DB data)
docker compose down

# Stop and wipe volumes (fresh DBs)
docker compose down -v

# Restart one API
docker compose up -d api-stage
```

### Connect to a database

```bash
docker exec -it connectly-postgres-stage psql -U postgres -d connectly_db_stage
```

---

## API URL layout (`/v1`)

With `server.servlet.context-path=/api` (dev and Docker profiles), URLs look like:

| Type | Example |
|------|---------|
| Public smoke test | `GET /api/v1/public/hello` |
| Auth | `POST /api/v1/auth/login` |
| Users | `GET /api/v1/users/me` |
| Test | `GET /api/v1/test/helloworld` |
| Actuator (not versioned) | `GET /api/actuator/health` |
| OpenAPI UI | `GET /api/swagger-ui.html` |

Path constants live in `ApiPaths.java`; new controllers should use `@RequestMapping(ApiPaths.V1 + "/...")` or a sub-constant.

---

## Compare: `bootRun` vs Docker Compose

| | `./gradlew bootRun` | `docker compose up -d` |
|--|---------------------|-------------------------|
| **Speed** | Fastest to start coding | Slower first build |
| **Environments** | Single (`dev`) | Three (stage, demo, prod) |
| **Port** | 8080 | 8081, 8082, 8083 |
| **PostgreSQL** | Your local install | Container per env |
| **Schema** | `create-drop` (dev) | `update` in Compose (local Docker override) |
| **Datadog** | Disabled | Disabled in local Compose |

---

## Troubleshooting

### `curl` hangs or `HTTP 000`

- **Docker:** APIs still starting — run `docker compose ps` and wait for `(healthy)`, or `docker compose wait api-stage api-demo api-prod`.
- **bootRun:** Process exited — check terminal for `APPLICATION FAILED TO START`; ensure Postgres is running.

### `BUILD SUCCESSFUL` but nothing on port 8080

The Gradle task finished because the app crashed. Read the full log above `BUILD SUCCESSFUL`. Common causes:

- Postgres not running or wrong credentials
- Datadog API key required (should not happen on `dev` profile)

### `Connection reset by peer`

You curled during startup. Wait and retry.

### Docker API container `Exited (1)`

```bash
docker compose logs api-stage --tail 50
```

| Log message | Fix |
|-------------|-----|
| `missing table` / `missing column` | Fresh volume: `docker compose down -v && docker compose up -d --build`, or schema is applied via init + `ddl-auto=update` in Compose |
| `MAIL_USERNAME` placeholder (prod) | Ensure latest `docker-compose.yml` sets `MAIL_USERNAME` / `MAIL_PASSWORD` for `api-prod` |
| Datadog `apiKey` required | Datadog export is disabled in Compose env; rebuild images if using an old compose file |

### Actuator returns 403

Health endpoints must be permitted in `SecurityConfiguration`. Rebuild after pulling latest code:

```bash
docker compose up -d --build
```

### Wrong URL

All environments use context path **`/api`**:

- Correct: `http://localhost:8080/api/v1/public/hello` (bootRun)
- Wrong: `http://localhost:8080/api/hello` (legacy; use `/v1/...`)

All business endpoints are under **`/api/v1/...`** (context path + version prefix). Actuator stays at `/api/actuator/...`.

---

## Postman

Import [`src/main/resources/docs/postman/connectly-api-v1.postman_collection.json`](../../src/main/resources/docs/postman/connectly-api-v1.postman_collection.json). All requests use `{{baseUrl}}/v1/...` with `baseUrl` including `/api` (e.g. `http://localhost:8080/api`). See [Debugging — Postman verification](debugging.md#postman-verification).

---

## Related docs

- [Debugging](debugging.md) — logs, Docker issues, HTTP 409 on registration, Postman verification, curl tips
- [Quick Start](quick-start.md) — Docker Compose cheat sheet (stage, demo, prod)
- [Deployment](../deployment/deployment.md) — multi-environment deployment, CI/CD
- [Documentation index](../README.md)
- [.env.example](../../.env.example) — environment variable template

[← Documentation index](../README.md)
