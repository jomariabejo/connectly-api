package com.jomariabejo.connectly_api.orders_api.repository;

import com.jomariabejo.connectly_api.orders_api.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.statusHistory WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdDate DESC")
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdDate DESC")
    Page<Order> findByStatus(@Param("status") com.jomariabejo.connectly_api.orders_api.entity.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.marketplaceSource = :marketplace ORDER BY o.createdDate DESC")
    Page<Order> findByMarketplaceSource(@Param("marketplace") String marketplace, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.createdDate BETWEEN :startDate AND :endDate ORDER BY o.createdDate DESC")
    Page<Order> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    long countByStatus(com.jomariabejo.connectly_api.orders_api.entity.OrderStatus status);

    long countByMarketplaceSource(String marketplace);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, com.jomariabejo.connectly_api.orders_api.entity.OrderStatus status);

    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);
}
