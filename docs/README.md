# Connectly API — Documentation

Central index for setup, operations, and deployment guides.

## Getting started

| Guide | Description |
|-------|-------------|
| [Quick Start](getting-started/quick-start.md) | Docker Compose: stage, demo, prod on ports 8081–8083 |
| [Local Development](getting-started/local-development.md) | `./gradlew bootRun`, Postgres, Mailpit, API URL layout |
| [Debugging](getting-started/debugging.md) | Logs, curl/Postman checks, HTTP 409, common failures |

**Typical path:** [Local Development](getting-started/local-development.md) for daily coding → [Quick Start](getting-started/quick-start.md) for full Docker stack → [Debugging](getting-started/debugging.md) when something breaks.

## Deployment & observability

| Guide | Description |
|-------|-------------|
| [Deployment](deployment/deployment.md) | Multi-env architecture, CI/CD, DigitalOcean, phases 1–5 |
| [Datadog](deployment/datadog.md) | APM, metrics, dashboards, agent setup |

## API testing assets

| Asset | Location |
|-------|----------|
| Postman collection | [`src/main/resources/docs/postman/connectly-api-v1.postman_collection.json`](../src/main/resources/docs/postman/connectly-api-v1.postman_collection.json) |
| HTTP templates (REST Client) | [`src/main/resources/docs/http-template/`](../src/main/resources/docs/http-template/) |

## Reference

| Guide | Description |
|-------|-------------|
| [External links](reference/external-links.md) | Articles and resources used during development |

## URL convention

All API routes use:

```text
http://localhost:{port}/api/v1/{resource}
```

- **bootRun:** port `8080`, base `http://localhost:8080/api`
- **Docker stage / demo / prod:** ports `8081` / `8082` / `8083`

Actuator (not versioned): `http://localhost:{port}/api/actuator/health`
