package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderStatusHistoryMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderStatusServiceTest {
    private final OrderStatusHistoryRepository orderStatusHistoryRepository = mock(OrderStatusHistoryRepository.class);
    private final OrderStatusHistoryMapper orderStatusHistoryMapper = mock(OrderStatusHistoryMapper.class);
    private final OrderStatusService orderStatusService = new OrderStatusService(
            orderStatusHistoryRepository,
            orderStatusHistoryMapper
    );

    @Test
    void validateStatusTransitionRejectsSameStatus() {
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PENDING, OrderStatus.PENDING)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PROCESSING, OrderStatus.PROCESSING)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.SHIPPED, OrderStatus.SHIPPED)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.DELIVERED, OrderStatus.DELIVERED)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.CANCELLED, OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void validateStatusTransitionRejectsTerminalStatusChanges() {
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.DELIVERED, OrderStatus.PROCESSING)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.CANCELLED, OrderStatus.PENDING)).isFalse();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.CANCELLED, OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    void validateStatusTransitionAllowsConfiguredTransitions() {
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PENDING, OrderStatus.PROCESSING)).isTrue();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PENDING, OrderStatus.CANCELLED)).isTrue();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED)).isTrue();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isTrue();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).isTrue();
        assertThat(orderStatusService.validateStatusTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED)).isTrue();
    }
}
