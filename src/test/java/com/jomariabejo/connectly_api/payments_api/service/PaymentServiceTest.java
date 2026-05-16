package com.jomariabejo.connectly_api.payments_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.repository.OrderRepository;
import com.jomariabejo.connectly_api.payments_api.dto.CreateCheckoutRequest;
import com.jomariabejo.connectly_api.payments_api.dto.PaymentResponseDto;
import com.jomariabejo.connectly_api.payments_api.dto.WebhookResponseDto;
import com.jomariabejo.connectly_api.payments_api.entity.Payment;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentEvent;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentEventProcessingStatus;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentIntentType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentWebhookVerificationException;
import com.jomariabejo.connectly_api.payments_api.mapper.PaymentMapper;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutCommand;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutResult;
import com.jomariabejo.connectly_api.payments_api.provider.PaymentGateway;
import com.jomariabejo.connectly_api.payments_api.provider.PaymentGatewayRegistry;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderPaymentStatus;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderWebhookEvent;
import com.jomariabejo.connectly_api.payments_api.provider.WebhookVerificationResult;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentAttemptRepository;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentEventRepository;
import com.jomariabejo.connectly_api.payments_api.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);

        order = Order.builder()
                .id(99L)
                .customer(user)
                .totalAmount(new BigDecimal("49.98"))
                .marketplaceSource("amazon")
                .build();
    }

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
}
