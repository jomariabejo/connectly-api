# Postman Setup Guide

Use the bundled collection to exercise the API without hand-writing requests.

## What is included

- Collection file: [`connectly-api-v1.postman_collection.json`](connectly-api-v1.postman_collection.json)
- Auth flow variables for registration, login, password reset, and verification tokens
- Base URL presets for local bootRun, stage, demo, and prod

## Import the collection

1. Open Postman.
2. Click **Import**.
3. Select [`connectly-api-v1.postman_collection.json`](connectly-api-v1.postman_collection.json).
4. Import it as a collection.

If you use Postman web, you can also upload the same JSON file directly from the workspace.

## Set the base URL

The collection uses `{{baseUrl}}/v1/...` for every request.

Choose the value that matches the environment you want to test:

- Local bootRun: `http://localhost:8080/api`
- Stage: `http://localhost:8081/api`
- Demo: `http://localhost:8082/api`
- Prod: `http://localhost:8083/api`

You can either:

- Set `baseUrl` directly to one of the values above, or
- Copy the matching preset variable into `baseUrl`:
  - `baseUrlStage`
  - `baseUrlDemo`
  - `baseUrlProd`

## Recommended startup order

Run the requests in this order for a clean auth flow:

1. `User Account Flow -> Register User`
2. Copy the verification token from Mailpit.
3. `User Account Flow -> Verify User Email`
4. `User Account Flow -> Login User`
5. Use the returned `userToken` for authenticated requests.

## Auth variables

The collection already defines the main auth variables:

| Variable | Purpose |
|---|---|
| `userEmail` | Email used for registration and login |
| `userPassword` | Password used for login |
| `username` | Username used for registration |
| `verificationToken` | Token copied from the verification email |
| `resetToken` | Token copied from the reset email |
| `resetOtp` | OTP used for password reset |
| `userToken` | JWT returned from login |

## Common auth flow

### Register

Send `User Account Flow -> Register User` first. The response should contain the created user id when registration succeeds.

### Verify email

Open Mailpit, copy the verification token from the email link, then run `Verify Email with Token` (`GET {{baseUrl}}/v1/auth/verify?token=...`).

### Log in

Run `Login User` after verification. The test script stores the JWT in `userToken`.

### Use bearer auth

Requests under `Users`, `Posts`, `Orders`, and other protected sections reuse `{{userToken}}` automatically after login.

## Mailpit

For local email inspection, open:

```text
http://localhost:8025
```

You can also query the API directly:

```bash
curl -s http://localhost:8025/api/v1/messages | jq '.messages[] | {subject, To: .To, ID: .ID}'
```

## Troubleshooting

- If requests fail with `ECONNREFUSED`, confirm the API is running on the port that matches `baseUrl`.
- If login returns `401`, verify the user first and then retry login.
- If email-related requests never arrive, check that Mailpit is running and that `spring.mail.host` points to `localhost` or `mailpit` depending on the environment.
