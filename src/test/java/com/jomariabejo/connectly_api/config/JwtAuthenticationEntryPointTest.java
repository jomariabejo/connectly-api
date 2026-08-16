package com.jomariabejo.connectly_api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtAuthenticationEntryPoint}.
 *
 * <p>The entry point only needs an {@link ObjectMapper} and a servlet response, so no Mockito is
 * involved: a real {@code ObjectMapper} serializes the body and Spring's
 * {@link MockHttpServletResponse} captures what was written, which lets the tests parse the JSON
 * back and assert the exact contract ({@code ErrorResponse} has no no-arg constructor, so the body
 * is read back as a {@link JsonNode} rather than deserialized into the DTO).
 *
 * <p>The timestamp is generated with {@code System.currentTimeMillis()} inside the class under
 * test, so the tests bracket the call with before/after captures instead of asserting an exact
 * value.
 */
@DisplayName("JwtAuthenticationEntryPoint")
class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtAuthenticationEntryPoint entryPoint;

    /** An arbitrary concrete subclass -- {@link AuthenticationException} itself is abstract. */
    private static AuthenticationException anyAuthenticationException() {
        return new AuthenticationException("x") {
        };
    }

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Nested
    @DisplayName("commence")
    class Commence {

        @Test
        @DisplayName("answers with 401 and a JSON content type")
        void respondsWith401Json() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            entryPoint.commence(new MockHttpServletRequest(), response, anyAuthenticationException());

            // 401 for "who are you?" -- Spring's default entry point used to answer 403 here,
            // which made a missing token indistinguishable from a permission failure.
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("writes the standard ErrorResponse body with a current timestamp")
        void writesErrorResponseBody() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            long before = System.currentTimeMillis();
            entryPoint.commence(new MockHttpServletRequest(), response, anyAuthenticationException());
            long after = System.currentTimeMillis();

            JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(401);
            assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
            assertThat(body.get("message").asText())
                    .isEqualTo("Authentication required. Send a bearer token from POST /auth/login.");
            assertThat(body.get("timestamp").asLong())
                    .as("timestamp is taken while the request is handled")
                    .isBetween(before, after);
        }

        @Test
        @DisplayName("responds the same way regardless of the AuthenticationException subtype")
        void handlesInsufficientAuthenticationException() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            entryPoint.commence(new MockHttpServletRequest(), response,
                    new InsufficientAuthenticationException("Full authentication is required"));

            JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(body.get("status").asInt()).isEqualTo(401);
            assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        }
    }
}
