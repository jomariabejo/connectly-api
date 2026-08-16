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
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtAccessDeniedHandler}.
 *
 * <p>Same setup as {@code JwtAuthenticationEntryPointTest}: a real {@link ObjectMapper} plus
 * Spring's {@link MockHttpServletResponse}, no mocks. The written body is parsed back as a
 * {@link JsonNode} because {@code ErrorResponse} has no no-arg constructor for Jackson to
 * deserialize into, and the timestamp is asserted against a before/after bracket rather than an
 * exact value.
 */
@DisplayName("JwtAccessDeniedHandler")
class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JwtAccessDeniedHandler(objectMapper);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("answers with 403 and a JSON content type")
        void respondsWith403Json() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

            // 403 for "I know who you are, and no" -- the authenticated-but-forbidden half of
            // the 401/403 split established by JwtAuthenticationEntryPoint.
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("writes the standard ErrorResponse body with a current timestamp")
        void writesErrorResponseBody() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            long before = System.currentTimeMillis();
            handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));
            long after = System.currentTimeMillis();

            JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(403);
            assertThat(body.get("error").asText()).isEqualTo("Forbidden");
            assertThat(body.get("message").asText())
                    .isEqualTo("You do not have permission to access this resource.");
            assertThat(body.get("timestamp").asLong())
                    .as("timestamp is taken while the request is handled")
                    .isBetween(before, after);
        }
    }
}
