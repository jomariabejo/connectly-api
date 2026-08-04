---
sidebar_position: 3
title: Scheduled tasks
---

# Scheduled tasks

Two background jobs, both enabled by `@EnableScheduling` on `ScheduledDeletionTask`.

## `ScheduledDeletionTask`

Permanently deletes accounts whose 30-day grace period has run out.

| | |
|---|---|
| Schedule | `@Scheduled(cron = "0 0 2 * * *")` — daily at 02:00 server time |
| Calls | `UserService.checkAndDeleteScheduledUsers()` |
| Selects | `scheduled_deletion_at IS NOT NULL AND scheduled_deletion_at <= CURRENT_TIMESTAMP` |
| Then | Hard-deletes each row; `ON DELETE CASCADE` removes the dependent tokens |

Exceptions are caught and logged, so one bad row does not stop the job — but it also will not retry until tomorrow.

```
Starting scheduled deletion task...
Successfully permanently deleted 3 user account(s)
```

The class also carries a commented-out hourly variant for testing.

:::warning Accounts with content are never actually purged
`schema.sql` declares `ON DELETE CASCADE` on `user_roles`, `verification_token` and `password_reset_token` — but `spring.sql.init.mode=never`, so **that file is never executed**. The live schema comes from Hibernate's `ddl-auto`, and every foreign key it generates is `NO ACTION`:

```
 table_name           | column_name | delete_rule
----------------------+-------------+-------------
 comment              | user_id     | NO ACTION
 password_reset_token | user_id     | NO ACTION
 post                 | created_by  | NO ACTION
 post_like            | user_id     | NO ACTION
 user_roles           | user_id     | NO ACTION
 verification_token   | user_id     | NO ACTION
```

So deleting a user who has *any* dependent row fails:

```
ERROR: update or delete on table "app_user" violates foreign key constraint
       "fk37mjvnvpwbqdpewm39q75h9q" on table "comment"
DETAIL: Key (id)=(2) is still referenced from table "comment".
```

The task catches and logs the exception, so an account with a single post, comment or like **stays soft-deleted forever** — invisible to the API but never removed. Only accounts with no content at all are purged. See [known issues](../reference/known-issues.md).
:::

## `PasswordResetTokenCleanupTask`

Removes expired password-reset tokens.

| | |
|---|---|
| Schedule | `@Scheduled(fixedRateString = "${security.password.reset.cleanup-interval:3600000}")` — hourly |
| Configured by | `PASSWORD_RESET_CLEANUP_INTERVAL_MS` |
| Calls | `PasswordResetTokenService.deleteExpiredTokens()` |

Housekeeping only — an expired token is already rejected at validation time, so this just stops the table growing.

## What is *not* scheduled

**Rate-limit counters.** `RateLimitingService.clearAttemptTrackers()` carries `@Scheduled(fixedRateString = "${security.password.reset.attempt-cache-clear-interval:3660000}")`, so it does run roughly hourly — but the map is in-memory, so a restart clears it anyway. Note that property has no entry in `.env.example`; it falls back to its inline default.

**Verification tokens.** `VerificationTokenService.cleanupExpiredTokens()` exists but carries no `@Scheduled` annotation, so nothing calls it. Expired verification tokens accumulate.

## Running in more than one instance

Both jobs use plain Spring scheduling with no locking, so **every replica runs every job**. Two instances means two simultaneous deletion sweeps at 02:00. Before scaling out, add ShedLock or move the work to an external scheduler.

## Testing them by hand

```sql
-- Make an account eligible for permanent deletion on the next run
UPDATE app_user
SET deleted_at = NOW() - INTERVAL '31 days',
    scheduled_deletion_at = NOW() - INTERVAL '1 day',
    is_active = false
WHERE email = 'someone@example.com';
```

Then either wait for 02:00, temporarily narrow the cron expression, or call `userService.checkAndDeleteScheduledUsers()` directly from a test. `GET /users/admin/users/scheduled-deletion` lists exactly what the next run will target.
