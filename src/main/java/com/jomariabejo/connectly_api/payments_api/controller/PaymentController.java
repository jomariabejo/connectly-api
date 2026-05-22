package com.jomariabejo.connectly_api.payments_api.controller;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.payments_api.dto.CreateCheckoutRequest;
import com.jomariabejo.connectly_api.payments_api.dto.PaymentResponseDto;
import com.jomariabejo.connectly_api.payments_api.dto.WebhookResponseDto;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.exception.InvalidPaymentRequestException;
import com.jomariabejo.connectly_api.payments_api.service.PaymentService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class PaymentController {
    private final PaymentService paymentService;
    private final AuthenticationService authenticationService;

    public PaymentController(PaymentService paymentService, AuthenticationService authenticationService) {
        this.paymentService = paymentService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/payments/checkout")
    public ResponseEntity<PaymentResponseDto> createCheckout(@RequestBody @Valid CreateCheckoutRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();
        PaymentResponseDto response = paymentService.createCheckout(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long paymentId) {
        User currentUser = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(paymentService.getPayment(paymentId, currentUser));
    }

    @GetMapping("/orders/{orderId}/payments")
    public ResponseEntity<List<PaymentResponseDto>> getOrderPayments(@PathVariable Long orderId) {
        User currentUser = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(paymentService.getOrderPayments(orderId, currentUser));
    }

    @PostMapping("/payments/webhooks/{provider}")
    public ResponseEntity<WebhookResponseDto> processWebhook(
            @PathVariable String provider,
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers) {
        WebhookResponseDto response = paymentService.processWebhook(parseProvider(provider), rawPayload, headers);
        return ResponseEntity.accepted().body(response);
    }

    private PaymentProvider parseProvider(String provider) {
        try {
            return PaymentProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidPaymentRequestException("Unsupported payment provider: " + provider);
        }
    }
}
