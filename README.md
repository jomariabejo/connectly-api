# 🌐 Connectly API

A robust, scalable backend for a modern social platform — enabling users to post, comment, and engage with content while ensuring **secure authentication**, **privacy controls**, and **role-based access**. Built with integration and analytics in mind.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Auth-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![GitHub Stars](https://img.shields.io/github/stars/jomariabejo/connectly-api?style=flat-square&logo=github)
![Build Status](https://img.shields.io/badge/Status-Active-success?style=flat-square)

---

## ✨ Features

- 🧑‍🤝‍🧑 User Authentication with email verification
- 🔐 Privacy & Role-based Access Control
- 📝 Post & Comment System with likes
- 📊 Full REST API
- 🔧 Easy local development setup
- 💬 RESTful API Design

---

## 📖 Documentation

| Where | What |
|---|---|
| **[Documentation Central](documentation-central/)** | The full docs site — setup, architecture, endpoint reference, data model, testing. Run it with `cd documentation-central && npm install && npm start` |
| **Swagger UI** — http://localhost:8080/swagger-ui.html | Interactive API console. Click **Authorize**, paste a token from `POST /auth/login`, and call any endpoint |
| **OpenAPI spec** — http://localhost:8080/v3/api-docs | Machine-readable spec (add `.yaml` for YAML). Checked in at [`doc/api-documentation.yml`](doc/api-documentation.yml) |

---

## 📚 API Testing

Every endpoint has a `.http` file under [`src/main/resources/docs/http-template/`](src/main/resources/docs/http-template/) for the VS Code REST Client extension.

👉 Install [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client), open any file, replace `TOKEN_HERE` with a token from `POST /auth/login`, and click "Send Request".

---

## 🛠️ Tech Stack

- **Backend:** Spring Boot 3
- **Database:** PostgreSQL
- **Authentication:** JWT
- **Email:** MailHog (local development)
- **Build:** Gradle

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **PostgreSQL** (running locally)
- **Gradle** (included with `./gradlew`)
- **Docker** (optional, for MailHog)

### Quick Setup

#### 1. Clone & Navigate
```bash
git clone https://github.com/jomariabejo/connectly-api.git
cd connectly-api
```

#### 2. Configure (optional)
```bash
cp .env.example .env
```

Every setting has a working local default, so **you can skip this entirely** and `./gradlew bootRun` still works. Copy the file only when you need to change something — a different database password, an SMTP port, your own JWT secret. `.env.example` documents every variable; `.env` is gitignored.

Real environment variables take precedence over `.env`, so CI and production can set the same names without shipping a file.

#### 3. Setup PostgreSQL Database
```bash
createdb connectly_db
```

That is all — Flyway creates every table and seeds the roles on first start. Defaults to `postgres`/`admin` on `localhost:5432`; set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in `.env` to change that.

The schema is owned by migrations in `src/main/resources/db/migration`, and Hibernate runs with `ddl-auto=validate` so it refuses to start if the entities and tables disagree.

#### 4. Setup Local Email with Mailpit
Mailpit captures outgoing mail locally so you can read verification and password-reset messages:

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

Once running:
- 📧 **SMTP Server:** `localhost:1025` (app sends emails here)
- 🌐 **Web UI:** http://localhost:8025 (view sent emails)

#### 4. Run the Application
```bash
./gradlew bootRun
```

The API will be running at `http://localhost:8080`, with Swagger UI at `http://localhost:8080/swagger-ui.html`.

---

## 📡 API Endpoints

There is **no `/api` prefix**. Send `Authorization: Bearer <token>` on everything outside `/auth/**`. A missing or invalid token gets **401**; a valid token without the right ownership or role gets **403**.

### Authentication (public)
- `POST /auth/registration` — Register a new account (password needs 8+ chars, uppercase, digit, special)
- `POST /auth/login` — Log in, returns a JWT
- `GET /auth/verify?token=TOKEN` — Verify an email address
- `GET /auth/registrationConfirm?token=TOKEN` — Confirm from the emailed link
- `POST /auth/forgot-password/email` — Request a reset link
- `POST /auth/forgot-password/otp` — Request a reset one-time code
- `POST /auth/reset-password` — Complete a reset with a token or OTP

### Users
- `GET /users/me` — Current user's profile
- `GET /users/` — All users (note the trailing slash)
- `GET /users/paginated` — Users, paginated and filterable
- `DELETE /users/me` — Soft-delete the account → **202**, 30-day grace period
- `POST /users/reactivate` — Restore an account inside the grace period

### Users — admin only (`ROLE_ADMIN`)
- `DELETE /users/admin/users/{id}` — Delete an account (`{"forceDelete": true}` to skip the grace period)
- `PUT /users/admin/users/{id}/extend-deletion` — Extend a grace period
- `GET /users/admin/users/scheduled-deletion` — List accounts pending permanent deletion

### Posts
- `POST /posts` — Create a post → **201**
- `GET /posts` — All posts, paginated and filterable
- `GET /posts/{id}` — Single post (**403** if missing *or* not yours)
- `PUT /posts/{id}` — Update a post
- `DELETE /posts/{id}` — Delete a post → **204**
- `GET /posts/my-posts` — Caller's posts
- `GET /posts/my-posts/paginated` — Caller's posts, paginated
- `GET /posts/user/{userId}/paginated` — One user's posts, paginated

### Comments
- `POST /posts/{postId}/comments` — Add a comment → **201**
- `GET /posts/{postId}/comments` — A post's comments, paginated and filterable
- `GET /posts/{postId}/comments/{commentId}` — Single comment
- `PUT /posts/{postId}/comments/{commentId}` — Update a comment
- `DELETE /posts/{postId}/comments/{commentId}` — Delete a comment → **204**

### Likes
- `POST /{postId}/likes/toggle` — Like or unlike; `data` holds the resulting state
- `GET /{postId}/likes/count` — Like count (public posts only)
- `GET /{postId}/likes/my-likes` — Caller's likes across all posts

---

## 🧪 Testing

```bash
./gradlew unitTest   # 143 Mockito unit tests + controller slices — no database needed
./gradlew test       # adds ConnectlyApiApplicationTests, which needs PostgreSQL running
```

To exercise the API by hand: install the **REST Client** extension in VS Code, open a file under `src/main/resources/docs/http-template/`, swap `TOKEN_HERE` for a real token, and click "Send Request".

Example: `src/main/resources/docs/http-template/auth/register.http`

---

## 🤝 Contributing

Feel free to submit issues and enhancement requests!
