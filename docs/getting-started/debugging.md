# Debugging guide

How to verify Docker/local runs, read logs, and interpret common HTTP responses (including **409 Conflict** on registration).

---

## Quick sanity check (copy-paste)

```bash
cd connectly-api

docker compose up -d --build

# Wait until APIs are healthy (~30–40s after "Started")
docker compose wait api-stage api-demo api-prod 2>/dev/null || sleep 40

docker compose ps   # api-* should show (healthy)

# Health (actuator — not under /v1)
curl -s --connect-timeout 3 --max-time 10 \
  http://localhost:8081/api/actuator/health && echo

# Hello (v1 public)
curl -s --connect-timeout 3 --max-time 10 \
  http://localhost:8081/api/v1/public/hello && echo
```

| Port | Environment | `baseUrl` for Postman |
|------|-------------|------------------------|
| 8081 | stage | `http://localhost:8081/api` |
| 8082 | demo | `http://localhost:8082/api` |
| 8083 | prod (local) | `http://localhost:8083/api` |

Example auth URL: `POST {{baseUrl}}/v1/auth/registration` → `http://localhost:8082/api/v1/auth/registration`

Email links use the same base (`app.api.public-base-url`):

- Verify: `http://localhost:8082/api/v1/auth/verify?token=...`
- Password reset: `http://localhost:8082/api/v1/auth/reset-password?token=...`

---

## Postman verification

Collection file: [`src/main/resources/docs/postman/connectly-api-v1.postman_collection.json`](../../src/main/resources/docs/postman/connectly-api-v1.postman_collection.json)

1. **Import** the collection into Postman (Import → file).
2. **Set `baseUrl`** for your target:
   - bootRun: `http://localhost:8080/api` (default)
   - Docker stage / demo / prod: copy `baseUrlStage`, `baseUrlDemo`, or `baseUrlProd` into `baseUrl`, or edit `baseUrl` directly.
3. **Smoke test** (no auth): run **System Routes → Public Hello** → expect `Hello, Connectly API is running!`
4. **Auth chain**: Register User → copy `verificationToken` from Mailpit → Verify User Email → Login User (sets `userToken`).
5. **Authenticated check**: User Account Flow → Get Current User (Bearer `{{userToken}}`).
6. **Orders + payments** (optional): Orders Flow → Create Order → Payments Flow → Create Payment Checkout.

Every request URL must follow `{{baseUrl}}/v1/...` where `baseUrl` ends with `/api`. Wrong: `http://localhost:8080/v1/...` (missing `/api`).

---

## Your registration `409` — API is working

```json
{
  "status": 409,
  "error": "Request conflicts with existing data",
  "message": "The request conflicts with an existing record..."
}
```

**This is not a Docker or routing failure.** The request reached the app and was rejected because a record already exists (usually **email already registered** in that environment’s database).

### What to do

1. **Register with a new email**

   ```json
   {
     "firstName": "Test",
     "lastName": "User",
     "email": "test.unique.$(date +%s)@example.com",
     "password": "TestPassword123!"
   }
   ```

2. **Or log in** with the existing account:

   `POST http://localhost:8082/api/v1/auth/login`

3. **Or reset demo DB** (wipes all Docker DB data):

   ```bash
   docker compose down -v
   docker compose up -d --build
   docker compose wait api-stage api-demo api-prod 2>/dev/null || sleep 40
   ```

---

## Docker Compose debugging

### 1. Container status

```bash
docker compose ps -a
```

| Status | Meaning |
|--------|---------|
| `Up (healthy)` | Ready for requests |
| `Up (health: starting)` | JVM still booting — wait |
| `Exited (1)` | Crashed — read logs |
| Not listed on 8081 | Nothing listening — curl hangs or `HTTP 000` |

### 2. Logs (first place to look)

```bash
# Follow one API
docker compose logs -f api-stage

# Last 80 lines (startup errors)
docker compose logs api-stage --tail 80

# All APIs
docker compose logs api-stage api-demo api-prod --tail 50
```

