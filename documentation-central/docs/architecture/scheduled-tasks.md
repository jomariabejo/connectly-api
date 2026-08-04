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

:::info Purging used to fail for any account with content
Every foreign key to `app_user` was `NO ACTION`, so deleting a user who had a single post, comment or like threw a constraint violation. The task caught and logged it, leaving the account soft-deleted forever while its owner had been told the data would be gone in 30 days.

Fixed on both sides: `UserService.permanentlyDeleteUser` clears likes, comments and posts in dependency order before the user row, and the [Flyway baseline](../data-model/schema.md) declares `ON DELETE CASCADE`. See [known issues](../reference/known-issues.md).
:::

## `PasswordResetTokenCleanupTask`

Removes expired password-reset tokens.

| | |
|---|---|
| Schedule | `@Scheduled(fixedRateString = "${security.password.reset.cleanup-interval:3600000}")` — hourly |
| Configured by | `PASSWORD_RESET_CLEANUP_INTERVAL_MS` |
| Calls | `PasswordResetTokenService.deleteExpiredTokens()` and `VerificationTokenService.cleanupExpiredTokens()` |

Housekeeping only — an expired token is already rejected at validation time, so this just stops the table growing.

## What is *not* scheduled

**Rate-limit counters.** `RateLimitingService.clearAttemptTrackers()` carries `@Scheduled(fixedRateString = "${security.password.reset.attempt-cache-clear-interval:3660000}")`, so it does run roughly hourly — but the map is in-memory, so a restart clears it anyway. Note that property has no entry in `.env.example`; it falls back to its inline default.

## Running in more than one instance

Both jobs are wrapped in [ShedLock](https://github.com/lukas-krecan/ShedLock) via `@SchedulerLock`, backed by the `shedlock` table from the baseline migration. Each job takes a row lock, so **one instance runs it and the rest skip** — no more duplicate deletion sweeps at 02:00 when you scale out. On a single instance the lock is simply always acquired.

| Job | Lock name | Held for |
|---|---|---|
| `permanentlyDeleteScheduledUsers` | `permanentlyDeleteScheduledUsers` | 5–30 min |
| `cleanupExpiredTokens` | `cleanupExpiredTokens` | 1–10 min |

Neither job retries on failure — it waits for the next scheduled run.

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
