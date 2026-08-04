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
| `JPA_DDL_AUTO` | `validate` | See the note below |
| `FLYWAY_ENABLED` | `true` | Run migrations on start |
| `JPA_SHOW_SQL` | `true` | Log generated SQL |

:::note The schema is owned by Flyway
Migrations in `src/main/resources/db/migration` build and version the schema; Hibernate only checks it. On first start Flyway applies `V1__baseline_schema.sql` and creates every table, so a fresh `createdb connectly_db` needs nothing else.

| `JPA_DDL_AUTO` | Behaviour |
|---|---|
| `validate` | Verify the schema matches the entities, change nothing *(default)* |
| `none` | Skip the check entirely |
| `update` | Let Hibernate apply additive changes — diverges from the migrations |
| `create-drop` | Rebuild on start, **drop on shutdown** — destroys data |

The default used to be `create-drop`, which rebuilt the schema on every start. `validate` earns its keep: it refuses to boot when an entity and its table disagree, rather than silently rewriting the table.
:::

### JWT

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | a committed dev key | HMAC-SHA256 signing key, **Base64-decoded** before use |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) | Token lifetime |

:::danger Rotate the signing key
The default `JWT_SECRET` is committed to the repository, so it is public. Anyone can forge a valid token against a deployment still using it. It exists so a fresh clone runs with no configuration; it is not safe anywhere else. Generate your own:

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

Both feed the single `CorsConfigurationSource` bean in `SecurityConfiguration`:

```bash
$ curl -i -X OPTIONS http://localhost:8080/posts \
    -H 'Origin: http://localhost:3000' \
    -H 'Access-Control-Request-Method: GET'
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE
```

:::info Previously inert
Two configurations existed and neither applied, because the filter chain never called `.cors(…)`. No browser frontend on another origin could reach the API. See [known issues](../reference/known-issues.md).
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

:::note One strength rule, both flows
`PASSWORD_MIN_LENGTH` plus an uppercase letter, a digit and a special character — enforced by **both** `POST /auth/registration` and `POST /auth/reset-password`. Registration used to enforce only `@NotBlank`.
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
