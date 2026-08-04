---
sidebar_position: 5
title: Comments
---

# Comments

Comments are nested under their post: `/posts/{postId}/comments`. Every endpoint needs `Authorization: Bearer <token>`.

## `POST /posts/{postId}/comments`

Add a comment. The post comes from the path and the author from the token, so the body carries only the text.

```json
{ "text": "Nice post!" }
```

| Status | When |
|---|---|
| `201` | Created |
| `400` | `text` is blank |
| `404` | No post with that id |
| `403` | Missing or invalid token |

## `GET /posts/{postId}/comments/{commentId}`

| Status | When |
|---|---|
| `200` | Returned |
| `404` | No comment with that id |

:::note Lookup is by comment id alone
`CommentService.getComment` receives `postId` and the authenticated user but uses neither — it resolves the comment purely by `commentId`. A mismatched `postId` still returns the comment, and any authenticated user can read any comment. Reads are not owner-scoped here, unlike [posts](./posts.md).
:::

## `PUT /posts/{postId}/comments/{commentId}`

```json
{ "text": "Edited comment" }
```

| Status | When |
|---|---|
| `200` | Updated |
| `401` | You are not the comment's author |
| `404` | No comment with that id |

## `DELETE /posts/{postId}/comments/{commentId}`

| Status | When |
|---|---|
| `204` | Deleted |
| `401` | You are not the comment's author |
| `404` | No comment with that id |

## `GET /posts/{postId}/comments`

A post's comments in the standard [pagination envelope](./pagination.md). Defaults to `?page=0&size=10&sort=createdAt,desc`.

Filters — either one switches to the filtered query:

| Parameter | Matches |
|---|---|
| `content` | substring of the comment text |
| `createdById` | exact author id |

```bash
curl "http://localhost:8080/posts/1/comments?page=0&size=20&content=nice" \
  -H "Authorization: Bearer $JWT"
```

Comments by soft-deleted users are excluded from every query.

## Response shape

```json
{
  "id": 100,
  "text": "Nice post!",
  "createdAt": "2026-08-04T07:54:32",
  "user": { "id": 1, "username": "someone", "email": "someone@example.com", "…": "…" }
}
```

:::warning The embedded author includes the password hash
`CommentResponseDto.user` is the full `User` entity, so every comment response carries that user's BCrypt hash. See [known issues](../reference/known-issues.md).
:::

## Examples

```bash
# Create
curl -X POST http://localhost:8080/posts/1/comments \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"text":"Nice post!"}'

# List
curl "http://localhost:8080/posts/1/comments?page=0&size=10" -H "Authorization: Bearer $JWT"

# Update
curl -X PUT http://localhost:8080/posts/1/comments/1 \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"text":"Edited comment"}'

# Delete
curl -X DELETE http://localhost:8080/posts/1/comments/1 -H "Authorization: Bearer $JWT"
```

Runnable versions live in [`src/main/resources/docs/http-template/comment/`](https://github.com/jomariabejo/connectly-api/tree/main/src/main/resources/docs/http-template/comment).
