---
sidebar_position: 7
title: Pagination
---

# Pagination & filtering

Every list endpoint that paginates uses the same query parameters and returns the same envelope.

## Query parameters

Standard Spring Data paging.

| Parameter | Meaning |
|---|---|
| `page` | Zero-based page index |
| `size` | Items per page |
| `sort` | `field,direction` — e.g. `createdAt,desc`. Repeat for multiple keys |

```bash
curl "http://localhost:8080/posts?page=1&size=5&sort=title,asc" -H "Authorization: Bearer $JWT"
```

### Defaults per endpoint

| Endpoint | Default |
|---|---|
| `GET /posts` | `page=0&size=10&sort=createdAt,desc` |
| `GET /posts/my-posts/paginated` | `page=0&size=10&sort=createdAt,desc` |
| `GET /posts/user/{userId}/paginated` | `page=0&size=10&sort=createdAt,desc` |
| `GET /posts/{postId}/comments` | `page=0&size=10&sort=createdAt,desc` |
| `GET /users/paginated` | `page=0&size=10&sort=id,asc` |

Sort on any persistent field of the underlying entity — `sort` maps to a JPQL property name, so `createdAt` works and `created_at` does not.

## The response envelope

Every paginated endpoint returns a `PaginationDto`:

```json
{
  "content": [ … ],
  "pageNumber": 1,
  "pageSize": 5,
  "totalElements": 12,
  "totalPages": 3,
  "hasNext": true,
  "hasPrevious": true
}
```

| Field | Meaning |
|---|---|
| `content` | The items on this page |
| `pageNumber` | Zero-based index of this page |
| `pageSize` | Requested page size |
| `totalElements` | Total matching rows across all pages |
| `totalPages` | Total number of pages |
| `hasNext` | `pageNumber < totalPages - 1` |
| `hasPrevious` | `pageNumber > 0` |

`hasNext` and `hasPrevious` are derived, not stored — you never have to compute them yourself.

:::note Not Spring's `Page` JSON
This is a hand-rolled envelope, so it has no `first`, `last`, `numberOfElements`, `empty`, `pageable` or `sort` object. Do not expect Spring Data's default serialization.
:::

## Filtering

Filters live alongside the paging parameters. **Supplying any one of them switches the endpoint to its filtered query** — otherwise the plain paginated query runs.

| Endpoint | Filters |
|---|---|
| `GET /posts` | `title`, `content`, `postType`, `privacy`, `createdById` |
| `GET /posts/my-posts/paginated` | `title`, `content`, `postType`, `privacy` |
| `GET /posts/user/{userId}/paginated` | `title`, `content`, `postType`, `privacy` |
| `GET /posts/{postId}/comments` | `content`, `createdById` |
| `GET /users/paginated` | `username`, `email`, `firstName`, `lastName` |

Text filters are **substring** matches (`LIKE %value%`), case-sensitive. Id filters are exact.

```bash
# Public text posts whose title contains "First", newest first, 5 per page
curl "http://localhost:8080/posts?size=5&sort=createdAt,desc&title=First&privacy=public&postType=text" \
  -H "Authorization: Bearer $JWT"
```

Filters combine with `AND`. A `null` (omitted) filter is skipped by the query rather than matched against `null`.

## Soft-deleted users are always excluded

Every paginated and filtered query joins on `deletedAt IS NULL`. Posts and comments authored by an account inside its [deletion grace period](./users.md) disappear from all listings, and reappear if the account is reactivated. `totalElements` reflects that too.

## Walking every page

```bash
page=0
while :; do
  body=$(curl -s "http://localhost:8080/posts?page=$page&size=50" -H "Authorization: Bearer $JWT")
  echo "$body" | jq -c '.content[]'
  [ "$(echo "$body" | jq -r .hasNext)" = "true" ] || break
  page=$((page + 1))
done
```
