---
sidebar_position: 4
title: Posts
---

# Posts

Base path `/posts`. Every endpoint needs `Authorization: Bearer <token>`.

:::info Posts are owner-scoped
Despite the `privacy` field accepting `public`, reads are restricted to the author. `GET /posts/{id}` returns a post only to the person who created it, whatever its privacy setting. The `privacy` value is only consulted by [`GET /{postId}/likes/count`](./likes.md).
:::

## `POST /posts`

Create a post. The author comes from the token — never from the body.

```json
{
  "title": "My First Post",
  "content": "This is my first post",
  "postType": "text",
  "privacy": "public",
  "metadata": "{}"
}
```

| Field | Required | Rules |
|---|---|---|
| `title` | ✅ | 5–100 characters |
| `content` | ✅ | Non-blank |
| `postType` | — | `text`, `image` or `video` (DB `CHECK` constraint) |
| `privacy` | — | `public` or `private`; defaults to `public` |
| `metadata` | — | Free-form JSON, stored in the `JSONB` column |

| Status | When |
|---|---|
| `201` | Created |
| `400` | Validation failed — e.g. a title under 5 characters |
| `403` | Missing or invalid token |

## `GET /posts/{id}`

Fetch one post.

| Status | When |
|---|---|
| `200` | Returned |
| `403` | Post does not exist **or** is not yours |

:::note 403 is deliberately ambiguous
`PostService.getPost` throws for both "no such post" and "not your post"; the controller catches everything and answers `403`. A caller cannot use this endpoint to discover which post IDs exist. It also means a `403` here does not tell *you* which of the two happened.
:::

## `PUT /posts/{id}`

Replace the mutable fields. Only the author may update.

```json
{
  "title": "Updated title",
  "content": "Updated content",
  "postType": "text",
  "privacy": "private",
  "metadata": "{}"
}
```

Every field is overwritten with what you send — omit one and it becomes `null`. `title` is still bound by 5–100 characters when present.

| Status | When |
|---|---|
| `200` | Updated |
| `401` | You are not the author |
| `500` | No post with that id |

:::warning Missing post gives 500, not 404
`PostService.updatePost` throws a bare `RuntimeException("Post not found")`, which falls through to the catch-all handler. See [Errors](./errors.md).
:::

## `DELETE /posts/{id}`

| Status | When |
|---|---|
| `204` | Deleted |
| `403` | Post does not exist **or** is not yours |

## `GET /posts/my-posts`

Every post by the caller, unpaginated. Fine for small accounts; prefer the paginated variant otherwise.

## Paginated listings

Three endpoints share the same [pagination envelope](./pagination.md) and default to `?page=0&size=10&sort=createdAt,desc`.

| Endpoint | Scope |
|---|---|
| `GET /posts` | All posts |
| `GET /posts/my-posts/paginated` | The caller's posts |
| `GET /posts/user/{userId}/paginated` | One user's posts |

### Filters

Supplying **any** filter switches to the filtered query. All are substring matches except `createdById`.

| Parameter | Applies to |
|---|---|
| `title` | all three |
| `content` | all three |
| `postType` | all three |
| `privacy` | all three |
| `createdById` | `GET /posts` only |

```bash
# Newest 5 public text posts
curl "http://localhost:8080/posts?page=0&size=5&sort=createdAt,desc&privacy=public&postType=text" \
  -H "Authorization: Bearer $JWT"
```

Posts by soft-deleted users are excluded from every query — the repository joins on `createdBy.deletedAt IS NULL`.

## Examples

```bash
# Create
curl -X POST http://localhost:8080/posts \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"title":"My First Post","content":"This is my first post","postType":"text","privacy":"public"}'

# List
curl "http://localhost:8080/posts?page=0&size=10" -H "Authorization: Bearer $JWT"

# Update
curl -X PUT http://localhost:8080/posts/1 \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"title":"Updated title","content":"Updated content","postType":"text","privacy":"private"}'

# Delete
curl -X DELETE http://localhost:8080/posts/1 -H "Authorization: Bearer $JWT"
```

Runnable versions live in [`src/main/resources/docs/http-template/post/`](https://github.com/jomariabejo/connectly-api/tree/main/src/main/resources/docs/http-template/post).
