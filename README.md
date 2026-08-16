<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.4.4" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL 16" />
  <img src="https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle&logoColor=white" alt="Gradle 8.13" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?logo=jsonwebtokens&logoColor=white" alt="JWT Auth" />
  <img src="https://img.shields.io/badge/Flyway-CC0200?logo=flyway&logoColor=white" alt="Flyway" />
  <img src="https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D?logo=swagger&logoColor=black" alt="Swagger / OpenAPI 3" />
  <img src="https://img.shields.io/badge/Docusaurus-3.10-3ECC5F?logo=docusaurus&logoColor=white" alt="Docusaurus 3.10" />
  <img src="https://img.shields.io/badge/Tests-263_passing-brightgreen?logo=junit5&logoColor=white" alt="263 tests passing" />
  <img src="https://img.shields.io/badge/Endpoints-24-blue?logo=fastapi&logoColor=white" alt="24 endpoints" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?logo=opensourceinitiative&logoColor=white" alt="MIT License" />
</p>

# 🌐 Connectly API

> A Spring Boot backend for a social platform — users post, comment and like, behind **JWT authentication**, **email verification**, **password reset**, and **soft account deletion** with a 30-day grace period.
> Fully documented, fully tested, and it starts with no configuration at all.

---

## Documentation

| Where | What |
|-------|------|
| **[Documentation Central](documentation-central/)** | The full docs site — setup, architecture, endpoint reference, data model, testing, known issues |
| **Swagger UI** — `/swagger-ui.html` | Interactive console. Click **Authorize**, paste a token from `POST /auth/login`, call anything |
| **OpenAPI spec** — `/v3/api-docs` | Machine-readable (`.yaml` for YAML). Checked in at [`doc/api-documentation.yml`](doc/api-documentation.yml) |
| **[`http-template/`](src/main/resources/docs/http-template/)** | Runnable `.http` files for the VS Code REST Client |
| **[Postman setup](documentation-central/docs/api/postman.md)** | Import the spec by URL — the collection stays in sync with the API |

