package com.jomariabejo.connectly_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers authenticated-but-forbidden requests with <b>403</b> and the standard
 * {@link ErrorResponse} body, so a caller lacking a role gets the same JSON shape as every other
 * error rather than Spring's default empty response.
 *
 * @see JwtAuthenticationEntryPoint
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource.",
                System.currentTimeMillis()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
