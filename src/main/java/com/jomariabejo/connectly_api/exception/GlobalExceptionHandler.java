package com.jomariabejo.connectly_api.exception;

import com.jomariabejo.connectly_api.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFilterException(com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid filter parameters", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.payments_api.exception.PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(com.jomariabejo.connectly_api.payments_api.exception.PaymentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Payment not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.payments_api.exception.InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentRequestException(com.jomariabejo.connectly_api.payments_api.exception.InvalidPaymentRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid payment request", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.payments_api.exception.PaymentWebhookVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentWebhookVerificationException(com.jomariabejo.connectly_api.payments_api.exception.PaymentWebhookVerificationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid payment webhook signature", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGatewayException(com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayException ex) {
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Payment gateway error", ex);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized access", ex);
    }

    @ExceptionHandler(AccountDeletionScheduledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDeletionScheduledException(AccountDeletionScheduledException ex) {
        return buildErrorResponse(HttpStatus.GONE, "Account scheduled for deletion", ex);
    }

    @ExceptionHandler(AccountReactivationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAccountReactivationFailedException(AccountReactivationFailedException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Account reactivation failed", ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = resolveDataIntegrityMessage(ex);
        logger.warn("Data integrity violation: {}", message);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Request conflicts with existing data",
                message,
                System.currentTimeMillis()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String mostSpecificMessage = ex.getMostSpecificCause().getMessage();
        String message = mostSpecificMessage == null ? ex.getMessage() : mostSpecificMessage;

        if (message != null && message.contains("app_user_username_key")) {
            return "Username is already taken";
        }

        if (message != null && message.contains("app_user_email_key")) {
            return "Email is already registered";
        }

        return "The request conflicts with an existing record. Please review your input and try again.";
    }
}
