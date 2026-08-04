---
sidebar_position: 7
title: Likes
---

# Likes

Base path `/{postId}`. Every endpoint needs `Authorization: Bearer <token>`.

:::note An unusual base path
`PostLikeController` maps `/{postId}` at the class level, so these routes sit at the root rather than under `/posts`. That is the shape the API ships; it is not a typo in this page.
:::

## `POST /{postId}/likes/toggle`

One endpoint for both liking and unliking. There is no `DELETE` variant.

**Response** — `200`

```json
{ "message": "Post liked successfully.",   "data": true  }
{ "message": "Post unliked successfully.", "data": false }
```

`data` is the **resulting** state: `true` means the post is now liked, `false` means the like was removed. Call it twice and you are back where you started.

| Status | When |
|---|---|
| `200` | Toggled |
| `404` | No post with that id |
| `401` | Missing or invalid token |

```bash
$ curl -X POST http://localhost:8080/1/likes/toggle -H "Authorization: Bearer $JWT"
{"message":"Post liked successfully.","data":true}

$ curl -X POST http://localhost:8080/1/likes/toggle -H "Authorization: Bearer $JWT"
{"message":"Post unliked successfully.","data":false}
```

:::info This route was recently corrected
It used to declare `@PostMapping("/{postId}/likes/toggle")` under the class-level `@RequestMapping("/{postId}")`, which concatenated into `/{postId}/{postId}/likes/toggle`. Spring Boot 3's `PathPatternParser` refuses to capture the same variable twice, so that mapping **aborted application startup** — the endpoint never served a request. It now matches its sibling routes. A regression test pins the shape.
:::

## `GET /{postId}/likes/count`

The number of likes on a post.

```json
{ "message": "Total likes retrieved.", "data": 3 }
```

| Status | When |
|---|---|
| `200` | Counted |
| `404` | Post does not exist **or** is not `public` |

:::note Private posts report 404, not 0
`PostLikeService.countLikesByPost` only counts when `privacy` equals `public`; anything else raises `PostNotFoundException`. This is the one place the `privacy` field changes behaviour — [reads elsewhere are owner-scoped regardless](./posts.md).
:::

## `GET /{postId}/likes/my-likes`

Every like the caller has made, across all posts.

```json
{
  "message": "User likes retrieved successfully.",
  "data": [
    { "id": 77, "postId": 10, "createdAt": "2026-08-04T07:54:32" }
  ]
}
```

:::note `{postId}` is ignored here
The segment is inherited from the class-level mapping and never read. `/1/likes/my-likes` and `/999/likes/my-likes` return the same list. Pass any value.
:::

## Uniqueness

`post_like` carries `UNIQUE (user_id, post_id)`, so a user can like a post at most once. The toggle endpoint respects that: it looks for an existing row and deletes it rather than inserting a duplicate.

Runnable examples live in [`src/main/resources/docs/http-template/post/`](https://github.com/jomariabejo/connectly-api/tree/main/src/main/resources/docs/http-template/post) — `likePost.http` and `unlikePost.http`.
