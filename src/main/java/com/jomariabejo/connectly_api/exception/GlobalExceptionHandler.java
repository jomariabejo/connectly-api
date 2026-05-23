package com.jomariabejo.connectly_api.exception;

import com.jomariabejo.connectly_api.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        logger.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid credentials",
                "Invalid email or password.",
                System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        logger.debug("User not found during authentication: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid or expired session. Please sign in again.",
                System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex) {
        logger.debug("Disabled account login attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Account not activated",
                "Please verify your email before signing in.",
                System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFoundException(PostNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Post not found", ex);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFoundException(CommentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Comment not found", ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Order not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFilterException(com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid filter parameters", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.inventory_api.exception.InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInventoryNotFoundException(com.jomariabejo.connectly_api.inventory_api.exception.InventoryNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Inventory item not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.inventory_api.exception.InvalidInventoryRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInventoryRequestException(com.jomariabejo.connectly_api.inventory_api.exception.InvalidInventoryRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid inventory request", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.inventory_api.exception.InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventoryException(com.jomariabejo.connectly_api.inventory_api.exception.InsufficientInventoryException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Insufficient inventory", ex);
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

    @ExceptionHandler(com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFoundException(com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Tenant not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.tenant_api.exception.TenantRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleTenantRegistrationException(
            com.jomariabejo.connectly_api.tenant_api.exception.TenantRegistrationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Registration failed", ex);
    }

    @ExceptionHandler(InvalidVerificationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationException(InvalidVerificationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Verification failed", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.tenant_api.exception.InvalidInvitationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvitationException(
            com.jomariabejo.connectly_api.tenant_api.exception.InvalidInvitationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid invitation", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDeniedException(com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Tenant access denied", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.tenant_api.exception.ProductNotSubscribedException.class)
    public ResponseEntity<ErrorResponse> handleProductNotSubscribedException(com.jomariabejo.connectly_api.tenant_api.exception.ProductNotSubscribedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Product not subscribed", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.crm_api.exception.CrmCustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCrmCustomerNotFoundException(com.jomariabejo.connectly_api.crm_api.exception.CrmCustomerNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "CRM customer not found", ex);
    }

    @ExceptionHandler(com.jomariabejo.connectly_api.ticketing_api.exception.TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFoundException(com.jomariabejo.connectly_api.ticketing_api.exception.TicketNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Ticket not found", ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Invalid state", ex);
    }

    @ExceptionHandler(AccountDeletionScheduledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDeletionScheduledException(AccountDeletionScheduledException ex) {
        return buildErrorResponse(HttpStatus.GONE, "Account scheduled for deletion", ex);
    }

    @ExceptionHandler(AccountReactivationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAccountReactivationFailedException(AccountReactivationFailedException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Account reactivation failed", ex);
    }

    @ExceptionHandler(InvalidReactivationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReactivationTokenException(InvalidReactivationTokenException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid reactivation token", ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Validation failed" : error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                message,
                System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", ex);
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
