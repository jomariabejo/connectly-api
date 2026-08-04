---
sidebar_position: 1
title: Known issues
---

# Known issues

Behaviours that are surprising, wrong, or worth fixing — each one observed against a running instance, not inferred from reading code.

Two were fixed while writing these docs and are recorded at the bottom.

---

## Security

### The JWT signing key is committed

`application.properties` ships a default `jwt.secret-key`. It is in the public repository, so anyone can mint valid tokens against a deployment that has not overridden it.

**Fix:** set `JWT_SECRET` from `openssl rand -hex 32`. See [Configuration](../getting-started/configuration.md).

### The Bump.sh API token is committed in plaintext

[`.github/workflows/bump.yml`](https://github.com/jomariabejo/connectly-api/blob/main/.github/workflows/bump.yml) has a literal `token:` value on two jobs.

**Fix:** rotate it in the Bump.sh dashboard, then reference `${{ secrets.BUMP_TOKEN }}`. Rotation matters more than the code change — the current value is already public.

### Responses leak the password hash

`POST /auth/registration` and `GET /users/me` return the raw `User` entity, including the BCrypt hash and the verification token. `CommentResponseDto.user` embeds it too, so every comment listing carries the author's hash.

```json
{
  "id": 1,
  "password": "$2a$10$HqgA60KmLNFh34j0TFX7V…",
  "verificationToken": "bd7ce86a-7b1c-478c-b0fe-a458d3ee5055"
}
```

The verification token is the worse half: anyone who sees the registration response can activate the account without the mailbox.

**Fix:** return a `UserDto` projection. One already exists at `dto/user/UserDto.java` and is unused.

### Nothing ever assigns a role

Registration creates users with an empty `roles` set and no endpoint grants one, so every account has zero authorities. `/user/**`, `/admin/**` and the three `@PreAuthorize("hasRole('ADMIN')")` endpoints are unreachable by anyone.

**Workaround:** insert into `user_roles` by hand — and note `JPA_DDL_AUTO=create-drop` wipes it on restart.

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r
WHERE u.email = 'you@example.com' AND r.name = 'ADMIN';
```

**Fix:** assign `ROLE_USER` during signup, and add an admin-only role-management endpoint.

### Registration accepts weak passwords

`RegisterUserDto.password` carries only `@NotBlank`. The uppercase / digit / special-character rules live exclusively in `AuthenticationService.isPasswordStrong`, which is called from `resetPassword` and nothing else. `a` is a valid registration password.

**Fix:** call `isPasswordStrong` from `signup` too.

### CORS is configured twice and applied never

Two configurations exist — the `spring.web.cors.*` properties and a `CorsConfigurationSource` bean hardcoding `http://localhost:8080` with `GET,POST`. The filter chain never calls `.cors(…)`, so neither is used and preflight requests fail:

```bash
$ curl -i -X OPTIONS http://localhost:8080/posts \
    -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: GET'
HTTP/1.1 403
```

No browser frontend on another origin can call this API.

**Fix:** add `.cors(Customizer.withDefaults())` to the filter chain, delete the duplicate configuration, and drive the surviving one from `CORS_ALLOWED_ORIGINS`.

---

## Correctness

### `/auth/registrationConfirm` rejects the token users receive

Registering writes **two different tokens** for one account:

| Token | Stored in | Checked by | Emailed? |
|---|---|---|---|
| `472098eb-…` | `app_user.verification_token` | `GET /auth/verify` | ✅ |
| `ee162fd2-…` | `verification_token` table | `GET /auth/registrationConfirm` | ❌ |

`AuthenticationService.signup` writes the column; `RegistrationListener` independently generates a second UUID for the table. The email carries the first.

```bash
$ curl "http://localhost:8080/auth/registrationConfirm?token=<emailed token>"
Invalid verification token          # 400
$ curl "http://localhost:8080/auth/verify?token=<emailed token>"
Email verified successfully.        # 200
```

`/auth/registrationConfirm` also never checks expiry.

**Fix:** have `RegistrationListener` reuse the token already on the user, or remove the endpoint.

### Deleted accounts with content are never purged

`ScheduledDeletionTask` hard-deletes expired accounts, but every foreign key to `app_user` in the generated schema is `NO ACTION`:

```
ERROR: update or delete on table "app_user" violates foreign key constraint
       "fk37mjvnvpwbqdpewm39q75h9q" on table "comment"
```

The task catches and logs the failure, so an account with a single post, comment or like stays soft-deleted **forever**. Only empty accounts are removed. Users are told their data will be gone in 30 days; for most of them it will not be.

**Fix:** delete the dependent rows first, or declare cascades on the entity associations.

### `schema.sql` never runs and has drifted

`spring.sql.init.mode=never`, so the file is dead reference material. It has already diverged from the entities — it declares `ON DELETE CASCADE` on three foreign keys that are `NO ACTION` at runtime, and names a column `verificationToken` where Hibernate generates `verification_token`.

**Fix:** adopt Flyway or Liquibase and set `JPA_DDL_AUTO=validate`.

### `create-drop` is the default

Every restart drops and recreates the schema. Correct for local work, catastrophic anywhere else, and easy to inherit by accident.

**Fix:** set `JPA_DDL_AUTO=validate` outside development.

### `getComment` ignores its `postId` and caller

`CommentService.getComment(postId, commentId, user)` uses only `commentId`. A mismatched `postId` still resolves the comment, and any authenticated user can read any comment — unlike posts, which are owner-scoped.

**Fix:** decide whether comments are public within a post (then drop the unused parameters) or owner-scoped (then enforce it).

---

## Status codes

### Missing tokens give 403, not 401

Spring Security rejects unauthenticated requests with `403`. Meanwhile `UnauthorizedAccessException` — raised when you *are* authenticated but do not own the resource — maps to `401`. That is backwards from the convention.

**Fix:** add an `AuthenticationEntryPoint` returning `401`, and remap `UnauthorizedAccessException` to `403`.

### Some missing resources give 500

| Call | Expected | Actual | Cause |
|---|---|---|---|
| `PUT /posts/{id}` on a missing post | `404` | `500` | `PostService.updatePost` throws a bare `RuntimeException` |
| `POST /auth/registration` with a duplicate email | `409` | `500` | `signup` throws `RuntimeException`, not the `EmailAlreadyInUseException` that exists and maps to 409 |

**Fix:** throw the mapped domain exceptions.

### Unmatched URLs give 500 for authenticated callers

The catch-all `@ExceptionHandler(Exception.class)` intercepts Spring's own `NoResourceFoundException`, so an authenticated request to an unknown path returns `500` instead of `404`. (Unauthenticated ones get `403` from the security layer first.)

**Fix:** add `@ExceptionHandler(ErrorResponseException.class)` that preserves the framework's status, ahead of the catch-all.

---

## Maintenance

### A stray duplicate of the request templates

`src/main/resources/docs/http-template copy/` is an unmaintained copy of `http-template/`. It still contains the old, incorrect `/api/...` URLs. The canonical directory has been corrected; the copy has not.

**Fix:** delete it.

### `VerificationTokenService.cleanupExpiredTokens()` is never called

The method exists but carries no `@Scheduled` annotation, so expired verification tokens accumulate indefinitely.

### Scheduled jobs have no locking

Both background tasks use plain Spring scheduling. Every replica runs every job, so scaling past one instance means concurrent deletion sweeps.

**Fix:** ShedLock, or move the work to an external scheduler.

---

## Fixed while writing these docs

### The application could not start *(fixed)*

`PostLikeController` declared `@PostMapping("/{postId}/likes/toggle")` beneath a class-level `@RequestMapping("/{postId}")`. The two concatenated into `/{postId}/{postId}/likes/toggle`, and Spring Boot 3's `PathPatternParser` refuses to capture the same variable twice:

```
APPLICATION FAILED TO START

Invalid mapping pattern detected:
/{postId}/{postId}/likes/toggle
          ^
Not allowed to capture 'postId' twice in the same pattern
```

The context aborted, so **no endpoint served any request**. The route now matches its siblings at `/{postId}/likes/toggle`, and `PostLikeControllerTest` guards the shape. Because the endpoint had never successfully served a request, this was not a breaking change.

### Validation errors returned 500 *(fixed)*

`GlobalExceptionHandler`'s catch-all claimed `MethodArgumentNotValidException` before Spring's `DefaultHandlerExceptionResolver` could map it, because `ExceptionHandlerExceptionResolver` runs first. Every malformed request body in the API came back as `500`.

An explicit handler now returns `400` with the offending fields:

```json
{
  "status": 400,
  "error": "Validation failed",
  "message": "title: Title must be between 5 and 100 characters",
  "timestamp": 1785801272830
}
```
