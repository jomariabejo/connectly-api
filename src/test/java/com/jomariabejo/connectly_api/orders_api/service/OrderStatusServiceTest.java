package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatusHistory;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderStatusHistoryMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void recordStatusChangePersistsHistoryWithActorAndStatuses() {
        User user = new User();
        user.setId(10L);
        Order order = Order.builder()
                .id(99L)
                .customer(user)
                .totalAmount(new BigDecimal("49.98"))
                .status(OrderStatus.PROCESSING)
                .marketplaceSource("amazon")
                .build();

        orderStatusService.recordStatusChange(order, OrderStatus.PENDING, OrderStatus.PROCESSING, user);

        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        OrderStatusHistory history = historyCaptor.getValue();
        assertThat(history.getOrder()).isSameAs(order);
        assertThat(history.getOldStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.getNewStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(history.getChangedBy()).isSameAs(user);
    }

    @Test
    void recordStatusChangeAllowsNullOldStatusForInitialPendingState() {
        User user = new User();
        Order order = Order.builder()
                .id(99L)
                .customer(user)
                .totalAmount(new BigDecimal("49.98"))
                .status(OrderStatus.PENDING)
                .marketplaceSource("amazon")
                .build();

        orderStatusService.recordStatusChange(order, null, OrderStatus.PENDING, user);

        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getOldStatus()).isNull();
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getStatusHistoryMapsRepositoryPage() {
        Pageable pageable = PageRequest.of(1, 5);
        User user = new User();
        OrderStatusHistory history = OrderStatusHistory.builder()
                .id(1L)
                .oldStatus(OrderStatus.PENDING)
                .newStatus(OrderStatus.PROCESSING)
                .changedBy(user)
                .build();
        OrderStatusHistoryDto historyDto = OrderStatusHistoryDto.builder()
                .id(1L)
                .oldStatus(OrderStatus.PENDING)
                .newStatus(OrderStatus.PROCESSING)
                .build();
        when(orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(99L, pageable))
                .thenReturn(new PageImpl<>(List.of(history), pageable, 1));
        when(orderStatusHistoryMapper.toDto(history)).thenReturn(historyDto);

        Page<OrderStatusHistoryDto> response = orderStatusService.getStatusHistory(99L, pageable);

        assertThat(response.getContent()).containsExactly(historyDto);
        assertThat(response.getNumber()).isEqualTo(1);
        verify(orderStatusHistoryMapper).toDto(history);
    }

    @Test
    void getStatusHistoryCountDelegatesToRepository() {
        when(orderStatusHistoryRepository.countByOrderId(99L)).thenReturn(3L);

        long count = orderStatusService.getStatusHistoryCount(99L);

        assertThat(count).isEqualTo(3L);
        verify(orderStatusHistoryRepository).countByOrderId(99L);
    }
}
