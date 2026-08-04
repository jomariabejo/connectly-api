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
| `400` | `Password too weak` | Password fails the strength rules (registration or reset) |
| `401` | `Unauthorized` | No token, or a malformed/expired one |
| `401` | `Invalid credentials` | Bad email or password |
| `403` | `Forbidden` | Authenticated, but not the owner of the resource — or missing a role |
| `404` | `Post not found` | No post with that id |
| `404` | `Comment not found` | No comment with that id |
| `404` | `Not Found` | Unknown URL |
| `405` | `Method Not Allowed` | Wrong HTTP verb for that path |
| `409` | `User already exists` | Duplicate username |
| `409` | `Email already in use` | Duplicate email |
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

### `401` vs `403`

The conventional split:

| Situation | Status |
|---|---|
| No token, malformed token, expired token | `401` |
| Valid token, not the owner of the resource | `403` |
| Valid token, missing a required role | `403` |

```bash
$ curl -i http://localhost:8080/users/me
HTTP/1.1 401
{"status":401,"error":"Unauthorized",
 "message":"Authentication required. Send a bearer token from POST /auth/login."}
```

### An unauthenticated request to an unknown URL returns `401`

Security runs before routing, so an unmatched path is indistinguishable from an unauthorized one until you are authenticated. **With** a valid token you get a proper `404`.

### `GET /posts/{id}` returns `403` for a post that does not exist

Deliberate — see [Posts](./posts.md). A missing post and someone else's post are made indistinguishable.

### Framework errors keep their own status

Anything Spring raises with a status of its own is passed through rather than flattened to `500`:

```bash
$ curl -H "Authorization: Bearer $JWT" http://localhost:8080/no/such/route
{"status":404,"error":"Not Found","message":"No static resource no/such/route."}

$ curl -X PATCH -H "Authorization: Bearer $JWT" http://localhost:8080/posts/1   # 405
$ curl -X POST  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
    -d 'not json' http://localhost:8080/posts                                    # 400
```

All of these used to return `500`. See [known issues](../reference/known-issues.md).

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
