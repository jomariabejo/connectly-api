---
name: "AI Assistant: Phase 1 - Foundation Setup"
about: Implement basic AI chat endpoint with Gemini integration
title: "[AI Assistant] Phase 1: Foundation Setup"
labels: enhancement, ai, backend
assignees: ''
---

# Phase 1: AI Assistant Foundation Setup

**Parent Issue:** AI Chat Assistant Integration

## Objective

Implement a basic working AI chat endpoint that allows authenticated users to send messages and receive responses from Google Gemini API.

---

## Scope

### In Scope

- Spring AI + Gemini dependency setup
- Basic configuration properties
- Core DTOs (request/response)
- Service layer implementation
- REST controller with single endpoint
- Exception handling integration
- Unit tests for service layer

### Out of Scope

- Rate limiting (Phase 2)
- Circuit breaker/resilience (Phase 2)
- Metrics and observability (Phase 3)
- Provider abstraction (Phase 4)
- Conversation memory/sessions

---

## Tasks

### 1. Dependency Setup

- [ ] Add Spring AI BOM and Gemini starter to `build.gradle`
- [ ] Add Spring AI milestone repository
- [ ] Verify dependencies resolve correctly

```groovy
// Add to build.gradle
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

dependencies {
    // AI Assistant
    implementation platform('org.springframework.ai:spring-ai-bom:1.0.0-M3')
    implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter'
}
```

### 2. Configuration

- [ ] Add API path constant to `ApiPaths.java`

```java
public static final String V1_ASSISTANT = V1 + "/assistant";
```

- [ ] Add assistant properties to `application.properties`

```properties
# AI Assistant Configuration
assistant.provider=gemini
assistant.gemini.api-key=${GEMINI_API_KEY:}
assistant.gemini.model=gemini-1.5-flash
assistant.gemini.temperature=0.7
assistant.gemini.max-tokens=1024
assistant.gemini.timeout-ms=5000
```

- [ ] Create `AssistantConfiguration.java` with Gemini client bean

### 3. DTOs

- [ ] Create `AssistantChatRequest.java`

```java
@Getter @Setter
public class AssistantChatRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String message;
    
    private String sessionId;
}
```

- [ ] Create `AssistantChatResponse.java`

```java
@Getter @Setter
@AllArgsConstructor
public class AssistantChatResponse {
    private String response;
    private String sessionId;
    private AssistantMetadata metadata;
}
```

- [ ] Create `AssistantMetadata.java`

```java
@Getter @Setter
@AllArgsConstructor
public class AssistantMetadata {
    private Integer tokensUsed;
    private String model;
    private Long latencyMs;
}
```

### 4. Service Layer

- [ ] Create `AssistantService.java` interface

```java
public interface AssistantService {
    GenericResponse<AssistantChatResponse> chat(AssistantChatRequest request);
}
```

- [ ] Create `AssistantServiceImpl.java` implementation
- [ ] Integrate with Spring AI ChatClient
- [ ] Use `AuthenticationService.getAuthenticatedUser()` for user context

### 5. Controller

- [ ] Create `AssistantController.java`

```java
@RestController
@RequestMapping(ApiPaths.V1_ASSISTANT)
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistantService;
    
    @PostMapping("/chat")
    public ResponseEntity<GenericResponse<AssistantChatResponse>> chat(
            @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(request));
    }
}
```

### 6. Exception Handling

- [ ] Create `AssistantProviderException.java`

```java
public class AssistantProviderException extends RuntimeException {
    public AssistantProviderException(String message) {
        super(message);
    }
    
    public AssistantProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] Add handler to `GlobalExceptionHandler.java`

```java
@ExceptionHandler(AssistantProviderException.class)
public ResponseEntity<ErrorResponse> handleAssistantProviderException(AssistantProviderException ex) {
    return buildErrorResponse(HttpStatus.BAD_GATEWAY, "AI assistant service error", ex);
}
```

### 7. System Prompt

- [ ] Create `src/main/resources/prompts/system-prompt.txt`

```
You are Connectly Assistant, a helpful AI for the Connectly social platform.

You help users with:
- Creating and managing posts with privacy settings
- Understanding circles and follower-only content
- Managing followers, blocking, and social connections
- Navigating orders and payment issues
- Account settings and security

Be friendly, concise, and respect user privacy.
```

### 8. Testing

- [ ] Create `AssistantServiceTest.java` with unit tests
- [ ] Create `AssistantControllerTest.java` with integration tests
- [ ] Mock Gemini API responses for testing
- [ ] Test validation errors (empty message, too long message)
- [ ] Test authentication requirement (401 for unauthenticated)

---

## Module Structure

```
src/main/java/com/jomariabejo/connectly_api/assistant_api/
├── config/
│   └── AssistantConfiguration.java
├── controller/
│   └── AssistantController.java
├── service/
│   ├── AssistantService.java
│   └── impl/
│       └── AssistantServiceImpl.java
├── dto/
│   ├── AssistantChatRequest.java
│   ├── AssistantChatResponse.java
│   └── AssistantMetadata.java
└── exception/
    └── AssistantProviderException.java

src/main/resources/
└── prompts/
    └── system-prompt.txt

src/test/java/com/jomariabejo/connectly_api/assistant_api/
├── service/
│   └── AssistantServiceTest.java
└── controller/
    └── AssistantControllerTest.java
```

---

## API Contract

**Request:**

```http
POST /api/v1/assistant/chat
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "message": "How do I create a circle?",
  "sessionId": null
}
```

**Response (200):**

```json
{
  "message": "Assistant response generated",
  "data": {
    "response": "To create a circle in Connectly...",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "metadata": {
      "tokensUsed": 120,
      "model": "gemini-1.5-flash",
      "latencyMs": 1150
    }
  }
}
```

---

## Environment Variables

```bash
GEMINI_API_KEY=your-api-key-here
```

---

## Acceptance Criteria

- [ ] `POST /api/v1/assistant/chat` returns 200 with valid response
- [ ] Endpoint requires JWT authentication (returns 401 without token)
- [ ] Invalid requests return 400 with validation errors
- [ ] Provider errors return 502 with appropriate message
- [ ] API key loaded from environment variable
- [ ] Unit tests pass with >80% coverage on service layer
- [ ] Integration tests verify endpoint behavior

---

## Definition of Done

- [ ] All tasks completed
- [ ] Code reviewed and approved
- [ ] Tests passing in CI
- [ ] Documentation updated (OpenAPI auto-generated)
- [ ] Tested manually with valid Gemini API key
