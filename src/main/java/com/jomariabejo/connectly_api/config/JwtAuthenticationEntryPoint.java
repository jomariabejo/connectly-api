package com.jomariabejo.connectly_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers unauthenticated requests with <b>401</b> and the standard {@link ErrorResponse} body.
 *
 * <p>Spring Security's default entry point returns 403 for an anonymous caller, which inverted the
 * usual convention: a missing token looked identical to a permission failure. With this in place
 * the split is conventional —
 *
 * <ul>
 *   <li><b>401</b> — no token, malformed token, expired token: "I do not know who you are"
 *   <li><b>403</b> — valid token, insufficient rights: "I know who you are, and no"
 * </ul>
 *
 * @see JwtAccessDeniedHandler
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentication required. Send a bearer token from POST /auth/login.",
                System.currentTimeMillis()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
