package com.jomariabejo.connectly_api.exception;

import com.jomariabejo.connectly_api.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>The advice has no collaborators, so each test invokes the {@code @ExceptionHandler} methods
 * directly on a {@code new GlobalExceptionHandler()} — no Spring context, no MockMvc, no Mockito.
 * Every test asserts the complete contract of a mapping: the HTTP status of the entity, the
 * mirrored numeric {@code status} field, the {@code error} title, the {@code message}, and the
 * {@code timestamp}. The timestamp is checked against {@code System.currentTimeMillis()} captures
 * taken immediately before and after the call rather than against a fixed value, so the assertion
 * can never flake on clock granularity.
 *
 * <p>The twelve one-line business mappings all delegate to the same {@code buildErrorResponse}
 * helper, so they live together in one nested class instead of twelve; the handlers with logic of
 * their own (validation aggregation, Spring self-describing errors, the 500 catch-all) each get
 * their own nested class.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedError,
            String expectedMessage,
            long before,
            long after) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(body.getError()).isEqualTo(expectedError);
        assertThat(body.getMessage()).isEqualTo(expectedMessage);
        assertThat(body.getTimestamp())
                .as("timestamp must be taken at handling time")
                .isBetween(before, after);
    }

    /**
     * {@link MethodArgumentNotValidException} and {@link MethodArgumentTypeMismatchException}
     * both demand a {@link MethodParameter} in their constructors. The handler never reads it,
     * so this points at the throwaway {@link #dummyEndpoint(String)} below.
     */
    private static MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", String.class), 0);
    }

    @SuppressWarnings("unused")
    private static void dummyEndpoint(String body) {
    }

    @Nested
    @DisplayName("business exception mappings")
    class BusinessExceptionMappings {

        @Test
        @DisplayName("maps UserAlreadyExistsException to 409 'User already exists'")
        void userAlreadyExists() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleUserAlreadyExistsException(
                    new UserAlreadyExistsException("Username 'someone' is taken"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.CONFLICT, "User already exists",
                    "Username 'someone' is taken", before, after);
        }

        @Test
        @DisplayName("maps EmailAlreadyInUseException to 409 'Email already in use'")
        void emailAlreadyInUse() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleEmailAlreadyInUseException(
                    new EmailAlreadyInUseException("Email someone@example.com is already registered"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.CONFLICT, "Email already in use",
                    "Email someone@example.com is already registered", before, after);
        }

        @Test
        @DisplayName("maps InvalidCredentialsException to 401 'Invalid credentials'")
        void invalidCredentials() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentialsException(
                    new InvalidCredentialsException("Invalid email or password"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid credentials",
                    "Invalid email or password", before, after);
        }

        @Test
        @DisplayName("maps PostNotFoundException to 404 'Post not found'")
        void postNotFound() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handlePostNotFoundException(
                    new PostNotFoundException(42L));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.NOT_FOUND, "Post not found",
                    "Post not found with id: 42", before, after);
        }

        @Test
        @DisplayName("maps CommentNotFoundException to 404 'Comment not found'")
        void commentNotFound() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleCommentNotFoundException(
                    new CommentNotFoundException("Comment not found with id: 7"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.NOT_FOUND, "Comment not found",
                    "Comment not found with id: 7", before, after);
        }

        @Test
        @DisplayName("maps UnauthorizedAccessException to 403 'Forbidden', not 401")
        void unauthorizedAccess() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedAccessException(
                    new UnauthorizedAccessException("You do not own this post"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not own this post", before, after);
        }

        @Test
        @DisplayName("maps WeakPasswordException to 400 'Password too weak'")
        void weakPassword() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleWeakPasswordException(
                    new WeakPasswordException("Password must be at least 8 characters long"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Password too weak",
                    "Password must be at least 8 characters long", before, after);
        }

        @Test
        @DisplayName("maps AccountDeletionScheduledException to 410 'Account scheduled for deletion'")
        void accountDeletionScheduled() {
            LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 1, 10, 30);
            LocalDateTime scheduledDeletionAt = deletedAt.plusDays(30);

            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleAccountDeletionScheduledException(
                    new AccountDeletionScheduledException(
                            "Account is within its deletion grace period", deletedAt, scheduledDeletionAt));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.GONE, "Account scheduled for deletion",
                    "Account is within its deletion grace period", before, after);
        }

        @Test
        @DisplayName("maps AccountReactivationFailedException to 400 'Account reactivation failed'")
        void accountReactivationFailed() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleAccountReactivationFailedException(
                    new AccountReactivationFailedException("Reactivation token has expired"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Account reactivation failed",
                    "Reactivation token has expired", before, after);
        }

        @Test
        @DisplayName("maps InvalidPasswordResetTokenException to 400 'Invalid password reset token'")
        void invalidPasswordResetToken() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleInvalidPasswordResetTokenException(
                    new InvalidPasswordResetTokenException("Token does not match any pending reset"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid password reset token",
                    "Token does not match any pending reset", before, after);
        }

        @Test
        @DisplayName("maps PasswordResetTokenExpiredException to 400 'Password reset token expired'")
        void passwordResetTokenExpired() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handlePasswordResetTokenExpiredException(
                    new PasswordResetTokenExpiredException("Token expired 5 minutes ago"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Password reset token expired",
                    "Token expired 5 minutes ago", before, after);
        }

        @Test
        @DisplayName("maps PasswordResetAttemptsExceededException to 429 'Too many password reset attempts'")
        void passwordResetAttemptsExceeded() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handlePasswordResetAttemptsExceededException(
                    new PasswordResetAttemptsExceededException("Try again in 15 minutes"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts",
                    "Try again in 15 minutes", before, after);
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationException {

        @Test
        @DisplayName("returns 400 'Validation failed' with the field errors sorted and comma-joined")
        void sortsAndJoinsFieldErrors() throws NoSuchMethodException {
            // Errors are registered username-first; the sorted output must lead with email.
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "registerUserDto");
            bindingResult.addError(
                    new FieldError("registerUserDto", "username", "must not be blank"));
            bindingResult.addError(
                    new FieldError("registerUserDto", "email", "must be a well-formed email address"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Validation failed",
                    "email: must be a well-formed email address, username: must not be blank",
                    before, after);
        }
    }

    @Nested
    @DisplayName("handleSpringErrorResponse")
    class HandleSpringErrorResponse {

        @Test
        @DisplayName("reads 405 off HttpRequestMethodNotSupportedException and titles it with the reason phrase")
        void methodNotSupported() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleSpringErrorResponse(
                    new HttpRequestMethodNotSupportedException("PATCH"));
            long after = System.currentTimeMillis();

            // 405 only reaches the body through the org.springframework.web.ErrorResponse cast;
            // a fallback bug would surface here as 400.
            assertErrorResponse(response, HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                    "Request method 'PATCH' is not supported", before, after);
        }

        @Test
        @DisplayName("answers a parameter type mismatch with 400 Bad Request")
        void parameterTypeMismatch() throws NoSuchMethodException {
            // In Spring 6.2 MethodArgumentTypeMismatchException does not implement the
            // org.springframework.web.ErrorResponse interface, so this exercises the handler's
            // BAD_REQUEST fallback branch. The message text is Spring's, so only its stable
            // parts are pinned down.
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "id", dummyMethodParameter(),
                    new NumberFormatException("For input string: \"abc\""));

            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleSpringErrorResponse(ex);
            long after = System.currentTimeMillis();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(body.getError()).isEqualTo("Bad Request");
            assertThat(body.getMessage())
                    .startsWith("Method parameter 'id':")
                    .contains("Failed to convert value of type");
            assertThat(body.getTimestamp()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("maps any unhandled exception to 500 'An unexpected error occurred'")
        void unhandledException() {
            long before = System.currentTimeMillis();
            ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                    new IllegalStateException("connection pool exhausted"));
            long after = System.currentTimeMillis();

            assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred", "connection pool exhausted", before, after);
        }
    }
}