**Healthy startup ends with:**

```text
Started ConnectlyApiApplication in ... seconds
```

**Common failures:**

| Log snippet | Fix |
|-------------|-----|
| `missing table` / `missing column` | `docker compose down -v` then `up -d --build`, or check `SPRING_JPA_HIBERNATE_DDL_AUTO=update` in compose |
| `apiKey was 'null'` (Datadog) | Datadog disabled in compose env; rebuild image |
| `MAIL_USERNAME` placeholder (prod) | Set `MAIL_USERNAME` / `MAIL_PASSWORD` in `docker-compose.yml` for `api-prod` |
| `Application run failed` | Scroll up in logs for root `Caused by:` |

### 3. Test from inside the container

```bash
docker exec connectly-api-stage curl -s http://localhost:8080/api/actuator/health
docker exec connectly-api-stage curl -s http://localhost:8080/api/v1/public/hello
```

If this works but host `curl` fails, check port mapping (`8081:8080`).

### 4. Postgres

```bash
docker exec -it connectly-postgres-demo psql -U postgres -d connectly_db_demo -c \
  "SELECT id, email FROM app_user ORDER BY id DESC LIMIT 5;"
```

### 5. Rebuild after code changes

Docker may use **cached** layers and skip recompiling:

```bash
docker compose build --no-cache api-stage
docker compose up -d api-stage
```

Or rebuild all:

```bash
docker compose up -d --build --force-recreate
```

### 6. `docker compose wait` vs Ctrl+C

`docker compose wait api-stage ...` blocks until health checks pass. If you press **Ctrl+C**, wait is cancelled but containers keep running — use `docker compose ps` and curl after ~40s.

---

## HTTP status cheat sheet

| Code | Typical meaning here |
|------|----------------------|
| **200** | Success |
| **401** | Missing/invalid JWT |
| **403** | Authenticated but not allowed (or wrong path before actuator permit) |
| **404** | Wrong URL — e.g. `/api/hello` instead of `/api/v1/public/hello` |
| **409** | Duplicate email/user (registration) or other conflict |
| **422** | Validation error on request body |
| **000** (curl) | Nothing listening yet, or connection refused |

### Verbose curl (see status + headers)

```bash
curl -v --connect-timeout 3 --max-time 10 \
  -H "Content-Type: application/json" \
  -d '{"firstName":"A","lastName":"B","email":"newuser@example.com","password":"TestPassword123!"}' \
  http://localhost:8082/api/v1/auth/registration
```

---

## Local `./gradlew bootRun` debugging

```bash
./gradlew bootRun
# Wait for: Started ConnectlyApiApplication

curl http://localhost:8080/api/v1/public/hello
curl http://localhost:8080/api/actuator/health
```

- Profile: `dev` (default)
- Requires Postgres on `localhost:5432`, database `connectly_db`
- See [Local Development](local-development.md) for full setup

**IDE:** Run `ConnectlyApiApplication` with env `SPRING_PROFILES_ACTIVE=dev`, then attach debugger.

---

## URL reference (avoid 404)

| Wrong (legacy) | Correct |
|----------------|---------|
| `/api/hello` | `/api/v1/public/hello` |
| `/hello` | `/api/v1/public/hello` (with context path `/api`) |
| `/api/auth/...` | `/api/v1/auth/...` |

Actuator and Swagger stay **outside** `/v1`:

- `/api/actuator/health`
- `/api/swagger-ui.html`

---

## Mailpit (password reset / verification emails)

UI: http://localhost:8025

```bash
docker compose logs mailpit --tail 20
```

---

## Related docs

- [Local Development](local-development.md) — run locally (`bootRun` + Docker)
- [Quick Start](quick-start.md) — short Compose commands
- [Deployment](../deployment/deployment.md) — deployment overview
- [Documentation index](../README.md)

[← Documentation index](../README.md)
