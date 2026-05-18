package com.jomariabejo.connectly_api.payments_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final PaymentMapper paymentMapper;
    private final InventoryService inventoryService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventRepository paymentEventRepository,
                          PaymentAttemptRepository paymentAttemptRepository,
                          OrderRepository orderRepository,
                          PaymentGatewayRegistry paymentGatewayRegistry,
                          PaymentMapper paymentMapper,
                          InventoryService inventoryService) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.orderRepository = orderRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.paymentMapper = paymentMapper;
        this.inventoryService = inventoryService;
    }

    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public PaymentResponseDto createCheckout(CreateCheckoutRequest request, User authenticatedUser) {
        validateCreateCheckoutRequest(request);

        String idempotencyKey = resolveIdempotencyKey(request);
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            ensurePaymentOwner(existing.get(), authenticatedUser);
            return paymentMapper.toDto(existing.get());
        }

        Order order = orderRepository.findByIdWithDetails(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));
        ensureOrderOwner(order, authenticatedUser);

        if (paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.PAID)) {
            throw new InvalidPaymentRequestException("Order already has a successful payment");
        }

        Payment payment = Payment.builder()
                .order(order)
                .customer(authenticatedUser)
                .provider(request.getProvider())
                .amountMinor(toMinorUnits(order.getTotalAmount()))
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .intentType(request.getIntentType() == null ? PaymentIntentType.CAPTURE_NOW : request.getIntentType())
                .idempotencyKey(idempotencyKey)
                .build();
        payment = paymentRepository.save(payment);

        PaymentGateway gateway = paymentGatewayRegistry.getGateway(request.getProvider());

        try {
            CreateCheckoutResult result = gateway.createCheckout(CreateCheckoutCommand.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .orderReference("connectly-order-" + order.getId())
                    .provider(request.getProvider())
                    .amountMinor(payment.getAmountMinor())
                    .currency(payment.getCurrency())
                    .successUrl(request.getSuccessUrl())
                    .cancelUrl(request.getCancelUrl())
                    .idempotencyKey(idempotencyKey)
                    .intentType(payment.getIntentType())
                    .build());

            payment.setProviderPaymentId(result.getProviderPaymentId());
            payment.setProviderCheckoutId(result.getProviderCheckoutId());
            payment.setCheckoutUrl(result.getCheckoutUrl());
            payment.setStatus(result.getStatus() == null ? PaymentStatus.PENDING : result.getStatus());
            payment = paymentRepository.save(payment);
            recordAttempt(payment, PaymentAttemptRequestType.CREATE_CHECKOUT, result.getProviderCheckoutId(), PaymentAttemptStatus.SUCCEEDED);
            return paymentMapper.toDto(payment);
        } catch (PaymentGatewayException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode("GATEWAY_ERROR");
            payment.setFailureMessage(ex.getMessage());
            paymentRepository.save(payment);
            recordAttempt(payment, PaymentAttemptRequestType.CREATE_CHECKOUT, null, PaymentAttemptStatus.FAILED);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(Long paymentId, User authenticatedUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ensurePaymentOwner(payment, authenticatedUser);
        return paymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getOrderPayments(Long orderId, User authenticatedUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        ensureOrderOwner(order, authenticatedUser);

        return paymentRepository.findByOrderIdOrderByCreatedDateDesc(orderId)
                .stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WebhookResponseDto processWebhook(PaymentProvider provider, String rawPayload, Map<String, String> headers) {
        PaymentGateway gateway = paymentGatewayRegistry.getGateway(provider);
        WebhookVerificationResult verification = gateway.verifyWebhook(rawPayload, headers);
        if (!verification.isValid()) {
            throw new PaymentWebhookVerificationException(verification.getReason());
        }

        ProviderWebhookEvent providerEvent = gateway.parseWebhook(rawPayload);
        String providerEventId = resolveProviderEventId(providerEvent, rawPayload);

        Optional<PaymentEvent> duplicate = paymentEventRepository.findByProviderAndProviderEventId(provider, providerEventId);
        if (duplicate.isPresent()) {
            return WebhookResponseDto.builder()
                    .provider(provider)
                    .providerEventId(providerEventId)
                    .eventType(duplicate.get().getEventType())
                    .processingStatus(PaymentEventProcessingStatus.DUPLICATE)
                    .build();
        }

        Payment payment = findPaymentForEvent(provider, providerEvent).orElse(null);
        PaymentEventProcessingStatus processingStatus = payment == null ? PaymentEventProcessingStatus.IGNORED : PaymentEventProcessingStatus.PROCESSED;

        PaymentEvent event = PaymentEvent.builder()
                .payment(payment)
                .provider(provider)
                .providerEventId(providerEventId)
                .eventType(providerEvent.getEventType() == null ? "unknown" : providerEvent.getEventType())
                .rawPayload(rawPayload)
                .receivedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .processingStatus(processingStatus)
                .build();
        paymentEventRepository.save(event);

        if (payment != null && providerEvent.getPaymentStatus() != null) {
            applyPaymentStatus(payment, providerEvent);
            recordAttempt(payment, PaymentAttemptRequestType.PROCESS_WEBHOOK, providerEventId, PaymentAttemptStatus.SUCCEEDED);
        }

        return WebhookResponseDto.builder()
                .provider(provider)
                .providerEventId(providerEventId)
                .eventType(event.getEventType())
                .processingStatus(processingStatus)
                .build();
    }

    private void validateCreateCheckoutRequest(CreateCheckoutRequest request) {
        if (request.getIntentType() == PaymentIntentType.AUTHORIZE_ONLY) {
            throw new InvalidPaymentRequestException("AUTHORIZE_ONLY is reserved for a future release");
        }
    }

    private String resolveIdempotencyKey(CreateCheckoutRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return request.getIdempotencyKey();
        }
        return "checkout-" + request.getOrderId() + "-" + request.getProvider() + "-" + UUID.randomUUID();
    }

    private Long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void ensureOrderOwner(Order order, User authenticatedUser) {
        if (order.getCustomer() == null || authenticatedUser == null ||
                !order.getCustomer().getId().equals(authenticatedUser.getId())) {
            throw new InvalidPaymentRequestException("You can only create or view payments for your own orders");
        }
    }

    private void ensurePaymentOwner(Payment payment, User authenticatedUser) {
        if (payment.getCustomer() == null || authenticatedUser == null ||
                !payment.getCustomer().getId().equals(authenticatedUser.getId())) {
            throw new InvalidPaymentRequestException("You can only access your own payments");
        }
    }

    private Optional<Payment> findPaymentForEvent(PaymentProvider provider, ProviderWebhookEvent providerEvent) {
        if (providerEvent.getProviderPaymentId() != null) {
            Optional<Payment> payment = paymentRepository.findByProviderAndProviderPaymentId(provider, providerEvent.getProviderPaymentId());
            if (payment.isPresent()) {
                return payment;
            }
        }
        if (providerEvent.getProviderCheckoutId() != null) {
            return paymentRepository.findByProviderAndProviderCheckoutId(provider, providerEvent.getProviderCheckoutId());
        }
        return Optional.empty();
    }

    private void applyPaymentStatus(Payment payment, ProviderWebhookEvent providerEvent) {
        payment.setStatus(providerEvent.getPaymentStatus());
        payment.setFailureCode(providerEvent.getFailureCode());
        payment.setFailureMessage(providerEvent.getFailureMessage());
        if (providerEvent.getProviderPaymentId() != null) {
            payment.setProviderPaymentId(providerEvent.getProviderPaymentId());
        }
        if (providerEvent.getProviderCheckoutId() != null) {
            payment.setProviderCheckoutId(providerEvent.getProviderCheckoutId());
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setPaymentStatus(providerEvent.getPaymentStatus());
        orderRepository.save(order);

        applyInventoryTransition(payment, providerEvent.getPaymentStatus());
    }

    private void applyInventoryTransition(Payment payment, PaymentStatus status) {
        if (status == PaymentStatus.PAID) {
            inventoryService.commitReservationsForOrder(payment.getOrder().getId(), payment.getId());
            return;
        }
        if (status == PaymentStatus.FAILED ||
                status == PaymentStatus.CANCELLED ||
                status == PaymentStatus.REFUNDED ||
                status == PaymentStatus.PARTIALLY_REFUNDED) {
            inventoryService.releaseReservationsForOrder(payment.getOrder().getId(), "Payment status " + status);
        }
    }

    private String resolveProviderEventId(ProviderWebhookEvent event, String rawPayload) {
        if (event.getProviderEventId() != null && !event.getProviderEventId().isBlank()) {
            return event.getProviderEventId();
        }
        return "payload-" + Integer.toHexString(rawPayload.hashCode());
    }

    private void recordAttempt(Payment payment,
                               PaymentAttemptRequestType requestType,
                               String providerReferenceId,
                               PaymentAttemptStatus status) {
        paymentAttemptRepository.save(PaymentAttempt.builder()
                .payment(payment)
                .provider(payment.getProvider())
                .requestType(requestType)
                .providerReferenceId(providerReferenceId)
                .status(status)
                .build());
    }
}
