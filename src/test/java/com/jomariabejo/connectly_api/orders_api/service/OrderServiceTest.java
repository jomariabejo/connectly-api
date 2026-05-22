package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.inventory_api.dto.PricedOrderItem;
import com.jomariabejo.connectly_api.inventory_api.exception.InsufficientInventoryException;
import com.jomariabejo.connectly_api.inventory_api.exception.InventoryNotFoundException;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
import com.jomariabejo.connectly_api.orders_api.dto.CreateOrderDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderFilterDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderItemDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderListItemDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderResponseDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto;
import com.jomariabejo.connectly_api.orders_api.dto.PaginatedResponse;
import com.jomariabejo.connectly_api.orders_api.dto.UpdateOrderStatusDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException;
import com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderRepository;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final OrderItemService orderItemService = mock(OrderItemService.class);
    private final OrderStatusService orderStatusService = mock(OrderStatusService.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final TenantContextService tenantContextService = mock(TenantContextService.class);
    private final OrderService orderService = new OrderService(
            orderRepository,
            orderMapper,
            orderItemService,
            orderStatusService,
            inventoryService,
            tenantContextService
    );

    private User user;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("maria");
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Default");
        tenant.setSlug("default");
        when(tenantContextService.requireTenantId()).thenReturn(1L);
        when(tenantContextService.requireTenant()).thenReturn(tenant);
    }

    @Test
    void createOrderRejectsNullItems() {
        CreateOrderDto request = CreateOrderDto.builder()
                .customerId(10L)
                .items(null)
                .totalAmount(new BigDecimal("49.98"))
                .marketplaceSource("amazon")
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Order must contain at least one item");

        verifyNoInteractions(orderRepository, orderItemService, orderStatusService, orderMapper, inventoryService);
    }

    @Test
    void createOrderRejectsEmptyItems() {
        CreateOrderDto request = CreateOrderDto.builder()
                .customerId(10L)
                .items(Collections.emptyList())
                .totalAmount(new BigDecimal("49.98"))
                .marketplaceSource("amazon")
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Order must contain at least one item");

        verifyNoInteractions(orderRepository, orderItemService, orderStatusService, orderMapper, inventoryService);
    }

    @Test
    void createOrderPersistsPendingOrderAndRecordsInitialStatus() {
        CreateOrderDto request = validCreateOrder();
        Order savedOrder = orderFor(user);
        PricedOrderItem pricedOrder = pricedOrder();
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(99L)
                .customerId(10L)
                .status(OrderStatus.PENDING)
                .build();

        when(inventoryService.priceOrderItems(request.getItems())).thenReturn(pricedOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(savedOrder));
        when(orderMapper.toResponseDto(savedOrder)).thenReturn(responseDto);

        OrderResponseDto response = orderService.createOrder(request, user);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order persistedOrder = orderCaptor.getValue();
        assertThat(persistedOrder.getCustomer()).isSameAs(user);
        assertThat(persistedOrder.getTotalAmount()).isEqualByComparingTo("49.98");
        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(persistedOrder.getMarketplaceSource()).isEqualTo("amazon");
        verify(inventoryService).reserveOrderItems(99L, pricedOrder.getItems());
        verify(orderItemService).createOrderItems(savedOrder, pricedOrder.getItems());
        verify(orderStatusService).recordStatusChange(savedOrder, null, OrderStatus.PENDING, user);
        assertThat(response).isSameAs(responseDto);
    }

    @Test
    void createOrderIgnoresClientTotalAndUsesInventoryPricing() {
        CreateOrderDto request = validCreateOrder();
        request.setTotalAmount(new BigDecimal("0.01"));
        Order savedOrder = orderFor(user);
        PricedOrderItem pricedOrder = pricedOrder();
        when(inventoryService.priceOrderItems(request.getItems())).thenReturn(pricedOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(savedOrder));
        when(orderMapper.toResponseDto(savedOrder)).thenReturn(OrderResponseDto.builder().id(99L).build());

        orderService.createOrder(request, user);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo("49.98");
    }

    @Test
    void createOrderPropagatesMissingInventoryItemBeforeSaving() {
        CreateOrderDto request = validCreateOrder();
        when(inventoryService.priceOrderItems(request.getItems())).thenThrow(new InventoryNotFoundException("NOTEBOOK-1"));

        assertThatThrownBy(() -> orderService.createOrder(request, user))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("NOTEBOOK-1");

        verifyNoInteractions(orderRepository, orderItemService, orderStatusService, orderMapper);
    }

    @Test
    void createOrderRollsBackWhenReservationFails() {
        CreateOrderDto request = validCreateOrder();
        Order savedOrder = orderFor(user);
        PricedOrderItem pricedOrder = pricedOrder();
        when(inventoryService.priceOrderItems(request.getItems())).thenReturn(pricedOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        org.mockito.Mockito.doThrow(new InsufficientInventoryException("NOTEBOOK-1", 1, 0))
                .when(inventoryService).reserveOrderItems(99L, pricedOrder.getItems());

        assertThatThrownBy(() -> orderService.createOrder(request, user))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("NOTEBOOK-1");

        verify(orderRepository).save(any(Order.class));
        verify(orderItemService, never()).createOrderItems(any(Order.class), any());
        verifyNoInteractions(orderStatusService, orderMapper);
    }

    @Test
    void createOrderThrowsWhenSavedOrderCannotBeFetchedWithDetails() {
        CreateOrderDto request = validCreateOrder();
        Order savedOrder = orderFor(user);
        PricedOrderItem pricedOrder = pricedOrder();
        when(inventoryService.priceOrderItems(request.getItems())).thenReturn(pricedOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request, user))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with ID 99 not found");

        verify(inventoryService).reserveOrderItems(99L, pricedOrder.getItems());
        verify(orderItemService).createOrderItems(savedOrder, pricedOrder.getItems());
        verify(orderStatusService).recordStatusChange(savedOrder, null, OrderStatus.PENDING, user);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getOrderByIdThrowsWhenOrderMissing() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with ID 99 not found");

        verifyNoInteractions(orderMapper, orderItemService, orderStatusService);
    }

    @Test
    void getOrderByIdMapsOrderWithDetails() {
        Order order = orderFor(user);
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(99L)
                .customerId(10L)
                .status(OrderStatus.PENDING)
                .build();
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto response = orderService.getOrderById(99L);

        assertThat(response).isSameAs(responseDto);
    }

    @Test
    void getAllOrdersRejectsNegativePage() {
        OrderFilterDto filter = validFilter();
        filter.setPage(-1);

        assertThatThrownBy(() -> orderService.getAllOrders(filter, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Page must be >= 0");

        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    void getAllOrdersRejectsInvalidSortField() {
        OrderFilterDto filter = validFilter();
        filter.setSortBy("customer.password");

        assertThatThrownBy(() -> orderService.getAllOrders(filter, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Invalid sort field: customer.password");

        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    void getAllOrdersRejectsInvalidSortOrder() {
        OrderFilterDto filter = validFilter();
        filter.setSortOrder("SIDEWAYS");

        assertThatThrownBy(() -> orderService.getAllOrders(filter, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Sort order must be 'ASC' or 'DESC'");

        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    void getAllOrdersRejectsPageSizeBelowOne() {
        OrderFilterDto filter = validFilter();
        filter.setPageSize(0);

        assertThatThrownBy(() -> orderService.getAllOrders(filter, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Page size must be between 1 and 100");

        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    void getAllOrdersRejectsPageSizeAboveMaximum() {
        OrderFilterDto filter = validFilter();
        filter.setPageSize(101);

        assertThatThrownBy(() -> orderService.getAllOrders(filter, user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Page size must be between 1 and 100");

        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    void getAllOrdersUsesAuthenticatedUserWhenCustomerIdMissing() {
        Order order = orderFor(user);
        OrderListItemDto itemDto = OrderListItemDto.builder()
                .id(99L)
                .customerId(10L)
                .customerName("maria")
                .totalAmount(new BigDecimal("49.98"))
                .status(OrderStatus.PENDING)
                .marketplaceSource("amazon")
                .itemCount(1)
                .build();
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toListItemDto(order)).thenReturn(itemDto);

        PaginatedResponse<OrderListItemDto> response = orderService.getAllOrders(validFilter(), user);

        CapturedOrderQuery query = captureOrderQuery();
        assertThat(query.pageable.getPageNumber()).isZero();
        assertThat(query.pageable.getPageSize()).isEqualTo(20);
        assertThat(query.pageable.getSort().getOrderFor("createdDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.getData()).containsExactly(itemDto);
        assertSpecificationUsesCustomerId(query.specification, 10L);
    }

    @Test
    void getAllOrdersHonorsExplicitCustomerIdAndSortOptions() {
        User otherUser = new User();
        otherUser.setId(77L);

        OrderFilterDto filter = validFilter();
        filter.setCustomerId(77L);
        filter.setSortBy("totalAmount");
        filter.setSortOrder("ASC");

        Order order = orderFor(otherUser);
        OrderListItemDto itemDto = OrderListItemDto.builder()
                .id(100L)
                .customerId(77L)
                .customerName("seller")
                .totalAmount(new BigDecimal("125.00"))
                .status(OrderStatus.PENDING)
                .marketplaceSource("shopify")
                .itemCount(1)
                .build();
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toListItemDto(order)).thenReturn(itemDto);

        PaginatedResponse<OrderListItemDto> response = orderService.getAllOrders(filter, user);

        CapturedOrderQuery query = captureOrderQuery();
        assertThat(query.pageable.getSort().getOrderFor("totalAmount").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(response.getData()).containsExactly(itemDto);
        assertSpecificationUsesCustomerId(query.specification, 77L);
    }

    @Test
    void getAllOrdersAcceptsLowercaseSortOrder() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        OrderFilterDto filter = validFilter();
        filter.setSortOrder("asc");

        PaginatedResponse<OrderListItemDto> response = orderService.getAllOrders(filter, user);

        CapturedOrderQuery query = captureOrderQuery();
        assertThat(query.pageable.getSort().getOrderFor("createdDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(response.getData()).isEmpty();
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getAllOrdersAppliesStatusMarketplaceAndDateRangeFilters() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        OrderFilterDto filter = validFilter();
        filter.setStatus(OrderStatus.PROCESSING);
        filter.setMarketplaceSource("etsy");
        filter.setDateFrom(LocalDate.of(2026, 5, 1));
        filter.setDateTo(LocalDate.of(2026, 5, 16));

        orderService.getAllOrders(filter, user);

        CapturedOrderQuery query = captureOrderQuery();
        assertSpecificationUsesOptionalFilters(query.specification);
    }

    @Test
    void updateOrderStatusThrowsWhenOrderMissing() {
        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, UpdateOrderStatusDto.builder()
                .newStatus(OrderStatus.PROCESSING)
                .build(), user))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with ID 99 not found");

        verifyNoInteractions(orderStatusService, orderMapper, orderItemService);
    }

    @Test
    void updateOrderStatusRejectsInvalidTransitionWithoutSaving() {
        Order order = orderFor(user);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.of(order));
        when(orderStatusService.validateStatusTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED))
                .thenReturn(false);

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, UpdateOrderStatusDto.builder()
                .newStatus(OrderStatus.CANCELLED)
                .build(), user))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Invalid status transition from DELIVERED to CANCELLED");

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(orderMapper, orderItemService);
    }

    @Test
    void updateOrderStatusRecordsHistoryForValidTransition() {
        Order order = orderFor(user);
        order.setStatus(OrderStatus.PENDING);
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(99L)
                .customerId(10L)
                .status(OrderStatus.PROCESSING)
                .build();

        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.of(order));
        when(orderStatusService.validateStatusTransition(OrderStatus.PENDING, OrderStatus.PROCESSING))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto response = orderService.updateOrderStatus(99L, UpdateOrderStatusDto.builder()
                .newStatus(OrderStatus.PROCESSING)
                .build(), user);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderStatusService).recordStatusChange(order, OrderStatus.PENDING, OrderStatus.PROCESSING, user);
        verify(inventoryService, never()).releaseReservationsForOrder(any(), any());
        assertThat(response).isSameAs(responseDto);
    }

    @Test
    void updateOrderStatusReleasesInventoryWhenCancelled() {
        Order order = orderFor(user);
        order.setStatus(OrderStatus.PENDING);
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(99L)
                .customerId(10L)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.of(order));
        when(orderStatusService.validateStatusTransition(OrderStatus.PENDING, OrderStatus.CANCELLED))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto response = orderService.updateOrderStatus(99L, UpdateOrderStatusDto.builder()
                .newStatus(OrderStatus.CANCELLED)
                .build(), user);

        verify(inventoryService).releaseReservationsForOrder(99L, "Order cancelled");
        verify(orderStatusService).recordStatusChange(order, OrderStatus.PENDING, OrderStatus.CANCELLED, user);
        assertThat(response).isSameAs(responseDto);
    }

    @Test
    void getStatusHistoryThrowsWhenOrderMissing() {
        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getStatusHistory(99L, 0, 50))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with ID 99 not found");

        verifyNoInteractions(orderStatusService, orderMapper, orderItemService);
    }

    @Test
    void getStatusHistoryLimitsPageSizeToMaximumAndSortsDescending() {
        Order order = orderFor(user);
        OrderStatusHistoryDto historyDto = OrderStatusHistoryDto.builder()
                .id(1L)
                .oldStatus(OrderStatus.PENDING)
                .newStatus(OrderStatus.PROCESSING)
                .changedById(10L)
                .changedByUsername("maria")
                .build();
        when(orderRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.of(order));
        when(orderStatusService.getStatusHistory(eq(99L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(historyDto)));

        PaginatedResponse<OrderStatusHistoryDto> response = orderService.getStatusHistory(99L, 2, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderStatusService).getStatusHistory(eq(99L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("changedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.getData()).containsExactly(historyDto);
    }

    private OrderFilterDto validFilter() {
        return OrderFilterDto.builder()
                .page(0)
                .pageSize(20)
                .sortBy("createdDate")
                .sortOrder("DESC")
                .build();
    }

    private CreateOrderDto validCreateOrder() {
        return CreateOrderDto.builder()
                .customerId(10L)
                .items(List.of(OrderItemDto.builder()
                        .sku("NOTEBOOK-1")
                        .productName("Notebook")
                        .quantity(1)
                        .price(new BigDecimal("49.98"))
                        .subtotal(new BigDecimal("49.98"))
                        .build()))
                .totalAmount(new BigDecimal("49.98"))
                .marketplaceSource("amazon")
                .build();
    }

    private PricedOrderItem pricedOrder() {
        return new PricedOrderItem(List.of(OrderItemDto.builder()
                .sku("NOTEBOOK-1")
                .productName("Notebook")
                .quantity(1)
                .price(new BigDecimal("49.98"))
                .subtotal(new BigDecimal("49.98"))
                .build()), new BigDecimal("49.98"));
    }

    private Order orderFor(User customer) {
        return Order.builder()
                .id(99L)
                .tenant(tenant)
                .customer(customer)
                .totalAmount(new BigDecimal("49.98"))
                .status(OrderStatus.PENDING)
                .marketplaceSource("amazon")
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CapturedOrderQuery captureOrderQuery() {
        ArgumentCaptor<Specification<Order>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        return new CapturedOrderQuery(specificationCaptor.getValue(), pageableCaptor.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertSpecificationUsesCustomerId(Specification<Order> specification, Long expectedCustomerId) {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> tenantPath = mock(Path.class);
        Path<Object> tenantIdPath = mock(Path.class);
        Path<Object> customerPath = mock(Path.class);
        Path<Object> customerIdPath = mock(Path.class);
        Predicate tenantPredicate = mock(Predicate.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("tenant")).thenReturn((Path) tenantPath);
        when(tenantPath.get("id")).thenReturn((Path) tenantIdPath);
        when(criteriaBuilder.equal(tenantIdPath, 1L)).thenReturn(tenantPredicate);
        when(root.get("customer")).thenReturn((Path) customerPath);
        when(customerPath.get("id")).thenReturn((Path) customerIdPath);
        when(criteriaBuilder.equal(customerIdPath, expectedCustomerId)).thenReturn(predicate);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);

        assertThat(specification.toPredicate(root, criteriaQuery, criteriaBuilder)).isSameAs(predicate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertSpecificationUsesOptionalFilters(Specification<Order> specification) {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> tenantPath = mock(Path.class);
        Path<Object> tenantIdPath = mock(Path.class);
        Path<Object> customerPath = mock(Path.class);
        Path<Object> customerIdPath = mock(Path.class);
        Path<OrderStatus> statusPath = mock(Path.class);
        Path<String> marketplacePath = mock(Path.class);
        Path<LocalDateTime> createdDatePath = mock(Path.class);
        Predicate tenantPredicate = mock(Predicate.class);
        Predicate customerPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate marketplacePredicate = mock(Predicate.class);
        Predicate dateRangePredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("tenant")).thenReturn((Path) tenantPath);
        when(tenantPath.get("id")).thenReturn((Path) tenantIdPath);
        when(criteriaBuilder.equal(tenantIdPath, 1L)).thenReturn(tenantPredicate);
        when(root.get("customer")).thenReturn((Path) customerPath);
        when(customerPath.get("id")).thenReturn((Path) customerIdPath);
        when(root.get("status")).thenReturn((Path) statusPath);
        when(root.get("marketplaceSource")).thenReturn((Path) marketplacePath);
        when(root.get("createdDate")).thenReturn((Path) createdDatePath);
        when(criteriaBuilder.equal(customerIdPath, 10L)).thenReturn(customerPredicate);
        when(criteriaBuilder.equal(statusPath, OrderStatus.PROCESSING)).thenReturn(statusPredicate);
        when(criteriaBuilder.equal(marketplacePath, "etsy")).thenReturn(marketplacePredicate);
        when(criteriaBuilder.between(
                (Expression) createdDatePath,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 16, 23, 59, 59)
        )).thenReturn(dateRangePredicate);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(combinedPredicate);

        assertThat(specification.toPredicate(root, criteriaQuery, criteriaBuilder)).isNotNull();
        verify(criteriaBuilder).equal(tenantIdPath, 1L);
        verify(criteriaBuilder).equal(customerIdPath, 10L);
        verify(criteriaBuilder).equal(statusPath, OrderStatus.PROCESSING);
        verify(criteriaBuilder).equal(marketplacePath, "etsy");
        verify(criteriaBuilder).between(
                (Expression) createdDatePath,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 16, 23, 59, 59)
        );
    }

    private record CapturedOrderQuery(Specification<Order> specification, Pageable pageable) {
    }
}
