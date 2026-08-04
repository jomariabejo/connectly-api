---
sidebar_position: 1
title: Swagger UI
---

# Swagger UI & the OpenAPI spec

The API describes itself. springdoc-openapi scans the controllers at runtime and serves both a machine-readable spec and an interactive console.

| | URL |
|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **Spec (JSON)** | http://localhost:8080/v3/api-docs |
| **Spec (YAML)** | http://localhost:8080/v3/api-docs.yaml |
| **Checked-in copy** | [`doc/api-documentation.yml`](https://github.com/jomariabejo/connectly-api/blob/main/doc/api-documentation.yml) |

All three are public — no bearer token needed to *read* the docs.

## Calling a secured endpoint from Swagger UI

Everything outside `/auth/**` needs a JWT. Swagger UI holds one for you:

1. **Get a token.** Expand `POST /auth/login` → **Try it out** → send your credentials. Copy the `token` from the response.
2. **Click Authorize** (top right).
3. **Paste the raw token** and confirm. Do *not* type `Bearer ` — the scheme is declared as `bearerFormat: JWT`, so the prefix is added for you.
4. Every secured operation now sends the header automatically. The padlocks close.

The token expires after `JWT_EXPIRATION_MS` (one hour by default). When calls start coming back `401`, log in again and re-authorize.

:::tip Account must be verified first
`POST /auth/login` rejects an account that has not clicked its verification link. See [Authentication](./authentication.md).
:::

## Using the spec elsewhere

The same `/v3/api-docs` URL drives more than Swagger UI. **[Postman](./postman.md)** imports it by link and builds a collection that stays in sync with the code — see that page for the import steps and a login script that captures the JWT automatically.

## What is in the spec

24 operations across five tags — Authentication, Users, Posts, Comments, Likes. The `/test/helloworld`, `/user/dashboard` and `/admin/dashboard` smoke-test endpoints are annotated `@Hidden` and deliberately excluded.

## Regenerating the checked-in spec

[`doc/api-documentation.yml`](https://github.com/jomariabejo/connectly-api/blob/main/doc/api-documentation.yml) is a committed snapshot of the live spec, useful when the API is not running — [Postman](./postman.md) falls back to it for offline imports. Refresh it whenever you change a route, DTO or annotation:

```bash
./gradlew bootRun &
curl -s localhost:8080/v3/api-docs.yaml -o doc/api-documentation.yml
```

Then commit the result. Nothing enforces this, so the file goes stale silently — the live `/v3/api-docs` is always the authority.

## Configuration

Set in [`application.properties`](../getting-started/configuration.md), all overridable:

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.api-docs.enabled=${SWAGGER_ENABLED:true}
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.display-request-duration=true
springdoc.swagger-ui.doc-expansion=none
springdoc.packages-to-scan=com.jomariabejo.connectly_api.controller
```

:::warning Turn it off in public deployments
`SWAGGER_ENABLED=false` removes both Swagger UI and `/v3/api-docs`. A public, unauthenticated map of every endpoint and payload shape is a reconnaissance gift.
:::

## How the docs get their content

Nothing is written twice — the spec is generated from the code:

| Annotation | Where | Produces |
|---|---|---|
| `@Tag` | controller class | The sidebar grouping |
| `@Operation` | handler method | Summary and description |
| `@ApiResponse` | handler method | The documented status codes |
| `@Schema` | DTO field | Field description, example, constraints |
| `@SecurityRequirement` | controller class | The padlock and `Authorize` wiring |
| `@Hidden` | controller class | Excludes it entirely |
| `@OpenAPIDefinition`, `@SecurityScheme` | `config/OpenApiConfig.java` | Title, version, license, the bearer scheme |

Change a route or a DTO and the spec follows. Change a *description* and you edit the annotation, not a YAML file.
