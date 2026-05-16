package com.jomariabejo.connectly_api.orders_api.repository;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    @Query("SELECT osh FROM OrderStatusHistory osh WHERE osh.order.id = :orderId ORDER BY osh.changedAt DESC")
    Page<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(@Param("orderId") Long orderId, Pageable pageable);

    long countByOrderId(Long orderId);
}
