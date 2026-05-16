package com.jomariabejo.connectly_api.payments_api.repository;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
}
