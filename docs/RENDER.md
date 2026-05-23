# Deploy Connectly API on Render

This repository includes a [Render Blueprint](https://render.com/docs/infrastructure-as-code) ([render.yaml](render.yaml)) plus Docker changes so the API listens on Render’s `PORT`, maps Render Postgres `DATABASE_URL` to JDBC, and passes health checks under the `/api` context path.

## Prerequisites

- GitHub (or GitLab) repository connected to [Render](https://render.com).
- Optional: SMTP credentials (SendGrid, Resend, Mailgun, etc.) for outbound mail.

## Option A — Blueprint (`render.yaml`)

1. Push this repo (including [render.yaml](render.yaml)) to your Git host.
2. In Render: **Blueprints** → **New Blueprint Instance** → select the repository.
3. On first apply, Render prompts for every `sync: false` variable (SMTP and public API URL). Use:
   - **`APP_API_PUBLIC_BASE_URL`**: `https://<your-web-service-name>.onrender.com/api` (set after you know the service hostname, or update after the first deploy).
   - **`SPRING_WEB_CORS_ALLOWED_ORIGINS`**: your SPA origin(s), comma-separated if multiple (e.g. `https://myapp.vercel.app`).
   - **`MAIL_*`**: real SMTP host, username, and password.
4. Keep the **database and web service in the same [region](https://render.com/docs/regions)** so the internal `DATABASE_URL` works.

The blueprint provisions:

| Resource | Notes |
|----------|--------|
| **PostgreSQL** | `connectly-db`, `basic-256mb` (edit `plan` in [render.yaml](render.yaml) if needed). |
| **Web service** | `runtime: docker`, [Dockerfile](Dockerfile), `plan: free` (change if you need always-on or more CPU). |

`JWT_SECRET` is generated automatically. Datadog metrics export is off by default (`MANAGEMENT_METRICS_EXPORT_DATADOG_ENABLED=false`); set it to `true` and add `DD_API_KEY` / `DD_APP_KEY` when you want Datadog.

## Option B — Manual dashboard setup

1. **PostgreSQL**: **New** → **PostgreSQL** → same **region** as the app. Note the **Internal Database URL** (`postgresql://...`).
2. **Web service**: **New** → **Web Service** → connect the repo.
   - **Runtime**: Docker (or use the Dockerfile path below).
   - **Dockerfile path**: `./Dockerfile`
   - **Health check path**: `/api/actuator/health`
3. **Environment** (minimum):

| Variable | Value |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod,render` |
| `DATABASE_URL` | Use **Link database** so Render injects the internal URL, or paste it from the database dashboard. |
| `JWT_SECRET` | Long random secret (hex or base64). |
| `APP_API_PUBLIC_BASE_URL` | `https://<service>.onrender.com/api` |
| `MANAGEMENT_METRICS_EXPORT_DATADOG_ENABLED` | `false` unless using Datadog |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Your SMTP provider |
| `SPRING_WEB_CORS_ALLOWED_ORIGINS` | Frontend origin(s) |

Render sets **`PORT`** automatically. The app reads it via `server.port=${PORT:8080}` in [application-prod.properties](src/main/resources/application-prod.properties). The Docker [render-entrypoint.sh](render-entrypoint.sh) converts `postgresql://` to `jdbc:postgresql://` for Spring.

## Profiles: `prod` + `render`

- **`prod`**: production tuning, `/api` context path, Flyway + Hibernate `validate`, JWT from env.
- **`render`**: [application-render.properties](src/main/resources/application-render.properties) clears split `spring.datasource.username` / `password` so credentials embedded in the JDBC URL from `DATABASE_URL` are used.

Do **not** set `SPRING_DATASOURCE_URL` manually if you rely on `DATABASE_URL`; the entrypoint sets it at container start.

## Health checks and URLs

- **Liveness / Render health check path**: `/api/actuator/health`
- **Example base URL**: `https://<service>.onrender.com/api`

## Payments (optional)

If you use PayMongo or PayPal, set the same variables as in [application.properties](src/main/resources/application.properties) (`PAYMONGO_*`, `PAYPAL_*`) in the Render dashboard.

## Datadog in production

[application-prod.properties](src/main/resources/application-prod.properties) defaults `management.metrics.export.datadog.enabled` to **false** unless you set `MANAGEMENT_METRICS_EXPORT_DATADOG_ENABLED=true` and provide keys. This matches local Docker overrides and avoids startup issues on Render without keys.

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Crash on DB connect | Same **region** for DB and web; internal `DATABASE_URL`; profile includes **`render`**. |
| 502 / failed health check | Logs for Flyway errors; DB reachable; path **`/api/actuator/health`**. |
| CORS errors from browser | `SPRING_WEB_CORS_ALLOWED_ORIGINS` includes your exact frontend origin (scheme + host, no trailing slash unless you mean it). |
| Mail errors | SMTP env vars; `app.mail.fail-on-error` is `false` in base config so API may continue, but fix SMTP for real delivery. |

## Related files

- [render.yaml](render.yaml) — Blueprint
- [Dockerfile](Dockerfile) — multi-stage build + `render-entrypoint.sh`
- [render-entrypoint.sh](render-entrypoint.sh) — `DATABASE_URL` → `SPRING_DATASOURCE_URL`
- [DEPLOYMENT.md](DEPLOYMENT.md) — broader ops guide (DigitalOcean, Docker Compose, Datadog)
