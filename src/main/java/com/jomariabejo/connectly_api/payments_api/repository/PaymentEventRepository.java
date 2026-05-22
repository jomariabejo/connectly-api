package com.jomariabejo.connectly_api.payments_api.repository;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentEvent;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    Optional<PaymentEvent> findByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);

    boolean existsByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);
}
