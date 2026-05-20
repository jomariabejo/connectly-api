---
name: "Feature: AI Chat Assistant Integration"
about: Implement a production-ready AI chat assistant for Connectly social platform
title: "[Feature] AI Chat Assistant Integration"
labels: enhancement, feature, ai
assignees: ''
---

# AI Chat Assistant Integration for Connectly

## Executive Summary

Implement a production-ready, provider-agnostic AI chat assistant in the Connectly Spring Boot application using Spring AI framework with Google Gemini API as the initial provider. This module enables users to get help with social features, privacy settings, account management, and commerce questions through natural language interaction.

---

## Business Context & Objectives

### Primary Goals

- **User Experience**: Enable natural language interaction for users to get help with Connectly features (posts, privacy, circles, orders)
- **Scalability**: Design for high concurrent usage aligned with existing platform capacity
- **Flexibility**: Provider-agnostic architecture supporting future AI model migrations
- **Reliability**: Implement circuit breakers, fallbacks, and graceful degradation
- **Consistency**: Follow existing Connectly patterns for security, DTOs, and exception handling

### Success Metrics

- API response time < 3 seconds (p95)
- 99.5% availability
- Support for 100+ concurrent users
- Zero API key exposures in logs/monitoring

### Connectly-Specific Use Cases

**Social Assistance:**
- "How do I make my post visible only to my Close Friends circle?"
- "Why can't I see this user's posts?" (privacy/blocking explanations)
- "How do I approve pending follow requests?"
- "What's the difference between FOLLOWERS_ONLY and MUTUALS_ONLY?"

**Account Support:**
- "How do I delete my account?" (soft-delete with 30-day grace period)
- "I changed my mind about deleting, how do I reactivate?"
- "How do I enable auto-approve for followers?"

**Commerce Help:**
- "What's the status of my order?"
- "Why did my payment fail?"
- "How do I check my order history?"

**Content Guidance:**
- "Help me write a post about..." (draft assistance)
- "What are the character limits for posts?"

---

## Technical Architecture

### Technology Stack

| Layer | Technology | Notes |
|-------|------------|-------|
| **AI Framework** | Spring AI | Latest stable |
| **Primary AI Provider** | Google Gemini API | gemini-1.5-flash (free tier) |
| **Backend Framework** | Spring Boot | 3.4.4 (existing) |
| **Build Tool** | Gradle | Groovy DSL (existing) |
| **API Protocol** | REST (JSON) | Future: WebSocket for streaming |
| **Configuration** | Spring Boot Properties | `.properties` format (existing pattern) |
| **Validation** | Spring Validation (JSR-380) | Existing pattern |
| **Security** | Spring Security + JWT | Existing JJWT implementation |
| **Resilience** | Resilience4j | Circuit breaker, retry, rate limiting |
| **Observability** | Micrometer + Prometheus + Datadog | Existing setup |
| **Testing** | JUnit 5, Mockito, WireMock | Existing pattern |

---

## Module Structure

Follow existing feature-module pattern (`orders_api/`, `payments_api/`):

```
src/main/java/com/jomariabejo/connectly_api/assistant_api/
├── config/
│   └── AssistantConfiguration.java       # Spring AI client beans
│
├── controller/
│   └── AssistantController.java          # REST endpoint
│
├── service/
│   ├── AssistantService.java             # Business logic interface
│   └── impl/
│       └── AssistantServiceImpl.java     # Service implementation
│
├── dto/
│   ├── AssistantChatRequest.java         # Incoming chat message
│   ├── AssistantChatResponse.java        # AI response wrapper
│   └── AssistantMetadata.java            # Response metadata
│
├── provider/
│   ├── AiProvider.java                   # Provider interface
│   ├── gemini/
│   │   └── GeminiProvider.java           # Gemini implementation
│   └── factory/
│       └── AiProviderFactory.java        # Provider selection
│
├── exception/
│   ├── AssistantProviderException.java   # Provider failures
│   └── AssistantRateLimitException.java  # Too many requests
│
└── metrics/
    └── AssistantMetricsRecorder.java     # Custom metrics

src/main/resources/
├── application.properties                 # Add assistant.* properties
└── prompts/
    └── system-prompt.txt                  # Connectly-specific system instructions
```

---

## Implementation Tasks

### Phase 1: Foundation Setup

**Goal**: Basic working AI chat endpoint

#### 1.1 Dependencies

Add to `build.gradle`:

```groovy
// AI Assistant
implementation platform('org.springframework.ai:spring-ai-bom:1.0.0-M3')
implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter'

// Resilience (rate limiting, circuit breaker)
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.1.0'

// Testing
testImplementation 'com.github.tomakehurst:wiremock-jre8:2.35.0'
```