```bash
cd documentation-central && npm install && npm start   # docs at localhost:3000
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Java 17** |
| Framework | **Spring Boot 3.4.4** — Web, Data JPA, Security, Validation, Mail, Cache, HATEOAS |
| Database | **PostgreSQL 15+** (`JSONB` for post metadata) |
| Migrations | **Flyway** — schema is versioned, `ddl-auto=validate` |
| Auth | **JWT** (jjwt 0.11), BCrypt hashing, stateless sessions |
| Mapping | **MapStruct 1.6.3** |
| API Docs | **springdoc-openapi 2.8.6** → Swagger UI + OpenAPI 3 |
| Scheduling | **Spring `@Scheduled`** + **ShedLock** for multi-instance safety |
| Testing | **JUnit 5** + **Mockito 5** + **MockMvc** + **AssertJ**, JaCoCo coverage |
| Docs site | **Docusaurus 3.10** (TypeScript, Mermaid), deployed on **Vercel** |
| Build | **Gradle 8.13** |
| Mail (dev) | **Mailpit** |

---

## Features

- **Authentication**
  - Registration with emailed verification token (24 h)
  - Login by email → JWT (1 h, configurable)
  - Password reset by **link** or **one-time code**, rate limited per address and per flow
  - Password strength enforced identically at registration and reset
- **Posts** — full CRUD, owner-scoped reads, pagination and filtering
- **Comments** — nested under their post, author-only editing, pagination and filtering
- **Likes** — idempotent toggle endpoint, like counts, per-user like listing
- **Users**
  - Profile, paginated and filterable listings
  - **Soft account deletion** with a 30-day grace period and reactivation token
  - Admin endpoints: force-delete, extend grace period, list pending deletions
- **Role-based access** — `USER` granted at registration, `ADMIN` for privileged endpoints
- **Background jobs** — daily permanent-deletion sweep, hourly expired-token cleanup, both lock-protected
- **Zero-config startup** — every setting has a working default; `.env` only when you need to change one
- **263 unit and slice tests** that need no database (see [Testing](#testing))

---

## API Endpoints

There is **no `/api` prefix**. Send `Authorization: Bearer <token>` on everything outside `/auth/**`.
A missing or invalid token gets **401**; a valid token without the right ownership or role gets **403**.

### Authentication — public

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/auth/registration` | Register (password: 8+ chars, uppercase, digit, special) |
| `POST` | `/auth/login` | Log in, returns a JWT |
| `GET` | `/auth/verify?token=` | Verify an email address |
| `GET` | `/auth/registrationConfirm?token=` | Confirm from the emailed link |
| `POST` | `/auth/forgot-password/email` | Request a reset link |
| `POST` | `/auth/forgot-password/otp` | Request a reset one-time code |
| `POST` | `/auth/reset-password` | Complete a reset with a token or OTP |

### Users

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/users/me` | Current user's profile |
| `GET` | `/users/` | All users *(note the trailing slash)* |
| `GET` | `/users/paginated` | Users, paginated and filterable |
| `DELETE` | `/users/me` | Soft-delete → **202**, 30-day grace period |
| `POST` | `/users/reactivate` | Restore an account inside the grace period |

### Users — admin only (`ROLE_ADMIN`)

| Method | Path | Purpose |
|--------|------|---------|
| `DELETE` | `/users/admin/users/{id}` | Delete an account (`{"forceDelete": true}` skips the grace period) |
| `PUT` | `/users/admin/users/{id}/extend-deletion` | Extend a grace period |
| `GET` | `/users/admin/users/scheduled-deletion` | List accounts pending permanent deletion |

### Posts

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/posts` | Create → **201** |
| `GET` | `/posts` | All posts, paginated and filterable |
| `GET` | `/posts/{id}` | Single post (**403** if missing *or* not yours) |
| `PUT` | `/posts/{id}` | Update |
| `DELETE` | `/posts/{id}` | Delete → **204** |
| `GET` | `/posts/my-posts` | Caller's posts |
| `GET` | `/posts/my-posts/paginated` | Caller's posts, paginated |
| `GET` | `/posts/user/{userId}/paginated` | One user's posts, paginated |

### Comments

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/posts/{postId}/comments` | Add a comment → **201** |
| `GET` | `/posts/{postId}/comments` | A post's comments, paginated and filterable |
| `GET` | `/posts/{postId}/comments/{commentId}` | Single comment |
| `PUT` | `/posts/{postId}/comments/{commentId}` | Update |
| `DELETE` | `/posts/{postId}/comments/{commentId}` | Delete → **204** |

### Likes

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/{postId}/likes/toggle` | Like or unlike; `data` holds the resulting state |
| `GET` | `/{postId}/likes/count` | Like count (public posts only) |
| `GET` | `/{postId}/likes/my-likes` | Caller's likes across all posts |

---

## Project Structure

```text
connectly-api/
├── build.gradle
├── README.md
├── LICENSE
├── .env.example                        every configurable setting
├── doc/
│   └── api-documentation.yml           exported OpenAPI 3 spec
├── documentation-central/              Docusaurus docs site (Vercel)
│   ├── docs/
│   │   ├── getting-started/            installation, configuration
│   │   ├── api/                        endpoint reference, errors, pagination
│   │   ├── architecture/               overview, security, scheduled tasks
│   │   ├── data-model/                 schema + ERD
│   │   ├── testing/                    the Mockito suite
│   │   └── reference/                  known issues, further reading
│   ├── docusaurus.config.ts
│   └── vercel.json
├── src/
│   ├── main/
│   │   ├── java/com/jomariabejo/connectly_api/
│   │   │   ├── config/                 security, JWT filter, OpenAPI, ShedLock
│   │   │   ├── controller/             REST endpoints
│   │   │   ├── dto/                    request & response shapes
│   │   │   ├── exception/              domain exceptions + GlobalExceptionHandler
│   │   │   ├── mapper/                 MapStruct entity ⇄ DTO
│   │   │   ├── model/                  JPA entities
│   │   │   ├── repository/             Spring Data JPA
│   │   │   ├── scheduled/              background cleanup jobs
│   │   │   └── service/                business logic
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/           Flyway migrations
│   │       └── docs/http-template/     runnable .http request samples
│   └── test/java/com/jomariabejo/connectly_api/
│       ├── service/                    7 Mockito unit test classes
│       └── controller/                 6 @WebMvcTest slice classes
└── gradlew
```

---

## Getting Started

### Prerequisites

- **JDK 17+** (`java --version`)
- **PostgreSQL 15+** running locally (`pg_isready`)
- **Docker** *(optional — for the local mail catcher)*
- **Node 18+** *(optional — only to run the docs site)*

Gradle is not needed; the wrapper downloads it.

### 1. Clone

```bash
git clone https://github.com/jomariabejo/connectly-api.git
cd connectly-api
```

### 2. Configure *(optional)*

```bash
cp .env.example .env
```

Every setting has a working local default, so **you can skip this entirely**. Copy the file only when something differs — a database password, an SMTP port, your own JWT secret. `.env.example` documents every variable; `.env` is gitignored.

Real environment variables take precedence over `.env`, so CI and production set the same names without shipping a file.

### 3. Create the database

```bash
createdb connectly_db
```

That is all — **Flyway creates every table and seeds the roles on first start**. Defaults to `postgres` / `admin` on `localhost:5432`; override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

### 4. Run a mail catcher

Registration and password reset both send email. Mailpit accepts the messages and shows them in a browser.

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

### 5. Run the application

```bash
./gradlew bootRun
```

| | |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI spec | http://localhost:8080/v3/api-docs |
| Mail inbox | http://localhost:8025 |

### Verify it works

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/registration \
  -H 'Content-Type: application/json' \
  -d '{"username":"someone","email":"someone@example.com","password":"StrongPass1!"}'

# 2. Read the verification mail at http://localhost:8025, then:
curl "http://localhost:8080/auth/verify?token=THE_TOKEN_FROM_THE_EMAIL"

# 3. Log in and keep the token
JWT=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"someone@example.com","password":"StrongPass1!"}' | jq -r .token)

# 4. Call something protected
curl http://localhost:8080/users/me -H "Authorization: Bearer $JWT"
```

A `200` with your profile means everything is wired up.

---

## Testing

**263 tests** across 27 classes. **None of them need a database, a mail server, or a running application.**

```bash
./gradlew unitTest   # 263 tests — no database required
./gradlew test       # adds ConnectlyApiApplicationTests, which needs PostgreSQL
```

### Two Gradle tasks

| Task | Runs | Needs PostgreSQL? |
|------|------|-------------------|
| **`unitTest`** | Every Mockito unit test and controller slice | ❌ No |
| **`test`** | The above **plus** `ConnectlyApiApplicationTests` | ✅ Yes |

`ConnectlyApiApplicationTests.contextLoads()` is a `@SpringBootTest`, so it starts the whole application. `unitTest` excludes it by name — use it in CI.

### Service unit tests — 143 tests

Pure Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock` collaborators, `@InjectMocks` subject. No Spring context.

| Class | What it tests |
|-------|---------------|
| **`PostServiceTest`** | Author assignment, ownership on read/update/delete, pagination envelope, filter forwarding |
| **`CommentServiceTest`** | Post/author linking, author-only edits, `getComment` scoping to its `{postId}` |
| **`UserServiceTest`** | Soft-delete lifecycle — 30-day scheduling, reactivation, grace-period boundaries, dependant-ordered purge |
| **`AuthenticationServiceTest`** | Signup hashing, role grant, password strength, login, verification, both reset flows, rate limiting, `SecurityContext` lookup |
| **`PostLikeServiceTest`** | Toggle on/off, idempotent create, private/null-privacy posts reporting not-found, is-liked lookup |
| **`JwtServiceTest`** | Token round-trip, extra claims, expiry, wrong user, foreign signature, malformed/empty tokens |
| **`RateLimitingServiceTest`** | Threshold behaviour, per-address and per-action isolation, window expiry |
| **`PasswordResetTokenServiceTest`** | Link and OTP token issue, prior-token invalidation, expiry/used/attempt-limit validation order |
| **`VerificationTokenServiceTest`** | Token replacement on re-issue, expired-token burn, enable-on-verify |
| **`CustomUserDetailsServiceTest`** | Login-time grace-period handling — auto-reactivation, deletion-scheduled rejection, null-schedule safety |
| **`EmailServiceTest`** | Recipient/subject/body of all three mails, wrapped failures |
| **`AuditServiceTest`** | Audit event markers and timestamp format (via a Logback `ListAppender`) |

### Controller slice tests — 82 tests

`@WebMvcTest` with `MockMvc`: real routing, real JSON serialization, real validation — mocked services.

| Class | What it tests |
|-------|---------------|
| **`AuthenticationControllerTest`** | Registration, `@Valid` rejection, login payload + validation, verification, both reset flows, the 429 path, 409/401/410 error bodies |
| **`PostControllerTest`** | CRUD status codes, `@PageableDefault` binding, filter switching, the deliberate 403s, update validation |
| **`CommentControllerTest`** | Nested routes, 201/204, author-only edits, pagination defaults, blank-text/malformed-JSON/415/405 rejections |
| **`UserControllerTest`** | Profile, the **202** on `DELETE /users/me`, 30-day reactivation-token expiry, reactivation validation, admin endpoints |
| **`PostLikeControllerTest`** | Toggle semantics, like counting, regression guard on the route shape |
| **`UserControllerSecurityTest`** | Filters **on**: `@PreAuthorize` on `/users/admin/**` denies plain users (403), challenges anonymous callers (401) |

### Component tests — 38 tests

Direct unit tests for the pieces between the layers.

| Class | What it tests |
|-------|---------------|
| **`GlobalExceptionHandlerTest`** | Every exception→status mapping and the full `ErrorResponse` body |
| **`JwtAuthenticationFilterTest`** | Bearer parsing, context population, pass-through and error delegation |
| **`JwtAuthenticationEntryPointTest`** / **`JwtAccessDeniedHandlerTest`** | The 401/403 JSON bodies |
| **`ScheduledDeletionTaskTest`** / **`PasswordResetTokenCleanupTaskTest`** | Delegation and exception swallowing in the background jobs |
| **`RegistrationListenerTest`** | Verification-token creation on the registration event |
| **`OtpGeneratorTest`** | Six-digit zero-padded OTPs, canonical UUID tokens |

Coverage: `./gradlew test jacocoTestReport` → `build/reports/jacoco/test/html/`.

Full conventions — `@MockitoBean` vs `@MockBean`, `ReflectionTestUtils` for `@Value` fields, the shared `@ControllerSliceTest` annotation — are in **[the testing guide](documentation-central/docs/testing/mockito-suite.md)**.

---

## Sample Requests & Responses

<details>
<summary><strong>Register → verify → login</strong></summary>

```text
POST /auth/registration
{"username":"someone","email":"someone@example.com","password":"StrongPass1!"}

200 OK
{
  "id": 1,
  "username": "someone",
  "email": "someone@example.com",
  "enabled": false,
  "roles": ["USER"],
  "autoReactivationEnabled": true
}

GET /auth/verify?token=a8561612-d690-4a47-8e5e-f9e5fb886a81
200 OK
Email verified successfully. You can now login.

POST /auth/login
{"email":"someone@example.com","password":"StrongPass1!"}

200 OK
{"token":"eyJhbGciOiJIUzI1NiJ9…","expiresIn":3600000}
```

No password hash and no verification token in any response.

</details>

<details>
<summary><strong>Paginated listing</strong></summary>

```text
GET /posts?page=1&size=5&sort=createdAt,desc
Authorization: Bearer <token>

200 OK
{
  "content": [ … ],
  "pageNumber": 1,
  "pageSize": 5,
  "totalElements": 12,
  "totalPages": 3,
  "hasNext": true,
  "hasPrevious": true
}
```

Supplying any filter (`title`, `content`, `postType`, `privacy`, `createdById`) switches to the filtered query.

</details>

<details>
<summary><strong>Like toggle</strong></summary>

```text
POST /1/likes/toggle
200 OK  {"message":"Post liked successfully.","data":true}

POST /1/likes/toggle
200 OK  {"message":"Post unliked successfully.","data":false}

GET /1/likes/count
200 OK  {"message":"Total likes retrieved.","data":3}
```

One endpoint for both directions — `data` is the resulting state, not the action.

</details>

<details>
<summary><strong>Account deletion (202, not 204)</strong></summary>

```text
DELETE /users/me
{"autoReactivationEnabled": true}

202 Accepted
{
  "message": "Account has been marked for deletion",
  "deletedAt": "2026-08-04T07:54:32",
  "scheduledDeletionAt": "2026-09-03T07:54:32",
  "gracePeriodDays": 30,
  "autoReactivationEnabled": true
}
```

Deletion is *scheduled*, not done. The account survives for 30 more days and stays invisible to every query meanwhile.

</details>

<details>
<summary><strong>Errors</strong></summary>

```text
# No token
401  {"status":401,"error":"Unauthorized",
      "message":"Authentication required. Send a bearer token from POST /auth/login."}

# Valid token, someone else's post
403  {"status":403,"error":"Forbidden","message":"…is not authorized…"}

# Validation failure
400  {"status":400,"error":"Validation failed",
      "message":"title: Title must be between 5 and 100 characters"}

# Weak password
400  {"status":400,"error":"Password too weak",
      "message":"Password must be at least 8 characters long and contain
                 uppercase, number, and special character"}

# Unknown URL (authenticated)
404  {"status":404,"error":"Not Found","message":"No static resource no/such/route."}
```

</details>

---

## Configuration

Nothing is hardcoded. Every property is a placeholder with a fallback:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/connectly_db}
```

Resolved in order — first one that exists wins:

1. A real **environment variable**
2. A **`.env`** file in the project root
3. The **default** in `application.properties`

`.env` is read by Spring Boot itself via `spring.config.import=optional:file:.env[.properties]` — no dotenv library involved. Full variable reference in **[the configuration guide](documentation-central/docs/getting-started/configuration.md)**.

> [!IMPORTANT]
> The default `JWT_SECRET` is committed so a fresh clone runs with no setup — which means it is **public**. Generate your own with `openssl rand -hex 32` for anything that is not your laptop.

> [!TIP]
> Before deploying publicly: set `SWAGGER_ENABLED=false` and drop `LOG_LEVEL_SECURITY` to `WARN`.

---

## Constraints and Notes

- **Schema is owned by Flyway.** Migrations live in `src/main/resources/db/migration`; Hibernate runs with `ddl-auto=validate` and refuses to start if the entities and tables disagree. Add `V2__your_change.sql` rather than editing `V1`.
- **Soft delete runs through every query.** Repositories filter `deletedAt IS NULL` explicitly — a derived query added without that clause will leak deleted users' data.
- **Ownership is enforced in the service layer**, not by path rules. `SecurityConfiguration` only distinguishes public / `USER` / `ADMIN`.
- **`GET /posts/{id}` returns 403 for a missing post** by design, so the endpoint cannot be used to discover which IDs exist.
- **Rate-limit counters are in memory** — cleared on restart, not shared between instances.
- **`ADMIN` has no grant endpoint.** Registration assigns `USER`; promote via SQL (see [Security](documentation-central/docs/architecture/security.md)).
- Remaining rough edges are tracked honestly in **[Known Issues](documentation-central/docs/reference/known-issues.md)**, each with the evidence behind it.

---

## Acknowledgments

This project was developed with the assistance of AI tools, including Claude (Anthropic).
AI was used for code suggestions, debugging, testing, and documentation.

All AI-generated outputs were reviewed, modified, and validated by the author.

---

## License

MIT — see [LICENSE](LICENSE).
