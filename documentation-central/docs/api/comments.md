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
| `401` | Missing or invalid token |

## `GET /posts/{postId}/comments/{commentId}`

| Status | When |
|---|---|
| `200` | Returned |
| `404` | No comment with that id |

:::note Readable by anyone, but scoped to the post
Comments are public within their post: any authenticated user can read one, unlike [posts](./posts.md), which are owner-scoped. Editing and deleting stay author-only.

The `{postId}` **is** checked — a comment belonging to another post is a 404. It used to be accepted and ignored, so `/posts/999/comments/1` returned a comment from post 10.
:::

## `PUT /posts/{postId}/comments/{commentId}`

```json
{ "text": "Edited comment" }
```

| Status | When |
|---|---|
| `200` | Updated |
| `403` | You are not the comment's author |
| `404` | No comment with that id |

## `DELETE /posts/{postId}/comments/{commentId}`

| Status | When |
|---|---|
| `204` | Deleted |
| `403` | You are not the comment's author |
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

:::info The embedded author is a safe projection
`CommentResponseDto.user` used to be the full `User` entity, so every comment listing carried the author's BCrypt hash. It is now the same credential-free shape as [`GET /users/me`](./users.md).
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
