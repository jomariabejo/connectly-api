---
sidebar_position: 1
title: Known issues
---

# Known issues

Everything on this page was observed against a running instance, not inferred from reading code.

Most of what used to be listed here **has since been fixed** — those entries moved to [Fixed](#fixed), each with the evidence that it is actually resolved. What remains open is below.

---

## Open

### The JWT signing key ships with a public default

`application.properties` carries a working `jwt.secret-key` so a fresh clone runs with no configuration. That value is in the public repository, so anyone can mint valid tokens against a deployment that has not overridden it.

Not "fixed" because removing the default would break zero-config startup, which is the point of it.

**What to do:** set `JWT_SECRET` from `openssl rand -hex 32` for anything that is not your laptop. See [Configuration](../getting-started/configuration.md).

### A Bump.sh token is still in the git history

The Bump.sh workflow has been removed, but the API token it once carried in plaintext remains in earlier commits. Deleting the file does not un-publish it.

**What to do:** revoke that token in the Bump.sh dashboard. Rewriting history is not worth it for a credential that can simply be revoked.

### `GET /posts/{id}` returns 403 for a post that does not exist

Deliberate, not a defect — a missing post and someone else's post are made indistinguishable so the endpoint cannot be used to discover which IDs exist. Documented on [Posts](../api/posts.md).

### Scheduled jobs assume one instance per environment

[ShedLock](../architecture/scheduled-tasks.md) now stops replicas duplicating work, but the jobs still have no retry: a failure waits for the next scheduled run. Fine at this scale; worth revisiting if the deletion sweep becomes business-critical.

### Registration still sends only one email, and it is plain text

`RegistrationListener` no longer sends a duplicate, but the surviving message from `EmailService` is unstyled plain text and the `MessageSource` bundle the listener used to read is now unused.

---

## Fixed

### The application could not start

`PostLikeController` declared `@PostMapping("/{postId}/likes/toggle")` beneath a class-level `@RequestMapping("/{postId}")`. The two concatenated into `/{postId}/{postId}/likes/toggle`, and Spring Boot 3's `PathPatternParser` refuses to capture the same variable twice:

```
APPLICATION FAILED TO START

Invalid mapping pattern detected:
/{postId}/{postId}/likes/toggle
          ^
Not allowed to capture 'postId' twice in the same pattern
```

The context aborted, so **no endpoint served any request**. The route now sits at `/{postId}/likes/toggle` alongside its siblings, and `PostLikeControllerTest` fails at context load if a duplicate segment ever returns.

### Malformed request bodies returned 500

`GlobalExceptionHandler`'s catch-all claimed `MethodArgumentNotValidException` before Spring's `DefaultHandlerExceptionResolver` could map it, because `ExceptionHandlerExceptionResolver` runs first. An explicit handler now returns 400 listing the offending fields.

### Responses leaked the password hash and verification token

`POST /auth/registration`, `GET /users/me`, the user listings and every embedded comment author serialized the raw `User` entity — BCrypt hash and verification token included. The token was the worse half: anyone who saw a registration response could activate the account without the mailbox.

All of them now return [`UserResponseDto`](https://github.com/jomariabejo/connectly-api/blob/main/src/main/java/com/jomariabejo/connectly_api/dto/user/UserResponseDto.java), which omits `password`, `verificationToken` and `expiryDate`:

```json
{
  "id": 1,
  "username": "someone",
  "email": "someone@example.com",
  "enabled": false,
  "roles": ["USER"],
  "autoReactivationEnabled": true
}
```

### Nothing assigned a role

Registration left `roles` empty, so every account had zero authorities and `/user/**`, `/admin/**` and all three `@PreAuthorize` endpoints were unreachable by anyone. Signup now grants `USER`, and the Flyway baseline seeds the `role` table. Verified: `GET /users/admin/users/scheduled-deletion` answers 200 for an account holding `ADMIN`.

### Registration accepted weak passwords

`RegisterUserDto` enforced only `@NotBlank` while the strength rules lived solely in the reset flow, so an account could be created with a password its owner could never reset back to. Both paths now share `isPasswordStrong`:

```bash
$ curl -X POST localhost:8080/auth/registration -d '{"…","password":"admin123"}'
{"status":400,"error":"Password too weak",
 "message":"Password must be at least 8 characters long and contain uppercase, number, and special character"}
```

### CORS was configured twice and applied never

The filter chain never called `.cors(…)`, so neither the properties nor the `CorsConfigurationSource` bean took effect and no browser frontend on another origin could reach the API. The chain now calls it, and the bean reads `CORS_ALLOWED_ORIGINS` / `CORS_ALLOWED_METHODS` rather than hardcoding a disagreeing second copy:

```bash
$ curl -i -X OPTIONS localhost:8080/posts -H 'Origin: http://localhost:3000' \
    -H 'Access-Control-Request-Method: GET'
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE
```

### `/auth/registrationConfirm` rejected the token users received

Registering produced two unrelated tokens: the one on `app_user.verification_token` that gets emailed, and a second UUID that `RegistrationListener` generated for the `verification_token` table. The confirm endpoint only accepted the second, which nobody was ever sent — and it also sent a *duplicate* registration email carrying its dead link.

The listener now reuses the token already assigned at signup and no longer sends its own email. Both columns hold the same value, and both endpoints accept it:

```
verification_token (column) | token (table)               | same
a8561612-d690-4a47-…        | a8561612-d690-4a47-…        | t
```

### Deleted accounts with content were never purged

Every foreign key to `app_user` was `NO ACTION`, so deleting a user who had a single post, comment or like threw a constraint violation. `ScheduledDeletionTask` caught and logged it, leaving the account soft-deleted forever while its owner had been told the data would be gone in 30 days.

Fixed on both sides: `permanentlyDeleteUser` clears dependants in order before the user row, and the Flyway baseline declares `ON DELETE CASCADE`. Verified against a soft-deleted account owning a post that carried another user's comment and like:

```
before: users=5 posts=2 comments=2 likes=2
DELETE /users/admin/users/{id}  {"forceDelete": true}   ->  200 User permanently deleted
after:  users=4 posts=1 comments=1 likes=1
```

### The admin endpoints could not see the accounts they manage

Both resolved users through `getUserById`, which filters `deletedAt IS NULL`. A soft-deleted account was therefore invisible to them: force-delete answered 404, and `extend-deletion` could never reach its "user is not scheduled for deletion" branch, because a scheduled user was never returned in the first place. They now use `getAnyUserById`.

### `schema.sql` never ran and had drifted

`spring.sql.init.mode=never` meant the file was dead reference material, and it had already diverged from the entities. The schema is now owned by [Flyway migrations](../data-model/schema.md) with `JPA_DDL_AUTO=validate`, so Hibernate refuses to start if the entities and tables disagree.

That check earned its keep immediately: it caught `VerificationToken` using `GenerationType.AUTO` — a sequence — where every other entity uses `IDENTITY`.

```
Schema-validation: missing sequence [verification_token_seq]
```

### `create-drop` was the default

Every restart dropped and recreated the schema. The default is now `validate`; `create-drop` remains available via `JPA_DDL_AUTO` for anyone who wants it.

### `getComment` ignored its `postId`

`/posts/999/comments/1` cheerfully returned comment 1 even though it belonged to post 10. The comment is now checked against the path, and a mismatch is a 404. Comments stay readable by any authenticated user — only editing and deleting are author-only.

### 401 and 403 were inverted

Spring Security answered 403 for unauthenticated requests while `UnauthorizedAccessException` — raised when you *are* authenticated but do not own the resource — mapped to 401. A `JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler` now produce the conventional split:

| Situation | Before | Now |
|---|---|---|
| No token | 403 | **401** |
| Expired or malformed token | 403 | **401** |
| Valid token, not the owner | 401 | **403** |
| Valid token, missing role | 403 | 403 |

### Missing resources and bad requests returned 500

The catch-all swallowed anything Spring raised with its own status. Explicit handlers now preserve them:

| Call | Before | Now |
|---|---|---|
| Unknown URL (authenticated) | 500 | **404** |
| Wrong HTTP verb | 500 | **405** |
| Unparseable request body | 500 | **400** |
| `PUT /posts/{id}` on a missing post | 500 | **404** |
| Duplicate email at registration | 500 | **409** |
| Duplicate username at registration | 500 | **409** |

### Admins were silently treated as strangers

`PostService.deletePost` read `getRoles().contains("ADMIN")` — a `Set<Role>` compared against a `String`, which is never true, so the admin arm of the ownership check was dead code. It now compares role names.

### `VerificationTokenService.cleanupExpiredTokens()` was never called

The method existed but carried no `@Scheduled` annotation, so expired verification tokens accumulated forever. It now runs alongside the password-reset sweep.

### Scheduled jobs ran once per replica

Both used plain Spring scheduling with no locking, so two instances meant two concurrent deletion sweeps at 02:00. [ShedLock](../architecture/scheduled-tasks.md) now takes a row lock in the `shedlock` table, created by the baseline migration.

### A stray duplicate of the request templates

`src/main/resources/docs/http-template copy/` was an unmaintained copy still carrying the old, incorrect `/api/...` URLs. Deleted.
