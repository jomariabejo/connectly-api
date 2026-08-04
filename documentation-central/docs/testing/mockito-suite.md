---
sidebar_position: 1
title: Mockito test suite
---

# The test suite

143 tests across 12 classes. **None of them need a database, a mail server, or a running application.**

```bash
./gradlew unitTest
```

## Two Gradle tasks

| Task | Runs | Needs PostgreSQL? |
|---|---|---|
| `./gradlew unitTest` | Every Mockito unit test and controller slice | ❌ No |
| `./gradlew test` | The above **plus** `ConnectlyApiApplicationTests` | ✅ Yes |

`ConnectlyApiApplicationTests.contextLoads()` is a `@SpringBootTest`, so it starts the whole application and connects to `localhost:5432`. `unitTest` excludes it by name:

```groovy
tasks.register('unitTest', Test) {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    filter {
        excludeTestsMatching 'com.jomariabejo.connectly_api.ConnectlyApiApplicationTests'
    }
}
```

Use `unitTest` in CI and while developing; use `test` when you specifically want to prove the context still starts.

Coverage reports come from JaCoCo — `./gradlew test jacocoTestReport`, output in `build/reports/jacoco/test/html/`.

## What is covered

### Service unit tests — 84 tests

Pure Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock` for collaborators, `@InjectMocks` for the subject. No Spring context.

| Class | Focus |
|---|---|
| `PostServiceTest` | Author assignment, ownership on read/update/delete, pagination envelope, filter forwarding |
| `CommentServiceTest` | Post/author linking, `UnauthorizedAccessException` for non-authors, `getComment` scoping to its `{postId}` |
| `UserServiceTest` | The soft-delete lifecycle — 30-day scheduling, reactivation, grace-period boundaries, the scheduled purge |
| `AuthenticationServiceTest` | Signup hashing and disabled state, login, verification, both reset flows, rate limiting, password strength |
| `PostLikeServiceTest` | Toggle on/off, idempotent create, private posts reporting not-found |
| `JwtServiceTest` | Token round-trip, extra claims, expiry, wrong user, foreign signature |
| `RateLimitingServiceTest` | Threshold behaviour, per-address and per-action isolation, window expiry |

### Controller slice tests — 59 tests

`@WebMvcTest` with `MockMvc`: real routing, real JSON serialization, real validation — mocked services.

| Class | Focus |
|---|---|
| `AuthenticationControllerTest` | Registration, `@Valid` rejection, login payload, verification, both reset flows, the 429 path |
| `PostControllerTest` | CRUD status codes, `@PageableDefault` binding, filter switching, the deliberate 403s |
| `CommentControllerTest` | Nested routes, 201/204, author-only edits, pagination defaults |
| `UserControllerTest` | Profile, the **202** on `DELETE /users/me`, reactivation, admin endpoints |
| `PostLikeControllerTest` | Toggle semantics, like counting, and a regression guard on the route shape |

## Conventions worth knowing

### `@MockitoBean`, not `@MockBean`

Spring Boot 3.4 deprecated `@MockBean` in favour of `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`. This project is on 3.4.4, so all slice tests use the new annotation.

### `@Value` fields need `ReflectionTestUtils`

`AuthenticationService`, `JwtService` and `RateLimitingService` read configuration through `@Value` fields. Plain Mockito performs **no property injection** — without seeding them, ints are `0` and Strings are `null`, and the tests fail for reasons unrelated to what they assert:

```java
@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(authenticationService, "passwordMinLength", 8);
    ReflectionTestUtils.setField(authenticationService, "passwordResetRedirectUrl", RESET_URL);
}
```

### The shared `@ControllerSliceTest` annotation

Every slice needs the same two things, so they are bundled:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
public @interface ControllerSliceTest {
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
```

Used as `@ControllerSliceTest(PostController.class)`.

`addFilters = false` bypasses the JWT chain — these tests assert the HTTP contract, not the security wiring. Excluding `JwtAuthenticationFilter` is the non-obvious part: `@WebMvcTest` instantiates `Filter` components *even when filters are disabled*, and that one needs a `JwtService` and a `UserDetailsService` a slice does not provide, so the context fails to start before a single request runs.

:::note What the slices do not prove
Because the filter chain is off, `@PreAuthorize` and the path rules in `SecurityConfiguration` are **not** enforced in these tests. `@WithMockUser(roles = "ADMIN")` documents the intended caller; it does not prove the rule holds. Verify role enforcement against a running server.
:::

### Events, not a mocked publisher

`AuthenticationController` is handed the `ApplicationContext` for its `ApplicationEventPublisher`, so a `@MockitoBean` of that type is never the instance it calls. The registration test uses Spring's own recorder instead:

```java
@ControllerSliceTest(AuthenticationController.class)
@RecordApplicationEvents
class AuthenticationControllerTest {

    @Autowired
    private ApplicationEvents applicationEvents;

    // …
    assertThat(applicationEvents.stream(OnRegistrationCompleteEvent.class))
            .singleElement()
            .satisfies(e -> assertThat(e.getUser().getEmail()).isEqualTo("someone@example.com"));
}
```

### Date assertions tolerate DST

`Calendar.add(DAY_OF_MONTH, 30)` is calendar arithmetic, so a daylight-saving transition inside the window shifts the result by an hour and a naive `toDays()` comparison flakes twice a year. The grace-period assertions use ranges:

```java
assertThat(TimeUnit.MILLISECONDS.toDays(graceMillis)).isBetween(29L, 30L);
```

## Things the tests caught

Writing this suite surfaced defects the code had been hiding:

1. **The application could not start.** `PostLikeController` mapped `/{postId}/{postId}/likes/toggle`; Spring Boot 3's `PathPatternParser` refuses to capture the same variable twice and aborted the context, so no endpoint served any request. `PostLikeControllerTest` now fails at context load if a duplicate segment returns.
2. **Validation errors returned 500.** The catch-all `@ExceptionHandler(Exception.class)` intercepted `MethodArgumentNotValidException` before Spring could map it to 400 — every malformed body in the API came back as a server error.
3. **Unmatched URLs, wrong verbs and unparseable bodies all returned 500** for the same reason.
4. **Admins were silently treated as strangers.** `PostService.deletePost` compared a `Set<Role>` against the string `"ADMIN"` — never true, so the admin arm of the ownership check was dead code.

All are fixed; see [known issues](../reference/known-issues.md) for the full list and the evidence.

## Adding a test

**Service logic** → a new `*ServiceTest` in `src/test/java/…/service/`. Mock the repositories; assert both the happy path and each `throw`.

**HTTP behaviour** → a new `*ControllerTest` in `src/test/java/…/controller/`. Use `@ControllerSliceTest(YourController.class)`, `@MockitoBean` the services, and assert status codes and `jsonPath` on the body.

If the class under test reads `@Value` fields, seed them with `ReflectionTestUtils` in `@BeforeEach`.
