---
sidebar_position: 2
title: Postman
---

# Postman

You do not need to build a collection by hand. springdoc publishes an OpenAPI 3 spec at runtime, and Postman converts it into a collection for you — every endpoint, path parameter, query parameter and request body already filled in.

## Import the spec by URL — recommended

Start the API first:

```bash
./gradlew bootRun
```

Then in Postman:

1. Click **Import**
2. Choose **Link**
3. Paste:

   ```
   http://localhost:8080/v3/api-docs
   ```

4. Click **Continue** → **Import**

Postman generates a collection grouped by tag — Authentication, Users, Posts, Comments, Likes.

:::tip Why the URL and not a file
Re-importing the same link picks up whatever the code currently exposes. Add an endpoint, change a DTO, adjust an `@Operation` description, and one re-import brings the collection back in sync. A file copy goes stale the moment someone edits a controller.
:::

### If the API is not running

Import the checked-in copy instead — [`doc/api-documentation.yml`](https://github.com/jomariabejo/connectly-api/blob/main/doc/api-documentation.yml). Choose **Import → File**. It is regenerated whenever the API surface changes, but it is a snapshot; the live URL is always authoritative.

## Authenticate the collection once

Every endpoint outside `/auth/**` needs a bearer token. Set it at the collection level and every request inherits it.

**1. Add a collection variable**

Collection → **Variables** tab:

| Variable | Initial value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `jwt` | *(leave empty)* |

**2. Set collection authorization**

Collection → **Authorization** tab:

- **Auth Type:** `Bearer Token`
- **Token:** `{{jwt}}`

**3. Capture the token automatically**

Open `POST /auth/login` → **Scripts** → **Post-response**, and add:

```javascript
// Store the JWT on the collection so every other request picks it up.
const body = pm.response.json();
if (body.token) {
    pm.collectionVariables.set("jwt", body.token);
    console.log("JWT stored, expires in " + body.expiresIn + "ms");
}
```

Now log in once and the whole collection is authenticated. When requests start coming back `401`, the hour is up — send login again.

:::note Verify the account first
`POST /auth/login` rejects an account that has not used its verification token. Register, open the mail catcher at http://localhost:8025, follow the link, *then* log in. See [Authentication](./authentication.md).
:::

## A first run in Postman

| Order | Request | Note |
|---|---|---|
| 1 | `POST /auth/registration` | Password needs 8+ chars, an uppercase letter, a digit and a special character |
| 2 | `GET /auth/verify?token=` | Token from the email at http://localhost:8025 |
| 3 | `POST /auth/login` | The post-response script stores `{{jwt}}` |
| 4 | `GET /users/me` | Should answer `200` with your profile |
| 5 | `POST /posts` | `201` |
| 6 | `POST /posts/{postId}/comments` | `201` |
| 7 | `POST /{postId}/likes/toggle` | `data: true`, send again for `data: false` |

## Environments for more than one target

Rather than editing `baseUrl` by hand, make an environment per target and switch with the top-right selector:

| Environment | `baseUrl` |
|---|---|
| Local | `http://localhost:8080` |
| Staging | `https://staging.example.com` |

Keep `jwt` as an **environment** variable too, so a staging token never leaks into local requests.

## Alternatives

**Newman** — run the collection from CI:

```bash
npm install -g newman
newman run collection.json --env-var "baseUrl=http://localhost:8080"
```

**VS Code REST Client** — the repo already ships runnable requests in [`src/main/resources/docs/http-template/`](https://github.com/jomariabejo/connectly-api/tree/main/src/main/resources/docs/http-template). Open a file, swap `TOKEN_HERE` for a real token, click **Send Request**. No import step at all.

**Swagger UI** — for a quick call without leaving the browser, [`/swagger-ui.html`](./swagger.md) has an **Authorize** button and a **Try it out** on every operation.

## Troubleshooting

**Import fails or the collection is empty**
Check the spec is actually being served: `curl http://localhost:8080/v3/api-docs` should return JSON. If it 404s, `SWAGGER_ENABLED` is `false` — see [Configuration](../getting-started/configuration.md).

**Everything returns 401**
`{{jwt}}` is empty or expired. Re-send `POST /auth/login`. Confirm the collection's **Authorization** tab is `Bearer Token` with `{{jwt}}`, and that the individual request is set to **Inherit auth from parent**.

**A request returns 403**
The token is valid but you do not own that resource, or you lack the role. `403` on the admin endpoints means the account has no `ADMIN` role — see [Users](./users.md).

**Postman cannot reach `localhost`**
The Postman **web** client cannot see your machine. Use the desktop app, or install the Postman Desktop Agent.
