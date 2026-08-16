package com.jomariabejo.connectly_api.exception;

import com.jomariabejo.connectly_api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "User already exists", ex);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyInUseException(EmailAlreadyInUseException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Email already in use", ex);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFoundException(PostNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Post not found", ex);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFoundException(CommentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Comment not found", ex);
    }

    /**
     * The caller is authenticated but does not own the resource.
     *
     * <p>403, not 401: 401 means "I do not know who you are" and is answered by
     * {@code JwtAuthenticationEntryPoint} when the token is missing or invalid. Reaching here means
     * the token was fine.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWeakPasswordException(WeakPasswordException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password too weak", ex);
    }

    @ExceptionHandler(AccountDeletionScheduledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDeletionScheduledException(AccountDeletionScheduledException ex) {
        return buildErrorResponse(HttpStatus.GONE, "Account scheduled for deletion", ex);
    }

    @ExceptionHandler(AccountReactivationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAccountReactivationFailedException(AccountReactivationFailedException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Account reactivation failed", ex);
    }

    /**
     * The password-reset exceptions are usually answered locally by
     * {@code AuthenticationController}'s catch blocks; these mappings cover any other route that
     * lets them escape, which previously fell through to the 500 catch-all.
     */
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetTokenException(InvalidPasswordResetTokenException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid password reset token", ex);
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handlePasswordResetTokenExpiredException(PasswordResetTokenExpiredException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password reset token expired", ex);
    }

    @ExceptionHandler(PasswordResetAttemptsExceededException.class)
    public ResponseEntity<ErrorResponse> handlePasswordResetAttemptsExceededException(PasswordResetAttemptsExceededException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts", ex);
    }

    /**
     * Bean-validation failures on {@code @Valid @RequestBody} arguments.
     *
     * <p>This handler has to exist explicitly. {@code ExceptionHandlerExceptionResolver} runs before
     * Spring's {@code DefaultHandlerExceptionResolver}, so without it the catch-all
     * {@link #handleGenericException(Exception)} below claims {@code MethodArgumentNotValidException}
     * and every malformed request body comes back as 500 instead of 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));

        logger.warn("Validation failed: {}", details);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                details,
                System.currentTimeMillis()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Exceptions Spring raises that already carry their own status — an unmatched URL
     * ({@code NoResourceFoundException} → 404), a wrong verb
     * ({@code HttpRequestMethodNotSupportedException} → 405), an unreadable or wrongly-typed body
     * ({@code HttpMessageNotReadableException} → 400), and so on.
     *
     * <p>Like the validation handler above, this must be declared explicitly: without it the
     * catch-all below claims them all and reports 500, so a simple typo in a URL looked like a
     * server fault.
     *
     * <p>The classes are listed individually rather than caught via a common supertype because
     * they share only the {@code org.springframework.web.ErrorResponse} <i>interface</i> —
     * {@code NoResourceFoundException}, for one, extends {@code ServletException} rather than
     * {@code ErrorResponseException}. The cast below is what reads the status back off them.
     */
    @ExceptionHandler({
            ErrorResponseException.class,
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleSpringErrorResponse(Exception ex) {
        HttpStatus status = ex instanceof org.springframework.web.ErrorResponse spring
                ? HttpStatus.valueOf(spring.getStatusCode().value())
                : HttpStatus.BAD_REQUEST;

        logger.warn("{}: {}", status, ex.getMessage());
        return buildErrorResponse(status, status.getReasonPhrase(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorTitle, Exception ex) {
        logger.error("{}: {}", errorTitle, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorTitle,
                ex.getMessage(),
                System.currentTimeMillis()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}
