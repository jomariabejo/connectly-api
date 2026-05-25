package com.jomariabejo.connectly_api.exception;

import com.jomariabejo.connectly_api.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataIntegrityViolationForDuplicateUsernameReturnsReadableConflict() {
        String databaseMessage = "ERROR: duplicate key value violates unique constraint \"app_user_username_key\"";
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException(databaseMessage)
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Request conflicts with existing data");
        assertThat(response.getBody().getMessage()).isEqualTo("Username is already taken");
        assertThat(response.getBody().getMessage()).doesNotContain("insert into app_user");
    }

    @Test
    void badCredentialsReturns401WithFriendlyMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentialsException(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password.");
    }

    @Test
    void httpRequestMethodNotSupportedReturns405WithSupportedMethods() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException(
                "PATCH",
                List.of("GET", "POST")
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleHttpRequestMethodNotSupportedException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Method not allowed");
        assertThat(response.getBody().getMessage()).contains("Supported methods");
        assertThat(response.getBody().getMessage()).contains("GET");
        assertThat(response.getBody().getMessage()).contains("POST");
    }

    @Test
    void disabledAccountReturns403WithVerificationHint() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDisabledException(new DisabledException("User is disabled"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("verify your email");
    }
}
