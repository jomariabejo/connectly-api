<!-- Title: use a conventional prefix — feat(scope): …, fix(scope): …, docs: …, chore: …
     Scope is optional but helps: feat(auth):, fix(posts):, docs(api):, chore(deps): -->

<!-- First PR here? documentation-central/ is the full docs site — run it with
     `cd documentation-central && npm start`. Two things to know before you open this:

     1. There is NO CI on this repository. No workflow runs on push or pull request,
        so nothing re-runs your tests for you. Your local green run IS the merge gate.
     2. Merging to `main` redeploys the docs site on Vercel. Application code is not
        auto-deployed anywhere — merge is not release. -->

## Summary

<!-- 2-4 sentences: what this PR does and WHY. Write for the reviewer who lands here in
     6 months with no context. Link the issue/discussion if one exists. -->

## Changes

<!-- Bullet the user-visible or behavioural changes, not the file list — the diff already
     shows files. Group as Features / Fixes / Refactors / Chores when the PR mixes them. -->

-

## Testing

<!-- Check what you actually ran. Nothing here is enforced by CI, so an unchecked box is
     a real gap, not a formality. "I verified X locally by doing Y" is the part only you
     can write — see documentation-central/docs/testing/mockito-suite.md. -->

- [ ] `./gradlew unitTest` — the full Mockito + `@WebMvcTest` suite, no database needed
- [ ] `./gradlew test` — adds `ConnectlyApiApplicationTests`; needs PostgreSQL on :5432
- [ ] `cd documentation-central && npm run build` — required if you touched any `.md`
      or the Docusaurus config. `onBrokenLinks: 'throw'`, so a dead link fails the build.
- [ ] `./gradlew bootRun` and exercised the change against the running API
- [ ] Manual verification — describe what you did and saw:

<!-- Endpoint changes: say which status codes and payloads you actually observed, not
     what you expect. Several bugs in this repo's history were "obviously fine" code
     that behaved differently at runtime. -->

## Screenshots

<!-- For docs-site or Swagger UI changes: before/after screenshots. Delete otherwise. -->

## Database Changes

<!-- Delete if none. The schema is owned by Flyway — see
     documentation-central/docs/data-model/schema.md

     If you add a migration:
     - Next free V number — check src/main/resources/db/migration/ right before merging,
       parallel branches collide on numbers
     - NEVER edit an applied migration. Its checksum is recorded in flyway_schema_history
       and Flyway will refuse to start. Add V<n+1>__your_change.sql instead.
     - `ddl-auto=validate`, so the entities and the migration must agree exactly or the
       application will not boot. Run it once against a fresh database to confirm.
     - Foreign keys to app_user need ON DELETE CASCADE, or permanent account deletion
       breaks (see UserService.permanentlyDeleteUser)
     - Anything queried in a listing must respect soft delete: filter deleted_at IS NULL
       in the repository, or deleted users' data leaks into responses -->

## Breaking Changes

<!-- API contracts, renamed fields, removed endpoints, status-code changes, response-shape
     changes, permission changes. "None." is a valid and useful answer.

     Status codes count: this API deliberately answers 401 for an unknown caller and 403
     for a known-but-forbidden one, and 403 for a missing post. Changing any of those is
     breaking even when it looks like a fix. -->

None.

## Deployment Notes

<!-- Anything a reviewer or deployer has to do by hand. Sharp edges in this repo:

     - Docs site: Vercel builds with Root Directory = documentation-central. Merging to
       main redeploys it. Application code has no deployment pipeline.
     - New config: add it to .env.example AND application.properties as ${VAR:default},
       or a fresh clone stops running with no configuration. Never commit a real .env.
     - JWT_SECRET ships with a public default so a clone runs out of the box. Any deployed
       environment must override it — `openssl rand -hex 32`.
     - doc/api-documentation.yml is a hand-refreshed snapshot; nothing regenerates it.
       If you changed a route, DTO or OpenAPI annotation:
         ./gradlew bootRun &
         curl -s localhost:8080/v3/api-docs.yaml -o doc/api-documentation.yml
     - Scheduled jobs are ShedLock-protected and need the shedlock table (created by V1). -->

None.
