package com.jomariabejo.connectly_api.payments_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException;
import com.jomariabejo.connectly_api.orders_api.repository.OrderRepository;
import com.jomariabejo.connectly_api.payments_api.dto.CreateCheckoutRequest;
import com.jomariabejo.connectly_api.payments_api.dto.PaymentResponseDto;
import com.jomariabejo.connectly_api.payments_api.dto.WebhookResponseDto;
import com.jomariabejo.connectly_api.payments_api.entity.Payment;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentAttempt;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentAttemptRequestType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentAttemptStatus;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentEvent;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentEventProcessingStatus;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentIntentType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import com.jomariabejo.connectly_api.payments_api.exception.InvalidPaymentRequestException;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayException;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentNotFoundException;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentWebhookVerificationException;
import com.jomariabejo.connectly_api.payments_api.mapper.PaymentMapper;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutCommand;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutResult;
import com.jomariabejo.connectly_api.payments_api.provider.PaymentGateway;
import com.jomariabejo.connectly_api.payments_api.provider.PaymentGatewayRegistry;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderWebhookEvent;
import com.jomariabejo.connectly_api.payments_api.provider.WebhookVerificationResult;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentAttemptRepository;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentEventRepository;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentEventRepository paymentEventRepository = mock(PaymentEventRepository.class);
    private final PaymentAttemptRepository paymentAttemptRepository = mock(PaymentAttemptRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaymentGatewayRegistry paymentGatewayRegistry = mock(PaymentGatewayRegistry.class);
    private final PaymentGateway paymentGateway = mock(PaymentGateway.class);
    private final PaymentService paymentService = new PaymentService(
            paymentRepository,
            paymentEventRepository,
            paymentAttemptRepository,
            orderRepository,
            paymentGatewayRegistry,
            new PaymentMapper()
    );

    private User user;
    private User otherUser;
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);

        otherUser = new User();
        otherUser.setId(20L);

        order = Order.builder()
                .id(99L)
                .customer(user)
                .totalAmount(new BigDecimal("49.98"))
                .marketplaceSource("amazon")
                .build();
    }

    // ==================== CREATE CHECKOUT TESTS ====================

    @Test
    void createCheckoutUsesOrderAmountAndStoresProviderReferences() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(99L, PaymentStatus.PAID)).thenReturn(false);
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class))).thenReturn(CreateCheckoutResult.builder()
                .providerCheckoutId("cs_test_123")
                .checkoutUrl("https://checkout.example/pay")
                .status(PaymentStatus.PENDING)
                .build());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            return payment;
        });

        PaymentResponseDto response = paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .successUrl("http://localhost:3000/success")
                .cancelUrl("http://localhost:3000/cancel")
                .idempotencyKey("idem-1")
                .intentType(PaymentIntentType.CAPTURE_NOW)
                .build(), user);

        ArgumentCaptor<CreateCheckoutCommand> commandCaptor = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
        verify(paymentGateway).createCheckout(commandCaptor.capture());

        assertThat(commandCaptor.getValue().getAmountMinor()).isEqualTo(4998L);
        assertThat(response.getAmount()).isEqualTo(4998L);
        assertThat(response.getProviderCheckoutId()).isEqualTo("cs_test_123");
        assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.example/pay");
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingPaymentWithoutGatewayCall() {
        Payment existing = Payment.builder()
                .id(7L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .intentType(PaymentIntentType.CAPTURE_NOW)
                .idempotencyKey("idem-1")
                .build();
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentResponseDto response = paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .successUrl("http://localhost:3000/success")
                .cancelUrl("http://localhost:3000/cancel")
                .idempotencyKey("idem-1")
                .build(), user);

        assertThat(response.getPaymentId()).isEqualTo(7L);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void duplicateIdempotencyKeyForAnotherUserIsRejectedWithoutGatewayCall() {
        Payment existing = Payment.builder()
                .id(7L)
                .order(order)
                .customer(otherUser)
                .provider(PaymentProvider.PAYMONGO)
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .intentType(PaymentIntentType.CAPTURE_NOW)
                .idempotencyKey("idem-1")
                .build();
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-1")
                .build(), user))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only access your own payments");

        verifyNoInteractions(paymentGateway, paymentGatewayRegistry, orderRepository);
    }

    @Test
    void createCheckoutGeneratesIdempotencyKeyUppercasesCurrencyAndRecordsSuccessfulAttempt() {
        when(paymentRepository.findByIdempotencyKey(any(String.class))).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(99L, PaymentStatus.PAID)).thenReturn(false);
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class))).thenReturn(CreateCheckoutResult.builder()
                .providerPaymentId("pay_123")
                .providerCheckoutId("cs_test_123")
                .checkoutUrl("https://checkout.example/pay")
                .status(null)
                .build());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            return payment;
        });

        PaymentResponseDto response = paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("php")
                .successUrl("http://localhost:3000/success")
                .cancelUrl("http://localhost:3000/cancel")
                .idempotencyKey("   ")
                .build(), user);

        ArgumentCaptor<CreateCheckoutCommand> commandCaptor = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
        verify(paymentGateway).createCheckout(commandCaptor.capture());

        CreateCheckoutCommand command = commandCaptor.getValue();
        assertThat(command.getIdempotencyKey()).startsWith("checkout-99-PAYMONGO-");
        assertThat(command.getCurrency()).isEqualTo("PHP");
        assertThat(command.getIntentType()).isEqualTo(PaymentIntentType.CAPTURE_NOW);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getProviderPaymentId()).isEqualTo("pay_123");

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getRequestType()).isEqualTo(PaymentAttemptRequestType.CREATE_CHECKOUT);
        assertThat(attemptCaptor.getValue().getProviderReferenceId()).isEqualTo("cs_test_123");
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
    }

    @Test
    void createCheckoutRoundsOrderTotalToMinorUnitsUsingHalfUp() {
        order.setTotalAmount(new BigDecimal("10.235"));
        when(paymentRepository.findByIdempotencyKey("idem-rounding")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(99L, PaymentStatus.PAID)).thenReturn(false);
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class))).thenReturn(CreateCheckoutResult.builder()
                .providerCheckoutId("cs_rounding")
                .checkoutUrl("https://checkout.example/pay")
                .status(PaymentStatus.PENDING)
                .build());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            return payment;
        });

        paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-rounding")
                .build(), user);

        ArgumentCaptor<CreateCheckoutCommand> commandCaptor = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
        verify(paymentGateway).createCheckout(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getAmountMinor()).isEqualTo(1024L);
    }

    @Test
    void invalidWebhookSignatureIsRejectedBeforeParsing() {
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.invalid("bad signature"));

        assertThatThrownBy(() -> paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of()))
                .isInstanceOf(PaymentWebhookVerificationException.class)
                .hasMessage("bad signature");
    }

    @Test
    void duplicateWebhookEventIsAcknowledgedWithoutReprocessing() {
        PaymentEvent existingEvent = PaymentEvent.builder()
                .provider(PaymentProvider.PAYMONGO)
                .providerEventId("evt_1")
                .eventType("payment.paid")
                .processingStatus(PaymentEventProcessingStatus.PROCESSED)
                .rawPayload("{}")
                .build();

        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook("{}")).thenReturn(ProviderWebhookEvent.builder()
                .providerEventId("evt_1")
                .eventType("payment.paid")
                .paymentStatus(PaymentStatus.PAID)
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, "evt_1"))
                .thenReturn(Optional.of(existingEvent));

        WebhookResponseDto response = paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of());

        assertThat(response.getProcessingStatus()).isEqualTo(PaymentEventProcessingStatus.DUPLICATE);
    }

    @Test
    void paidWebhookUpdatesPaymentAndOrderPaymentStatus() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .providerCheckoutId("cs_test_123")
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .intentType(PaymentIntentType.CAPTURE_NOW)
                .idempotencyKey("idem-1")
                .build();

        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook("{}")).thenReturn(ProviderWebhookEvent.builder()
                .providerEventId("evt_1")
                .eventType("payment.paid")
                .providerCheckoutId("cs_test_123")
                .providerPaymentId("pay_123")
                .paymentStatus(PaymentStatus.PAID)
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, "evt_1"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYMONGO, "pay_123"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderCheckoutId(PaymentProvider.PAYMONGO, "cs_test_123"))
                .thenReturn(Optional.of(payment));

        WebhookResponseDto response = paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of());

        assertThat(response.getProcessingStatus()).isEqualTo(PaymentEventProcessingStatus.PROCESSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderPaymentId()).isEqualTo("pay_123");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    // ==================== GETPAYMENT TESTS ====================

    @Test
    void getPaymentReturnsPaymentWhenOwner() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentService.getPayment(1L, user);

        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(4998L);
    }

    @Test
    void getPaymentThrowsNotFoundWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(999L, user))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getPaymentThrowsInvalidRequestWhenNotOwner() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(otherUser)
                .provider(PaymentProvider.PAYMONGO)
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPayment(1L, user))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only access your own payments");
    }

    @Test
    void getPaymentThrowsInvalidRequestWhenAuthenticatedUserIsMissing() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPayment(1L, null))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only access your own payments");
    }

    // ==================== GETORDERPAYMENTS TESTS ====================

    @Test
    void getOrderPaymentsReturnsPaymentsWhenOwner() {
        Payment payment1 = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .status(PaymentStatus.PENDING)
                .build();
        Payment payment2 = Payment.builder()
                .id(2L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .status(PaymentStatus.PAID)
                .build();

        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdOrderByCreatedDateDesc(99L)).thenReturn(List.of(payment1, payment2));

        List<PaymentResponseDto> response = paymentService.getOrderPayments(99L, user);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getPaymentId()).isEqualTo(1L);
        assertThat(response.get(1).getPaymentId()).isEqualTo(2L);
    }

    @Test
    void getOrderPaymentsThrowsNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getOrderPayments(999L, user))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderPaymentsThrowsInvalidRequestWhenNotOrderOwner() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.getOrderPayments(99L, otherUser))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only create or view payments for your own orders");
    }

    @Test
    void getOrderPaymentsThrowsInvalidRequestWhenOrderHasNoCustomer() {
        Order orderWithoutCustomer = Order.builder()
                .id(99L)
                .totalAmount(new BigDecimal("49.98"))
                .build();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(orderWithoutCustomer));

        assertThatThrownBy(() -> paymentService.getOrderPayments(99L, user))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only create or view payments for your own orders");
    }

    @Test
    void getOrderPaymentsReturnsEmptyListWhenNoPayments() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdOrderByCreatedDateDesc(99L)).thenReturn(List.of());

        List<PaymentResponseDto> response = paymentService.getOrderPayments(99L, user);

        assertThat(response).isEmpty();
    }

    // ==================== CHECKOUT AUTHORIZATION TESTS ====================

    @Test
    void createCheckoutThrowsInvalidRequestWhenOrderDoesNotExist() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(999L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-1")
                .build(), user))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void createCheckoutThrowsInvalidRequestWhenNotOrderOwner() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-1")
                .build(), otherUser))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("can only create or view payments for your own orders");
    }

    @Test
    void createCheckoutThrowsInvalidRequestWhenOrderAlreadyPaid() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(99L, PaymentStatus.PAID)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-1")
                .build(), user))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("already has a successful payment");
    }

    @Test
    void createCheckoutThrowsInvalidRequestWhenAuthorizeOnlyIntentIsRequested() {
        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-authorize")
                .intentType(PaymentIntentType.AUTHORIZE_ONLY)
                .build(), user))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("AUTHORIZE_ONLY is reserved for a future release");

        verifyNoInteractions(paymentRepository, orderRepository, paymentGatewayRegistry, paymentGateway);
    }

    // ==================== GATEWAYEXCEPTION TESTS ====================

    @Test
    void createCheckoutRecordsFailureWhenGatewayThrowsException() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(99L, PaymentStatus.PAID)).thenReturn(false);
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class)))
                .thenThrow(new PaymentGatewayException("Gateway timeout"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            return payment;
        });

        assertThatThrownBy(() -> paymentService.createCheckout(CreateCheckoutRequest.builder()
                .orderId(99L)
                .provider(PaymentProvider.PAYMONGO)
                .currency("PHP")
                .idempotencyKey("idem-1")
                .build(), user))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessage("Gateway timeout");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());

        Payment failedPayment = paymentCaptor.getAllValues().stream()
                .filter(p -> PaymentStatus.FAILED.equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        assertThat(failedPayment).isNotNull();
        assertThat(failedPayment.getFailureCode()).isEqualTo("GATEWAY_ERROR");

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getRequestType()).isEqualTo(PaymentAttemptRequestType.CREATE_CHECKOUT);
        assertThat(attemptCaptor.getValue().getProviderReferenceId()).isNull();
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
    }

    // ==================== WEBHOOK TESTS ====================

    @Test
    void webhookWithMissingPaymentIsIgnored() {
        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook("{}")).thenReturn(ProviderWebhookEvent.builder()
                .providerEventId("evt_1")
                .eventType("payment.paid")
                .providerCheckoutId("unknown-checkout")
                .paymentStatus(PaymentStatus.PAID)
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, "evt_1"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYMONGO, null))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderCheckoutId(PaymentProvider.PAYMONGO, "unknown-checkout"))
                .thenReturn(Optional.empty());

        WebhookResponseDto response = paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of());

        assertThat(response.getProcessingStatus()).isEqualTo(PaymentEventProcessingStatus.IGNORED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void webhookFallsBackToPayloadHashAndUnknownEventTypeWhenProviderFieldsAreMissing() {
        String rawPayload = "{\"data\":{\"id\":\"evt-missing\"}}";
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .providerPaymentId("pay_123")
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();
        String fallbackEventId = "payload-" + Integer.toHexString(rawPayload.hashCode());

        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook(rawPayload, Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook(rawPayload)).thenReturn(ProviderWebhookEvent.builder()
                .providerPaymentId("pay_123")
                .eventType(null)
                .paymentStatus(null)
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, fallbackEventId))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYMONGO, "pay_123"))
                .thenReturn(Optional.of(payment));

        WebhookResponseDto response = paymentService.processWebhook(PaymentProvider.PAYMONGO, rawPayload, Map.of());

        assertThat(response.getProviderEventId()).isEqualTo(fallbackEventId);
        assertThat(response.getEventType()).isEqualTo("unknown");
        assertThat(response.getProcessingStatus()).isEqualTo(PaymentEventProcessingStatus.PROCESSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentAttemptRepository);

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getProviderEventId()).isEqualTo(fallbackEventId);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("unknown");
        assertThat(eventCaptor.getValue().getPayment()).isEqualTo(payment);
    }

    @Test
    void failedWebhookUpdatesFailureDetailsAndRecordsWebhookAttempt() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .providerCheckoutId("cs_test_123")
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook("{}")).thenReturn(ProviderWebhookEvent.builder()
                .providerEventId("evt_failed")
                .eventType("payment.failed")
                .providerCheckoutId("cs_test_123")
                .providerPaymentId("pay_failed")
                .paymentStatus(PaymentStatus.FAILED)
                .failureCode("card_declined")
                .failureMessage("Card was declined")
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, "evt_failed"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYMONGO, "pay_failed"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderCheckoutId(PaymentProvider.PAYMONGO, "cs_test_123"))
                .thenReturn(Optional.of(payment));

        WebhookResponseDto response = paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of());

        assertThat(response.getProcessingStatus()).isEqualTo(PaymentEventProcessingStatus.PROCESSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("card_declined");
        assertThat(payment.getFailureMessage()).isEqualTo("Card was declined");
        assertThat(payment.getProviderPaymentId()).isEqualTo("pay_failed");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getRequestType()).isEqualTo(PaymentAttemptRequestType.PROCESS_WEBHOOK);
        assertThat(attemptCaptor.getValue().getProviderReferenceId()).isEqualTo("evt_failed");
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
    }

    @Test
    void webhookFindsPaymentByProviderPaymentIdBeforeCheckoutId() {
        Payment paymentByProviderPaymentId = Payment.builder()
                .id(1L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .providerPaymentId("pay_123")
                .providerCheckoutId("cs_current")
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();
        Payment paymentByCheckoutId = Payment.builder()
                .id(2L)
                .order(order)
                .customer(user)
                .provider(PaymentProvider.PAYMONGO)
                .providerCheckoutId("cs_test_123")
                .amountMinor(4998L)
                .currency("PHP")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentGatewayRegistry.getGateway(PaymentProvider.PAYMONGO)).thenReturn(paymentGateway);
        when(paymentGateway.verifyWebhook("{}", Map.of())).thenReturn(WebhookVerificationResult.valid());
        when(paymentGateway.parseWebhook("{}")).thenReturn(ProviderWebhookEvent.builder()
                .providerEventId("evt_1")
                .eventType("payment.paid")
                .providerPaymentId("pay_123")
                .providerCheckoutId("cs_test_123")
                .paymentStatus(PaymentStatus.PAID)
                .build());
        when(paymentEventRepository.findByProviderAndProviderEventId(PaymentProvider.PAYMONGO, "evt_1"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYMONGO, "pay_123"))
                .thenReturn(Optional.of(paymentByProviderPaymentId));

        paymentService.processWebhook(PaymentProvider.PAYMONGO, "{}", Map.of());

        assertThat(paymentByProviderPaymentId.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentByProviderPaymentId.getProviderCheckoutId()).isEqualTo("cs_test_123");
        assertThat(paymentByCheckoutId.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).findByProviderAndProviderCheckoutId(PaymentProvider.PAYMONGO, "cs_test_123");
    }
}
