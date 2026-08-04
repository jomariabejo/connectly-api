---
sidebar_position: 2
title: Further reading
---

# Further reading

The references the implementation was built from, plus the documentation for the libraries in use.

## Articles the design follows

- [Implement JWT authentication in a Spring Boot 3 application](https://medium.com/@tericcabrel/implement-jwt-authentication-in-a-spring-boot-3-application-5839e4fd8fac) — the shape of `JwtService`, `JwtAuthenticationFilter` and `SecurityConfiguration` comes from here
- [Role-Based Access Control with Spring Security](https://medium.com/@bubu.tripathy/role-based-access-control-with-spring-security-ca59d2ce80b0) — the `role` / `user_roles` model
- [Understanding DTOs in Spring Boot](https://medium.com/@roshanfarakate/understanding-dtos-in-spring-boot-a-comprehensive-guide-20e2b8101ee6)
- [Registration — Activate a New Account by Email](https://www.baeldung.com/registration-verify-user-by-email) — the source of the `VerificationToken` + `OnRegistrationCompleteEvent` + `RegistrationListener` pattern
- [What's the difference between JWTs and a Bearer Token?](https://stackoverflow.com/questions/40375508/whats-the-difference-between-jwts-and-a-bearer-token)

## Library documentation

| | |
|---|---|
| [Spring Boot 3.4](https://docs.spring.io/spring-boot/3.4/index.html) | The framework |
| [Spring Security](https://docs.spring.io/spring-security/reference/) | Filter chain, authorization rules |
| [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/) | Repositories, derived queries, `Pageable` |
| [jjwt](https://github.com/jwtk/jjwt) | JWT creation and parsing |
| [MapStruct](https://mapstruct.org/documentation/stable/reference/html/) | Entity ⇄ DTO mapping |
| [springdoc-openapi](https://springdoc.org/) | Swagger UI and the OpenAPI spec |
| [hibernate-types](https://github.com/vladmihalcea/hypersistence-utils) | The `JSONB` mapping on `post.metadata` |
| [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) · [AssertJ](https://assertj.github.io/doc/) | The [test suite](../testing/mockito-suite.md) |

## Tools

| | |
|---|---|
| [Mailpit](https://mailpit.axllent.org/) | Local SMTP catcher — reads verification and reset mail |
| [REST Client for VS Code](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) | Runs the `.http` files in `src/main/resources/docs/http-template/` |
| [Bump.sh](https://bump.sh/) | Publishes `doc/api-documentation.yml` from CI |
| [Docusaurus](https://docusaurus.io/) | This site |

## Topics worth reading before changing things

- [Spring Boot's `PathPatternParser`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/path-matching.html) — why duplicate path variables abort startup ([known issues](./known-issues.md))
- [`@MockitoBean` vs `@MockBean`](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html) — Boot 3.4 deprecated the latter
- [Externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) — how `spring.config.import` reads `.env` ([Configuration](../getting-started/configuration.md))
- [Flyway](https://documentation.red-gate.com/fd) or [Liquibase](https://docs.liquibase.com/) — for replacing the unused `schema.sql`
- [ShedLock](https://github.com/lukas-krecan/ShedLock) — before running [scheduled tasks](../architecture/scheduled-tasks.md) on more than one instance
