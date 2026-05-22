# Auth CRUD Guide

This guide shows the auth-related flows as copy-paste `curl` commands.

## Base URLs

- Stage: `http://localhost:8081/api`
- Demo: `http://localhost:8082/api`
- Prod: `http://localhost:8083/api`

All auth routes live under `/api/v1/auth`.

## Endpoints

| Action | Method | Path | Purpose |
|---|---|---|---|
| Create | `POST` | `/v1/auth/registration` | Register a new user |
| Create | `POST` | `/v1/auth/login` | Log in and get a JWT |
| Create | `POST` | `/v1/auth/register/customer` | Register a customer account |
| Create | `POST` | `/v1/auth/register/invite` | Register from an invite |
| Read | `GET` | `/v1/auth/verify?token=...` | Verify email with a token |
| Read | `GET` | `/v1/auth/registrationConfirm?token=...` | Confirm registration with a token |
| Read | `GET` | `/v1/auth/invites/{token}` | Preview an invite |
| Update | `POST` | `/v1/auth/verify/otp` | Verify email using OTP |
| Update | `POST` | `/v1/auth/verify/resend` | Resend verification email |
| Update | `POST` | `/v1/auth/forgot-password/email` | Start password reset by email |
| Update | `POST` | `/v1/auth/forgot-password/otp` | Start password reset by OTP |
| Update | `POST` | `/v1/auth/reset-password` | Reset password with token or OTP |

There is no auth `DELETE` endpoint in the current API.

## Create

### Register a user

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/registration \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!","username":"connectly_user","firstName":"Jane","lastName":"Doe"}'
```

Use a unique email and username if the sample values already exist.

### Log in

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'
```

The response includes a JWT token plus tenant and redirect data.

### Register a customer account

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/register/customer \
  -H "Content-Type: application/json" \
  -d '{"tenantSlug":"acme","user":{"email":"customer@example.com","password":"Password123!","username":"customer_acme","firstName":"Jane","lastName":"Doe"}}'
```

### Register from an invite

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/register/invite \
  -H "Content-Type: application/json" \
  -d '{"inviteToken":"PASTE_INVITE_TOKEN_HERE","user":{"email":"invitee@example.com","password":"Password123!","username":"invitee_1","firstName":"Jane","lastName":"Doe"}}'
```

## Read

### Verify email with a token

```bash
curl -i -X GET "http://localhost:8082/api/v1/auth/verify?token=PASTE_VERIFICATION_TOKEN_HERE"
```

### Confirm registration with a token

```bash
curl -i -X GET "http://localhost:8082/api/v1/auth/registrationConfirm?token=PASTE_VERIFICATION_TOKEN_HERE"
```

### Preview an invite

```bash
curl -i -X GET "http://localhost:8082/api/v1/auth/invites/PASTE_INVITE_TOKEN_HERE"
```

## Update

### Verify email using OTP

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/verify/otp \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","otp":"123456"}'
```

### Resend verification email

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/verify/resend \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Start password reset by email link

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/forgot-password/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Start password reset by OTP

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/forgot-password/otp \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Reset password with an email token

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"PASTE_RESET_TOKEN_HERE","otp":null,"newPassword":"NewSecurePassword123!"}'
```

### Reset password with an OTP

```bash
curl -i -X POST http://localhost:8082/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":null,"otp":"123456","newPassword":"NewSecurePassword123!"}'
```

## Use the JWT

After login, copy the token from the response and use it like this:

```bash
TOKEN="PASTE_JWT_HERE"

curl -i http://localhost:8082/api/v1/user/me \
  -H "Authorization: Bearer $TOKEN"
```

## Mailpit

If you need to inspect verification or reset messages locally:

```bash
curl -s http://localhost:8025/api/v1/messages | jq '.messages[] | {subject, To: .To, ID: .ID}'
```
