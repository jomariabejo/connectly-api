package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatusHistory;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderStatusHistoryMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderStatusService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;

    // Define valid status transitions
    private static final Map<OrderStatus, OrderStatus[]> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(OrderStatus.PENDING, new OrderStatus[]{OrderStatus.PROCESSING, OrderStatus.CANCELLED});
        VALID_TRANSITIONS.put(OrderStatus.PROCESSING, new OrderStatus[]{OrderStatus.SHIPPED, OrderStatus.CANCELLED});
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED, new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.CANCELLED});
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, new OrderStatus[]{});
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, new OrderStatus[]{});
    }

    public OrderStatusService(OrderStatusHistoryRepository orderStatusHistoryRepository, OrderStatusHistoryMapper orderStatusHistoryMapper) {
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
    }

    public boolean validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return false; // No transition if status is the same
        }

        OrderStatus[] validNextStatuses = VALID_TRANSITIONS.get(currentStatus);
        if (validNextStatuses == null) {
            return false;
        }

        for (OrderStatus validStatus : validNextStatuses) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void recordStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus, User changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    public Page<OrderStatusHistoryDto> getStatusHistory(Long orderId, Pageable pageable) {
        Page<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId, pageable);
        return history.map(orderStatusHistoryMapper::toDto);
    }

    public long getStatusHistoryCount(Long orderId) {
        return orderStatusHistoryRepository.countByOrderId(orderId);
    }
}