Add repository for Spring AI milestones:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}
```

#### 1.2 Configuration Properties

Add to `application.properties`:

```properties
# AI Assistant Configuration
assistant.provider=gemini
assistant.gemini.api-key=${GEMINI_API_KEY:}
assistant.gemini.model=gemini-1.5-flash
assistant.gemini.temperature=0.7
assistant.gemini.max-tokens=1024
assistant.gemini.timeout-ms=5000

# Assistant Rate Limiting
assistant.rate-limit.requests-per-minute=30
assistant.rate-limit.requests-per-hour=500

# Assistant Circuit Breaker
assistant.circuit-breaker.failure-rate-threshold=50
assistant.circuit-breaker.wait-duration-in-open-state-ms=30000

# Assistant Retry
assistant.retry.max-attempts=3
assistant.retry.backoff-delay-ms=1000
```

#### 1.3 API Path Registration

Extend `ApiPaths.java`:

```java
public static final String V1_ASSISTANT = V1 + "/assistant";
```

#### 1.4 Core Implementation Tasks

- [ ] Create `AssistantConfiguration` with Gemini client bean
- [ ] Implement `AssistantChatRequest` DTO with validation
- [ ] Implement `AssistantChatResponse` DTO
- [ ] Implement `AssistantService` interface and `AssistantServiceImpl`
- [ ] Create `AssistantController` with `POST /v1/assistant/chat`
- [ ] Add exception handlers to `GlobalExceptionHandler`
- [ ] Create system prompt file for Connectly context
- [ ] Write unit tests for service layer

---

### Phase 2: Resilience & Rate Limiting

**Goal**: Production-ready fault tolerance

#### 2.1 Tasks

- [ ] Configure Resilience4j circuit breaker for Gemini calls
- [ ] Implement retry logic with exponential backoff
- [ ] Add timeout configuration (5s default)
- [ ] Create fallback response mechanism
- [ ] Implement rate limiting per user (30 req/min)
- [ ] Add health indicator for AI provider status

---

### Phase 3: Observability

**Goal**: Complete visibility into AI operations

#### 3.1 Logging

- [ ] Log request metadata (userId, timestamp) - never log full prompts
- [ ] Log response metadata (tokens, latency)
- [ ] Add correlation IDs for request tracing
- [ ] Never log API keys or full AI responses

#### 3.2 Metrics

- [ ] Track request count by status (success/failure)
- [ ] Measure response latency (histogram)
- [ ] Monitor token usage
- [ ] Track circuit breaker state
- [ ] Monitor rate limit hits

```java
@Component
public class AssistantMetricsRecorder {
    private final MeterRegistry registry;
    
