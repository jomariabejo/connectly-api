# User API Migration

This refactor moves the user API to REST-oriented routes and centralizes account deletion behavior in `UserService`.

## Changed Endpoints

- `GET /v1/users/` and `GET /v1/users/paginated` are replaced by `GET /v1/users` with normal Spring `page`, `size`, `sort`, `username`, `email`, `firstName`, and `lastName` query parameters.
- `GET /v1/users/settings` is now `GET /v1/users/me/settings`.
- `PUT /v1/users/settings/account-privacy`, `PUT /v1/users/settings/auto-approve`, and `PUT /v1/users/settings/allow-following` are replaced by `PATCH /v1/users/me/settings`.
- `DELETE /v1/users/admin/users/{id}` is now `DELETE /v1/admin/users/{id}`.
- `PUT /v1/users/admin/users/{id}/extend-deletion` is now `PATCH /v1/admin/users/{id}/deletion-grace-period`.
- `GET /v1/users/admin/users/scheduled-deletion` is now `GET /v1/admin/users/scheduled-for-deletion`.

## Account Reactivation

Deleted accounts can no longer be reactivated by logging in, even when `autoReactivationEnabled` is true. Reactivation now always requires the emailed reactivation token through `POST /v1/users/reactivate`, which returns `204 No Content` on success.
