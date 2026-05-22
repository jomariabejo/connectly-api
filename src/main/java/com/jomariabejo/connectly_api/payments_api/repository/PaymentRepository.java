package com.jomariabejo.connectly_api.payments_api.repository;

import com.jomariabejo.connectly_api.payments_api.entity.Payment;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByCreatedDateDesc(Long orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);

    Optional<Payment> findByProviderAndProviderCheckoutId(PaymentProvider provider, String providerCheckoutId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
