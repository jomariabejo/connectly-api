---
sidebar_position: 1
slug: /intro
title: Introduction
---

# Connectly API

A Spring Boot backend for a social platform: users post, comment and like, behind JWT authentication with email verification, password reset, and soft account deletion with a 30-day grace period.

This site is the reference for that API — how to run it, how it is put together, and what every endpoint actually does.

## Where to start

| If you want to… | Go to |
|---|---|
| Get it running locally | [Installation](./getting-started/installation.md) |
| Change a port, password or secret | [Configuration](./getting-started/configuration.md) |
| Call the API interactively | [Swagger UI](./api/swagger.md) |
| Look up an endpoint | [Authentication](./api/authentication.md), [Users](./api/users.md), [Posts](./api/posts.md), [Comments](./api/comments.md), [Likes](./api/likes.md) |
| Understand the layering | [Architecture overview](./architecture/overview.md) |
| Know what is rough | [Known issues](./reference/known-issues.md) |

## Tech stack

| | |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.4.4 (Web, Data JPA, Security, Validation, Mail, Cache, HATEOAS) |
| **Database** | PostgreSQL 15+ |
| **Auth** | JWT (jjwt 0.11), BCrypt password hashing, stateless sessions |
| **Mapping** | MapStruct 1.6 |
| **API docs** | springdoc-openapi 2.8 → Swagger UI + OpenAPI 3 |
| **Build** | Gradle 8.13 |
| **Tests** | JUnit 5 + Mockito 5 + MockMvc |

## Two things to know before your first request

**There is no `/api` prefix.** Posts live at `/posts`, not `/api/posts`. Users at `/users`. Comments are nested under their post at `/posts/{postId}/comments`.

**Everything outside `/auth/**` needs a bearer token.** Get one from `POST /auth/login` and send it as:

```
Authorization: Bearer <token>
```

Tokens last one hour by default (`JWT_EXPIRATION_MS`).

## A complete first run

```bash
# 1. Register — the account starts disabled
curl -X POST http://localhost:8080/auth/registration \
  -H 'Content-Type: application/json' \
  -d '{"username":"someone","email":"someone@example.com","password":"admin123"}'

# 2. Verify — the token arrives by email (read it at http://localhost:8025)
curl "http://localhost:8080/auth/verify?token=THE_TOKEN"

# 3. Log in — returns the JWT
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"someone@example.com","password":"admin123"}'

# 4. Use it
curl http://localhost:8080/users/me -H "Authorization: Bearer $JWT"
```

Step 2 is not optional: `POST /auth/login` rejects an unverified account.

## Repository layout

```
connectly-api/
├── src/main/java/com/jomariabejo/connectly_api/
│   ├── config/          SecurityConfiguration, JwtAuthenticationFilter, OpenApiConfig
│   ├── controller/      REST endpoints
│   ├── dto/             request and response shapes
│   ├── exception/       domain exceptions + GlobalExceptionHandler
│   ├── mapper/          MapStruct entity ⇄ DTO mappers
│   ├── model/           JPA entities
│   ├── repository/      Spring Data JPA repositories
│   ├── scheduled/       background cleanup tasks
│   └── service/         business logic
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql       reference DDL
│   └── docs/http-template/   runnable .http request samples
├── src/test/java/…      Mockito unit tests + controller slice tests
├── doc/api-documentation.yml  exported OpenAPI spec
├── documentation-central/     this site
└── .env.example         every configurable setting
```
