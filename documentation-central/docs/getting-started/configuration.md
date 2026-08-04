---
sidebar_position: 2
title: Configuration
---

# Configuration

Every setting has a working local default, so **a fresh clone runs with no configuration at all**. You only touch this page when something on your machine differs from the defaults.

## How settings are resolved

`application.properties` never hardcodes a value. Every line is a placeholder with a fallback:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/connectly_db}
```

Which resolves in this order — first one that exists wins:

1. **A real environment variable** — `DB_URL=… ./gradlew bootRun`, or whatever your CI and hosting platform inject
2. **A `.env` file** in the project root
3. **The default** baked into `application.properties`

That ordering is what makes the same build work on a laptop and in production: production sets real environment variables and never ships a `.env`.

`.env` is loaded by Spring Boot itself, via one line at the top of `application.properties`:

```properties
spring.config.import=optional:file:.env[.properties]
```

`optional:` means a missing `.env` is fine, not an error. No dotenv library is involved.

## Creating a `.env`

```bash
cp .env.example .env
```

`.env.example` is the committed template documenting every variable; `.env` is gitignored and stays on your machine. Uncomment only the lines you actually change.

:::danger Never commit `.env`
`.gitignore` covers `.env` and `.env.*` while keeping `.env.example` tracked. Keep it that way.
:::

### File format

`.env` is parsed as a Java properties file, not a shell script:

```properties
# a comment
DB_PASSWORD=hunter2
```

- No `export`, one `KEY=value` per line
- **No quotes** — `DB_PASSWORD="hunter2"` makes the password literally `"hunter2"`, quotes included
- A literal backslash must be doubled: `\\`
- Trailing whitespace is part of the value

## Reference

### Server

| Variable | Default | Notes |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |

### Database

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/connectly_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `admin` | |
| `JPA_DDL_AUTO` | `create-drop` | See the warning below |
| `JPA_SHOW_SQL` | `true` | Log generated SQL |

:::warning `create-drop` destroys data
The default rebuilds the schema on start and **drops every table on shutdown**. Values you can set instead:

| Value | Behaviour |
|---|---|
| `create-drop` | Rebuild on start, drop on shutdown *(default — local only)* |
| `update` | Apply additive changes, keep data |
| `validate` | Verify the schema matches the entities, change nothing |
| `none` | Leave the schema entirely alone |

Use `validate` or `none` anywhere the data matters. [`schema.sql`](../data-model/schema.md) is the reference DDL to create the tables from.
:::

### JWT

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | a committed dev key | HMAC-SHA256 signing key, **Base64-decoded** before use |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) | Token lifetime |

:::danger Rotate the signing key
The default `JWT_SECRET` is committed to the repository, so it is public. Anyone can forge a valid token against a deployment still using it. Generate your own:

```bash
openssl rand -hex 32
```

The value is run through a Base64 decoder, so it must be valid Base64 that decodes to **at least 32 bytes** — the hex string above satisfies both.
:::

### Mail

| Variable | Default | Notes |
|---|---|---|
| `MAIL_HOST` | `localhost` | |
| `MAIL_PORT` | `1025` | Mailpit's SMTP port |
| `MAIL_USERNAME` | *(empty)* | |
| `MAIL_PASSWORD` | *(empty)* | |
| `MAIL_SMTP_AUTH` | `false` | Turn on for a real provider |
| `MAIL_SMTP_STARTTLS` | `false` | Turn on for a real provider |

### CORS

| Variable | Default | Notes |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,DELETE` | |

:::warning CORS is currently inert — changing these has no effect
Two CORS configurations exist and **neither is applied**: the properties above, and a `CorsConfigurationSource` bean in `SecurityConfiguration` that hardcodes `http://localhost:8080` with only `GET,POST`.

The security filter chain never calls `.cors(…)`, so the bean is never consulted, and preflight `OPTIONS` requests fall through to `anyRequest().authenticated()` and are rejected:

```bash
$ curl -i -X OPTIONS http://localhost:8080/posts \
    -H 'Origin: http://localhost:3000' \
    -H 'Access-Control-Request-Method: GET'
HTTP/1.1 403
# no Access-Control-Allow-Origin header
```

A browser-based frontend on another origin cannot call this API today. See [known issues](../reference/known-issues.md) for the fix.
:::

### Password reset

| Variable | Default | Notes |
|---|---|---|
| `PASSWORD_RESET_EXPIRY_MINUTES` | `15` | Lifetime of a reset link or OTP |
| `PASSWORD_RESET_MAX_ATTEMPTS` | `5` | Reset requests per address per window |
| `PASSWORD_RESET_ATTEMPT_WINDOW_MINUTES` | `60` | The window |
| `PASSWORD_RESET_MAX_OTP_ATTEMPTS` | `3` | Wrong guesses before the code is burned |
| `PASSWORD_RESET_CLEANUP_INTERVAL_MS` | `3600000` | Expired-token sweep interval |
| `PASSWORD_MIN_LENGTH` | `8` | Minimum for a *reset* password |
| `PASSWORD_RESET_REDIRECT_URL` | `http://localhost:8080/reset-password` | Where the emailed link points — set to your frontend |

:::note Strength rules apply on reset, not registration
`POST /auth/reset-password` requires length plus an uppercase letter, a digit and a special character. `POST /auth/registration` enforces only `@NotBlank`, so `a` is an acceptable password at sign-up. See [known issues](../reference/known-issues.md).
:::

### Docs and logging

| Variable | Default | Notes |
|---|---|---|
| `SWAGGER_ENABLED` | `true` | Set `false` to hide Swagger UI and `/v3/api-docs` |
| `LOG_LEVEL_APP` | `DEBUG` | `com.jomariabejo.connectly_api` |
| `LOG_LEVEL_WEB` | `INFO` | `org.springframework.web` |
| `LOG_LEVEL_SECURITY` | `DEBUG` | `org.springframework.security` |

:::tip Before deploying publicly
Set `SWAGGER_ENABLED=false` and drop `LOG_LEVEL_SECURITY` to `WARN` — at `DEBUG` Spring Security logs authentication details on every request.
:::

## A production-shaped example

```properties
DB_URL=jdbc:postgresql://db.internal:5432/connectly
DB_USERNAME=connectly
DB_PASSWORD=<from your secret manager>
JPA_DDL_AUTO=validate

JWT_SECRET=<openssl rand -hex 32>
JWT_EXPIRATION_MS=900000

MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<from your secret manager>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true

CORS_ALLOWED_ORIGINS=https://app.example.com
PASSWORD_RESET_REDIRECT_URL=https://app.example.com/reset-password

SWAGGER_ENABLED=false
LOG_LEVEL_APP=INFO
LOG_LEVEL_SECURITY=WARN
```

Supply these as real environment variables from your platform's secret store rather than as a file on disk.
