---
sidebar_position: 8
title: Errors
---

# Errors

## The error body

Every error routed through `GlobalExceptionHandler` returns the same shape:

```json
{
  "status": 404,
  "error": "Post not found",
  "message": "Post not found with id: 99",
  "timestamp": 1785801272830
}
```

| Field | Meaning |
|---|---|
| `status` | Repeats the HTTP status |
| `error` | Fixed category label for the exception type |
| `message` | The exception's own message — the specific detail |
| `timestamp` | Epoch milliseconds |

Some endpoints bypass this and return a bare string or a `GenericResponse` instead — noted below.

## Status reference

| Status | `error` | Raised by |
|---|---|---|
| `400` | `Validation failed` | A `@Valid` request body failed its constraints |
| `400` | `Account reactivation failed` | Bad/expired reactivation token, or the grace period has passed |
| `401` | `Invalid credentials` | Bad email or password |
| `401` | `Unauthorized access` | Authenticated, but not the owner of the resource |
| `404` | `Post not found` | No post with that id |
| `404` | `Comment not found` | No comment with that id |
| `409` | `User already exists` | Duplicate username |
| `409` | `Email already in use` | Duplicate email *(see the caveat below)* |
| `410` | `Account scheduled for deletion` | The account is inside its deletion grace period |
| `500` | `An unexpected error occurred` | Anything else |

### Validation failures

A `400` from bean validation lists the offending fields:

```json
{
  "status": 400,
  "error": "Validation failed",
  "message": "title: Title must be between 5 and 100 characters",
  "timestamp": 1785801272830
}
```

Multiple violations are joined with `, ` and sorted by field name.

## Things that surprise people

### `403`, not `401`, when the token is missing

Spring Security rejects unauthenticated requests before they reach a controller, and this configuration answers `403`:

```bash
$ curl -i http://localhost:8080/users/me
HTTP/1.1 403
```

A `401` from this API means "you are authenticated but not permitted" (`UnauthorizedAccessException`) — the opposite of the usual convention.

### An unknown URL returns `403`

```bash
$ curl -i http://localhost:8080/no/such/route
HTTP/1.1 403
```

`anyRequest().authenticated()` matches before routing, so an unmatched path is indistinguishable from an unauthorized one. Authenticated requests to unknown paths hit the catch-all handler and get `500` rather than `404`.

### `GET /posts/{id}` returns `403` for a post that does not exist

Deliberate — see [Posts](./posts.md). A missing post and someone else's post are made indistinguishable.

### Some "not found" cases return `500`

Not every missing-resource path throws a mapped exception. These throw a bare `RuntimeException`, which the catch-all turns into `500`:

| Call | Expected | Actual |
|---|---|---|
| `PUT /posts/{id}` on a missing post | `404` | `500` |
| `POST /auth/registration` with a duplicate email | `409` | `500` |

`EmailAlreadyInUseException` exists and maps to `409`, but `AuthenticationService.signup` throws `RuntimeException("Email already in use")` instead. See [known issues](../reference/known-issues.md).

### Password reset never reveals whether an account exists

`POST /auth/forgot-password/email` and `/otp` answer `200` with the same message either way, and `429` when rate limited. See [Authentication](./authentication.md).

### Endpoints that do not use the error envelope

| Endpoint | Error shape |
|---|---|
| `GET /auth/verify` | Plain string |
| `GET /auth/registrationConfirm` | Plain string |
| `POST /auth/forgot-password/*` | `GenericResponse` — `{"message": …, "data": null}` |
| `POST /auth/reset-password` | `GenericResponse` |
| `DELETE /users/admin/users/{id}` | Plain string |
| `PUT /users/admin/users/{id}/extend-deletion` | Plain string |
| `POST /users/reactivate` (success) | Plain string |

## Rate limiting

`429` comes only from the password-reset endpoints, governed by `PASSWORD_RESET_MAX_ATTEMPTS` within `PASSWORD_RESET_ATTEMPT_WINDOW_MINUTES`. Counters are per address **and** per flow, held in memory — a restart clears them, and they are not shared across instances.

```json
{ "message": "Too many requests. Please try again later.", "data": null }
```
