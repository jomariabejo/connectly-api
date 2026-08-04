---
sidebar_position: 1
title: Installation
---

# Installation

From a clean checkout to a working API in four steps. Only the first three are strictly required — the API boots without mail, you just cannot verify an account.

## Prerequisites

| | Why |
|---|---|
| **JDK 17+** | The Gradle toolchain pins Java 17 |
| **PostgreSQL 15+** | The only supported database — the schema uses `JSONB` |
| **Docker** *(optional)* | Easiest way to run a local mail catcher |

Gradle itself is not needed — the wrapper (`./gradlew`) downloads it.

## 1. Clone

```bash
git clone https://github.com/jomariabejo/connectly-api.git
cd connectly-api
```

## 2. Create the database

```bash
createdb connectly_db
```

The defaults expect `postgres` / `admin` on `localhost:5432`. Different credentials? See [Configuration](./configuration.md) — you do **not** edit `application.properties`.

:::warning The schema is rebuilt on every start
`JPA_DDL_AUTO` defaults to `create-drop`, so Hibernate drops and recreates every table when the app starts, and drops them again on shutdown. Convenient locally, total data loss anywhere else. Set `JPA_DDL_AUTO=validate` or `none` in `.env` for anything you want to keep.
:::

## 3. Run a mail catcher

Registration and password reset both send email. [Mailpit](https://mailpit.axllent.org/) accepts the messages and shows them in a browser instead of delivering them.

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

- **SMTP** — `localhost:1025` (where the app sends)
- **Web UI** — http://localhost:8025 (where you read)

Already using 1025 for something else? Point the app elsewhere without touching any tracked file:

```bash
MAIL_PORT=1026 ./gradlew bootRun
```

:::note Without a mail catcher
`POST /auth/registration` fails at the send step. Accounts are created disabled and can only be enabled by the emailed token, so registration is effectively blocked. Run the catcher.
:::

## 4. Start the API

```bash
./gradlew bootRun
```

| | |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI spec | http://localhost:8080/v3/api-docs (`.yaml` for YAML) |
| Mail inbox | http://localhost:8025 |

## Verify it works

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/registration \
  -H 'Content-Type: application/json' \
  -d '{"username":"someone","email":"someone@example.com","password":"StrongPass1!"}'

# 2. Open http://localhost:8025, read the verification mail, then:
curl "http://localhost:8080/auth/verify?token=THE_TOKEN_FROM_THE_EMAIL"

# 3. Log in and keep the token
JWT=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"someone@example.com","password":"StrongPass1!"}' | jq -r .token)

# 4. Call something protected
curl http://localhost:8080/users/me -H "Authorization: Bearer $JWT"
```

A `200` with your profile means everything is wired up.

## Run the tests

```bash
./gradlew unitTest   # 139 Mockito + MockMvc tests, no database required
./gradlew test       # the above plus ConnectlyApiApplicationTests, which needs PostgreSQL
```

See [the Mockito suite](../testing/mockito-suite.md) for what is covered.

## Troubleshooting

**`Connection to localhost:5432 refused`**
PostgreSQL is not running, or is on another port. Check with `pg_isready`, then set `DB_URL` in `.env`.

**`FATAL: password authentication failed for user "postgres"`**
Set `DB_USERNAME` and `DB_PASSWORD` in `.env` to your real credentials.

**`Mail server connection failed`**
No mail catcher on `MAIL_PORT`. Start Mailpit, or point `MAIL_PORT` at one that is already running.

**`Web server failed to start. Port 8080 was already in use.`**
Set `SERVER_PORT` in `.env`, or free the port.

**`401` on login for an account you just registered**
The account has not been verified. Open the mail inbox and follow the verification link.
