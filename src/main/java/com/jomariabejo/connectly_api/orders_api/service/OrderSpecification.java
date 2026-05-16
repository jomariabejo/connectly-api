package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderSpecification {

    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> withMarketplaceSource(String marketplace) {
        return (root, query, cb) -> marketplace == null || marketplace.isEmpty()
                ? null
                : cb.equal(root.get("marketplaceSource"), marketplace);
    }

    public static Specification<Order> withCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
                ? null
                : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Order> withDateRange(LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            if (dateFrom == null || dateTo == null) {
                return null;
            }
            LocalDateTime startDateTime = dateFrom.atStartOfDay();
            LocalDateTime endDateTime = dateTo.atTime(23, 59, 59);
            return cb.between(root.get("createdDate"), startDateTime, endDateTime);
        };
    }

    public static Specification<Order> withCreatedDateFrom(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("createdDate"), dateFrom.atStartOfDay());
    }

    public static Specification<Order> withCreatedDateTo(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null
                ? null
                : cb.lessThanOrEqualTo(root.get("createdDate"), dateTo.atTime(23, 59, 59));
    }
}