    public void recordRequest(long latencyMs, boolean success) {
        registry.counter("assistant.requests.total", 
            "status", success ? "success" : "failure"
        ).increment();
        
        registry.timer("assistant.request.latency")
            .record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
```

---

### Phase 4: Provider Abstraction

**Goal**: Multi-provider support foundation

- [ ] Create `AiProvider` interface with common methods
- [ ] Implement `GeminiProvider` with Spring AI
- [ ] Create `AiProviderFactory` for provider selection
- [ ] Externalize provider selection to config
- [ ] Implement provider health checks

```java
public interface AiProvider {
    AssistantChatResponse chat(AssistantChatRequest request, User user);
    boolean isHealthy();
    String getProviderName();
}
```

---

### Phase 5: Future Enhancements (Out of Scope)

- [ ] Conversation memory with session persistence
- [ ] Streaming responses via WebSocket/SSE
- [ ] RAG integration for Connectly documentation
- [ ] Tool calling for order status lookups

---

## REST API Specification

### Endpoint: Chat with Assistant

**Request:**

```http
POST /api/v1/assistant/chat
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "message": "How do I make my post visible only to Close Friends?",
  "sessionId": "optional-session-uuid",
  "context": {
    "currentPage": "post-editor"
  }
}
```

**Success Response (200):**

```json
{
  "message": "Assistant response generated",
  "data": {
    "response": "To make your post visible only to Close Friends, select 'CIRCLES' as your privacy setting when creating the post, then choose your 'Close Friends' circle...",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "metadata": {
      "tokensUsed": 156,
      "model": "gemini-1.5-flash",
      "latencyMs": 1243
    }
  }
}
```

**Error Response (429 - Rate Limit):**

```json
{
  "status": 429,
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again in 60 seconds.",
  "timestamp": 1716019800000
}
```

**Error Response (502 - Provider Error):**

```json
{
  "status": 502,
  "error": "AI assistant service error",
  "message": "The AI service is temporarily unavailable. Please try again.",
  "timestamp": 1716019800000
}
```

### Validation Rules

- `message`: Required, 1-4000 characters
- `sessionId`: Optional, UUID v4 format
- Request body max size: 10KB

---

## Security Considerations

### Authentication

- Endpoint inherits authentication from existing security config (`/v1/**` → authenticated)
- Use `AuthenticationService.getAuthenticatedUser()` for user context
- No additional security config changes needed

### Input Validation

- Validate all inputs with Bean Validation (`@Valid`, `@NotBlank`, `@Size`)
- Implement basic prompt injection detection (regex patterns)
- Limit message length to prevent token exhaustion
- Sanitize HTML/script tags from user input

### API Key Management

- Store `GEMINI_API_KEY` in environment variables only
- Never log API keys
- Use `${GEMINI_API_KEY:}` pattern matching existing payment keys

### Rate Limiting

- Per-user limits: 30 requests/minute, 500 requests/hour
- Return `Retry-After` header on 429 responses
- Monitor rate limit violations

---

## Exception Handling

Add to `GlobalExceptionHandler.java`:

```java
@ExceptionHandler(AssistantProviderException.class)
public ResponseEntity<ErrorResponse> handleAssistantProviderException(AssistantProviderException ex) {
    return buildErrorResponse(HttpStatus.BAD_GATEWAY, "AI assistant service error", ex);
}

@ExceptionHandler(AssistantRateLimitException.class)
public ResponseEntity<ErrorResponse> handleAssistantRateLimitException(AssistantRateLimitException ex) {
    return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", ex);
}
```

---

## System Prompt

Create `src/main/resources/prompts/system-prompt.txt`:

```
You are Connectly Assistant, a helpful AI for the Connectly social platform.

You help users with:
- Creating and managing posts with privacy settings (PUBLIC, FOLLOWERS_ONLY, MUTUALS_ONLY, CIRCLES, PRIVATE)
- Understanding circles and how to share content with specific groups
- Managing followers, follow requests, and blocking
- Navigating orders and payment issues
- Account settings, deletion, and reactivation

Guidelines:
- Be friendly, concise, and helpful
- Respect user privacy - never share information about other users
- If you don't know something specific about Connectly, say so
- For account-sensitive actions, direct users to the appropriate settings page
- Never execute actions on behalf of users - only explain how to do them
```

---

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {
    @Mock private AiProvider aiProvider;
    @Mock private AuthenticationService authService;
    @InjectMocks private AssistantServiceImpl service;
    
    @Test
    void shouldReturnResponse_whenValidRequest() {
        // Arrange
        User user = new User();
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("How do I create a circle?");
        
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(aiProvider.chat(any(), eq(user))).thenReturn(
            new AssistantChatResponse("To create a circle...", null, null)
        );
        
        // Act
        GenericResponse<AssistantChatResponse> response = service.chat(request);
        
        // Assert
        assertThat(response.getData().getResponse()).startsWith("To create");
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class AssistantControllerTest {
    @Autowired private MockMvc mockMvc;
    
    @Test
    @WithMockUser
    void shouldReturn200_whenValidChatRequest() throws Exception {
        mockMvc.perform(post("/v1/assistant/chat")
                .contentType(APPLICATION_JSON)
                .content("{\"message\":\"How do I block someone?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.response").exists());
    }
    
    @Test
    void shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/assistant/chat")
                .contentType(APPLICATION_JSON)
                .content("{\"message\":\"Hello\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## Environment Variables

```bash
# Required
GEMINI_API_KEY=your-api-key-here

# Optional (with defaults in application.properties)
ASSISTANT_PROVIDER=gemini
ASSISTANT_RATE_LIMIT_RPM=30
```

---

## Acceptance Criteria

### Functional

- [ ] User can send chat messages via `POST /api/v1/assistant/chat`
- [ ] AI returns contextually relevant responses about Connectly features
- [ ] API key loaded from environment variables
- [ ] Endpoint requires valid JWT authentication
- [ ] Rate limiting prevents abuse (30 req/min per user)
- [ ] Invalid requests return appropriate error codes

### Non-Functional

- [ ] API responds in < 3 seconds (p95)
- [ ] Circuit breaker handles provider failures gracefully
- [ ] Zero API key exposures in logs
- [ ] Code coverage > 80%
- [ ] Endpoint documented in OpenAPI/Swagger

### Quality Gates

- [ ] Code review approved
- [ ] Unit and integration tests passing
- [ ] No critical security vulnerabilities
- [ ] Follows existing Connectly code patterns

---

## References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Google Gemini API Docs](https://ai.google.dev/docs)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- Existing patterns: `orders_api/`, `payments_api/`, `GlobalExceptionHandler.java`
