---
name: "AI Assistant: Phase 2 - Resilience & Rate Limiting"
about: Add fault tolerance and rate limiting to AI assistant
title: "[AI Assistant] Phase 2: Resilience & Rate Limiting"
labels: enhancement, ai, backend, security
assignees: ''
---

# Phase 2: AI Assistant Resilience & Rate Limiting

**Parent Issue:** AI Chat Assistant Integration  
**Depends On:** Phase 1 - Foundation Setup

## Objective

Make the AI assistant production-ready by implementing circuit breakers, retry logic, timeouts, and per-user rate limiting to ensure reliability and prevent abuse.

---

## Scope

### In Scope

- Resilience4j integration (circuit breaker, retry, timeout)
- Per-user rate limiting
- Fallback responses when provider is unavailable
- Health indicator for AI provider
- Rate limit exception handling

### Out of Scope

- Metrics and observability (Phase 3)
- Provider abstraction (Phase 4)
- Global rate limiting (consider for future)

---

## Tasks

### 1. Dependency Setup

- [ ] Add Resilience4j dependencies to `build.gradle`

```groovy
// Resilience
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.1.0'
implementation 'io.github.resilience4j:resilience4j-retry:2.1.0'
implementation 'io.github.resilience4j:resilience4j-timelimiter:2.1.0'
```

### 2. Configuration

- [ ] Add resilience properties to `application.properties`

```properties
# Assistant Rate Limiting (per user)
assistant.rate-limit.requests-per-minute=30
assistant.rate-limit.requests-per-hour=500

# Assistant Circuit Breaker
assistant.circuit-breaker.failure-rate-threshold=50
assistant.circuit-breaker.wait-duration-in-open-state-ms=30000
assistant.circuit-breaker.sliding-window-size=10

# Assistant Retry
assistant.retry.max-attempts=3
assistant.retry.backoff-delay-ms=1000
assistant.retry.backoff-multiplier=2

# Assistant Timeout
assistant.timeout-ms=5000
```

- [ ] Create `AssistantResilienceConfiguration.java`

```java
@Configuration
public class AssistantResilienceConfiguration {
    
    @Bean
    public CircuitBreakerConfig assistantCircuitBreakerConfig(
            @Value("${assistant.circuit-breaker.failure-rate-threshold}") int failureRate,
            @Value("${assistant.circuit-breaker.wait-duration-in-open-state-ms}") long waitDuration,
            @Value("${assistant.circuit-breaker.sliding-window-size}") int windowSize) {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate)
            .waitDurationInOpenState(Duration.ofMillis(waitDuration))
            .slidingWindowSize(windowSize)
            .build();
    }
    
    @Bean
    public RetryConfig assistantRetryConfig(
            @Value("${assistant.retry.max-attempts}") int maxAttempts,
            @Value("${assistant.retry.backoff-delay-ms}") long backoffDelay) {
        return RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .waitDuration(Duration.ofMillis(backoffDelay))
            .retryExceptions(AssistantProviderException.class)
            .build();
    }
}
```

### 3. Rate Limiting

- [ ] Create `AssistantRateLimitException.java`

```java
public class AssistantRateLimitException extends RuntimeException {
    private final int retryAfterSeconds;
    
    public AssistantRateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
```

- [ ] Create `AssistantRateLimiter.java`

```java
@Component
public class AssistantRateLimiter {
    private final Map<Long, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig config;
    
    public AssistantRateLimiter(
            @Value("${assistant.rate-limit.requests-per-minute}") int requestsPerMinute) {
        this.config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(requestsPerMinute)
            .timeoutDuration(Duration.ZERO)
            .build();
    }
    
    public void checkRateLimit(User user) {
        RateLimiter limiter = userLimiters.computeIfAbsent(
            user.getId(), 
            id -> RateLimiter.of("user-" + id, config)
        );
        
        if (!limiter.acquirePermission()) {
            throw new AssistantRateLimitException(
                "Too many requests. Please try again later.", 
                60
            );
        }
    }
}
```

- [ ] Add exception handler to `GlobalExceptionHandler.java`

```java
@ExceptionHandler(AssistantRateLimitException.class)
public ResponseEntity<ErrorResponse> handleAssistantRateLimitException(
        AssistantRateLimitException ex) {
    logger.warn("Rate limit exceeded: {}", ex.getMessage());
    
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.TOO_MANY_REQUESTS.value(),
        "Rate limit exceeded",
        ex.getMessage(),
        System.currentTimeMillis()
    );
    
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(errorResponse);
}
```

### 4. Circuit Breaker Implementation

- [ ] Update `AssistantServiceImpl.java` with circuit breaker

```java
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {
    private final ChatClient chatClient;
    private final AuthenticationService authService;
    private final AssistantRateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    
    @Override
    public GenericResponse<AssistantChatResponse> chat(AssistantChatRequest request) {
        User user = authService.getAuthenticatedUser();
        
        // Check rate limit first
        rateLimiter.checkRateLimit(user);
        
        // Execute with circuit breaker and retry
        Supplier<AssistantChatResponse> decoratedSupplier = 
            CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(retry,
                    () -> executeChat(request, user)
                )
            );
        
        try {
            AssistantChatResponse response = decoratedSupplier.get();
            return new GenericResponse<>("Assistant response generated", response);
        } catch (CallNotPermittedException e) {
            // Circuit breaker is open
            return getFallbackResponse();
        }
    }
    
    private GenericResponse<AssistantChatResponse> getFallbackResponse() {
        AssistantChatResponse fallback = new AssistantChatResponse(
            "I'm temporarily unavailable. Please try again in a moment.",
            null,
            null
        );
        return new GenericResponse<>("Assistant temporarily unavailable", fallback);
    }
}
```

