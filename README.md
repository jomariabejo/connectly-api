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

## 📚 Documentation

| Guide | Description |
|-------|-------------|
| [**Documentation index**](docs/README.md) | Start here — all guides in one place |
| [Local development](docs/getting-started/local-development.md) | `./gradlew bootRun`, Postgres, Mailpit |
| [Quick start (Docker)](docs/getting-started/quick-start.md) | Stage, demo, prod via Docker Compose |
| [Debugging](docs/getting-started/debugging.md) | Logs, Postman, common errors |
| [Deployment](docs/deployment/deployment.md) | CI/CD, DigitalOcean, multi-env |
| [Datadog](docs/deployment/datadog.md) | APM and monitoring |

**API testing:** Postman collection and `.http` files under `src/main/resources/docs/`. Use [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) for `.http` files.

**Base URL:** `http://localhost:8080/api` — all routes are under `/api/v1/...` (e.g. `/api/v1/auth/login`).

---

## 🛠️ Tech Stack

- **Backend:** Spring Boot 3
- **Database:** PostgreSQL
- **Authentication:** JWT
- **Email:** MailHog (local development)
- **Build:** Gradle

---

## 🚀 Getting Started

**Prerequisites:** Java 17+, PostgreSQL, Gradle (wrapper included). Optional: Docker for Mailpit and multi-env Compose.

```bash
git clone https://github.com/jomariabejo/connectly-api.git
cd connectly-api
./gradlew bootRun
```

API base URL: **http://localhost:8080/api** — try `GET /api/v1/public/hello`.

Full setup (database, Mailpit, Docker): **[Local development guide](docs/getting-started/local-development.md)**.

---

## 📡 API Endpoints (prefix `/api/v1`)

### Public
- `GET /api/v1/public/hello` — smoke test

### Authentication
- `POST /api/v1/auth/registration` — Register
- `POST /api/v1/auth/login` — Login
- `GET /api/v1/auth/verify?token=TOKEN` — Verify email

### Users
- `GET /api/v1/users/me` — Current user
- `PUT /api/v1/users/me` — Update profile

### Posts & comments
- `POST /api/v1/posts` — Create post
- `GET /api/v1/posts` — List posts
- `POST /api/v1/posts/{postId}/comments` — Add comment
- `POST /api/v1/posts/{postId}/likes/toggle` — Toggle like

### Orders & payments
- `POST /api/v1/orders` — Create order
- `PATCH /api/v1/orders/{id}/status` — Update status
- `POST /api/v1/payments/checkout` — Checkout session

See Postman collection or `src/main/resources/docs/http-template/` for the full API.

---

## 🧪 Testing API Requests

1. **Install REST Client** extension in VS Code
2. Open any file in `src/main/resources/docs/http-template/`
3. Click "Send Request" button above the request
4. View response in the sidebar

Example: `src/main/resources/docs/http-template/auth/register.http`

---

## 🤝 Contributing

Feel free to submit issues and enhancement requests!