### 5. Timeout Configuration

- [ ] Add timeout wrapper to AI provider calls

```java
@Value("${assistant.timeout-ms}")
private long timeoutMs;

private AssistantChatResponse executeChat(AssistantChatRequest request, User user) {
    long startTime = System.currentTimeMillis();
    
    try {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
            chatClient.prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .call()
                .content()
        );
        
        String response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        long latency = System.currentTimeMillis() - startTime;
        
        return new AssistantChatResponse(
            response,
            UUID.randomUUID().toString(),
            new AssistantMetadata(null, "gemini-1.5-flash", latency)
        );
    } catch (TimeoutException e) {
        throw new AssistantProviderException("Request timed out", e);
    } catch (Exception e) {
        throw new AssistantProviderException("Provider error: " + e.getMessage(), e);
    }
}
```

### 6. Health Indicator

- [ ] Create `AssistantHealthIndicator.java`

```java
@Component
public class AssistantHealthIndicator implements HealthIndicator {
    private final CircuitBreaker circuitBreaker;
    
    public AssistantHealthIndicator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    @Override
    public Health health() {
        CircuitBreaker.State state = circuitBreaker.getState();
        
        return switch (state) {
            case CLOSED -> Health.up()
                .withDetail("circuitBreaker", "CLOSED")
                .withDetail("provider", "gemini")
                .build();
            case HALF_OPEN -> Health.status("DEGRADED")
                .withDetail("circuitBreaker", "HALF_OPEN")
                .withDetail("provider", "gemini")
                .build();
            case OPEN -> Health.down()
                .withDetail("circuitBreaker", "OPEN")
                .withDetail("provider", "gemini")
                .build();
            default -> Health.unknown().build();
        };
    }
}
```

### 7. Testing

- [ ] Create `AssistantRateLimiterTest.java`
- [ ] Create `AssistantResilienceTest.java`
- [ ] Test rate limit triggers after threshold
- [ ] Test circuit breaker opens after failures
- [ ] Test retry logic with transient failures
- [ ] Test timeout handling
- [ ] Test fallback response when circuit is open

```java
@Test
void shouldReturnFallback_whenCircuitBreakerOpen() {
    // Simulate failures to open circuit breaker
    for (int i = 0; i < 10; i++) {
        when(chatClient.prompt()).thenThrow(new RuntimeException("API Error"));
        try { service.chat(request); } catch (Exception ignored) {}
    }
    
    // Next call should return fallback
    GenericResponse<AssistantChatResponse> response = service.chat(request);
    
    assertThat(response.getMessage()).contains("temporarily unavailable");
}

@Test
void shouldThrowRateLimitException_whenLimitExceeded() {
    User user = new User();
    user.setId(1L);
    
    // Exhaust rate limit
    for (int i = 0; i < 30; i++) {
        rateLimiter.checkRateLimit(user);
    }
    
    // 31st request should fail
    assertThrows(AssistantRateLimitException.class, 
        () -> rateLimiter.checkRateLimit(user));
}
```

---

## Updated Module Structure

```
src/main/java/com/jomariabejo/connectly_api/assistant_api/
├── config/
│   ├── AssistantConfiguration.java
│   └── AssistantResilienceConfiguration.java   # NEW
├── controller/
│   └── AssistantController.java
├── service/
│   ├── AssistantService.java
│   ├── impl/
│   │   └── AssistantServiceImpl.java           # UPDATED
│   └── AssistantRateLimiter.java               # NEW
├── dto/
│   ├── AssistantChatRequest.java
│   ├── AssistantChatResponse.java
│   └── AssistantMetadata.java
├── exception/
│   ├── AssistantProviderException.java
│   └── AssistantRateLimitException.java        # NEW
└── health/
    └── AssistantHealthIndicator.java           # NEW
```

---

## API Error Responses

**Rate Limit (429):**

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/json

{
  "status": 429,
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "timestamp": 1716019800000
}
```

**Circuit Breaker Open (200 with fallback):**

```json
{
  "message": "Assistant temporarily unavailable",
  "data": {
    "response": "I'm temporarily unavailable. Please try again in a moment.",
    "sessionId": null,
    "metadata": null
  }
}
```

**Timeout (502):**

```json
{
  "status": 502,
  "error": "AI assistant service error",
  "message": "Request timed out",
  "timestamp": 1716019800000
}
```

---

## Health Endpoint

```http
GET /actuator/health

{
  "status": "UP",
  "components": {
    "assistant": {
      "status": "UP",
      "details": {
        "circuitBreaker": "CLOSED",
        "provider": "gemini"
      }
    }
  }
}
```

---

## Acceptance Criteria

- [ ] Rate limiting enforced at 30 requests/minute per user
- [ ] 429 response includes `Retry-After` header
- [ ] Circuit breaker opens after 50% failure rate in sliding window
- [ ] Fallback response returned when circuit is open
- [ ] Requests retry up to 3 times with exponential backoff
- [ ] Requests timeout after 5 seconds
- [ ] Health indicator reflects circuit breaker state
- [ ] All resilience tests passing

---

## Definition of Done

- [ ] All tasks completed
- [ ] Code reviewed and approved
- [ ] Tests passing in CI (including resilience tests)
- [ ] Load tested with simulated failures
- [ ] Health endpoint verified in staging
- [ ] Rate limiting verified with manual testing
