package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.inventory_api.dto.PricedOrderItem;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
import com.jomariabejo.connectly_api.orders_api.dto.CreateOrderDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderFilterDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderListItemDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderResponseDto;
import com.jomariabejo.connectly_api.orders_api.dto.PaginatedResponse;
import com.jomariabejo.connectly_api.orders_api.dto.UpdateOrderStatusDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.orders_api.exception.InvalidFilterException;
import com.jomariabejo.connectly_api.orders_api.exception.OrderNotFoundException;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderRepository;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "createdDate", "updatedDate", "totalAmount", "status"
    ));
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final OrderStatusService orderStatusService;
    private final InventoryService inventoryService;
    private final TenantContextService tenantContextService;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
                       OrderItemService orderItemService, OrderStatusService orderStatusService,
                       InventoryService inventoryService,
                       TenantContextService tenantContextService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderItemService = orderItemService;
        this.orderStatusService = orderStatusService;
        this.inventoryService = inventoryService;
        this.tenantContextService = tenantContextService;
    }

    @Transactional
    public OrderResponseDto createOrder(CreateOrderDto createOrderDto, User authenticatedUser) {
        // Validate input
        if (createOrderDto.getItems() == null || createOrderDto.getItems().isEmpty()) {
            throw new InvalidFilterException("Order must contain at least one item");
        }

        PricedOrderItem pricedOrder = inventoryService.priceOrderItems(createOrderDto.getItems());

        Tenant tenant = tenantContextService.requireTenant();

        Order order = Order.builder()
                .tenant(tenant)
                .customer(authenticatedUser)
                .totalAmount(pricedOrder.getTotalAmount())
                .status(OrderStatus.PENDING)
                .marketplaceSource(createOrderDto.getMarketplaceSource())
                .build();

        Order savedOrder = orderRepository.save(order);

        inventoryService.reserveOrderItems(savedOrder.getId(), pricedOrder.getItems());
        orderItemService.createOrderItems(savedOrder, pricedOrder.getItems());

        // Record initial status
        orderStatusService.recordStatusChange(savedOrder, null, OrderStatus.PENDING, authenticatedUser);

        // Fetch with details using eager loading
        Order orderWithDetails = orderRepository.findByIdWithDetails(savedOrder.getId())
                .orElseThrow(() -> new OrderNotFoundException(savedOrder.getId()));

        return orderMapper.toResponseDto(orderWithDetails);
    }

    @Cacheable(value = "orders", key = "#id", unless = "#result == null")
    public OrderResponseDto getOrderById(Long id) {
        Long tenantId = tenantContextService.requireTenantId();
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getTenant().getId().equals(tenantId)) {
            throw new OrderNotFoundException(id);
        }
        return orderMapper.toResponseDto(order);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OrderListItemDto> getAllOrders(OrderFilterDto filterDto, User authenticatedUser) {
        // Validate filter params
        validateFilterDto(filterDto);

        // Enforce max page size
        Integer pageSize = Math.min(filterDto.getPageSize(), MAX_PAGE_SIZE);

        // Build Sort
        Sort sort = buildSort(filterDto.getSortBy(), filterDto.getSortOrder());
        Pageable pageable = PageRequest.of(filterDto.getPage(), pageSize, sort);

        Long tenantId = tenantContextService.requireTenantId();
        TenantRole role = TenantContext.getTenantRole();

        // Build Specification for dynamic filtering
        Specification<Order> spec = Specification.where(OrderSpecification.withTenantId(tenantId));

        if (filterDto.getCustomerId() != null) {
            spec = spec.and(OrderSpecification.withCustomerId(filterDto.getCustomerId()));
        } else if (role == null || role == TenantRole.CUSTOMER || role == TenantRole.EMPLOYEE) {
            spec = spec.and(OrderSpecification.withCustomerId(authenticatedUser.getId()));
        }

        if (filterDto.getStatus() != null) {
            spec = spec.and(OrderSpecification.withStatus(filterDto.getStatus()));
        }

        if (filterDto.getMarketplaceSource() != null) {
            spec = spec.and(OrderSpecification.withMarketplaceSource(filterDto.getMarketplaceSource()));
        }

        if (filterDto.getDateFrom() != null && filterDto.getDateTo() != null) {
            spec = spec.and(OrderSpecification.withDateRange(filterDto.getDateFrom(), filterDto.getDateTo()));
        }

        // Fetch paginated results using lightweight DTO
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        Page<OrderListItemDto> dtoPage = orderPage.map(orderMapper::toListItemDto);

        // Build response
        return buildPaginatedResponse(dtoPage);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#id", beforeInvocation = false)
    public OrderResponseDto updateOrderStatus(Long id, UpdateOrderStatusDto updateDto, User authenticatedUser) {
        Long tenantId = tenantContextService.requireTenantId();
        Order order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Validate status transition
        if (!orderStatusService.validateStatusTransition(order.getStatus(), updateDto.getNewStatus())) {
            throw new InvalidFilterException("Invalid status transition from " + order.getStatus() + " to " + updateDto.getNewStatus());
        }

        // Store old status
        OrderStatus oldStatus = order.getStatus();

        // Update order status
        order.setStatus(updateDto.getNewStatus());
        Order updatedOrder = orderRepository.save(order);

        if (updateDto.getNewStatus() == OrderStatus.CANCELLED) {
            inventoryService.releaseReservationsForOrder(order.getId(), "Order cancelled");
        }

        // Record status change in audit trail
        orderStatusService.recordStatusChange(updatedOrder, oldStatus, updateDto.getNewStatus(), authenticatedUser);

        // Fetch with details
        Order orderWithDetails = orderRepository.findByIdWithDetails(updatedOrder.getId())
                .orElseThrow(() -> new OrderNotFoundException(id));

        return orderMapper.toResponseDto(orderWithDetails);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto> getStatusHistory(Long orderId, int page, int size) {
        Long tenantId = tenantContextService.requireTenantId();
        orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Enforce max page size
        Integer pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "changedAt"));

        Page<com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto> historyPage = orderStatusService.getStatusHistory(orderId, pageable);

        return buildPaginatedResponse(historyPage);
    }

    // ============== Helper Methods ==============

    private void validateFilterDto(OrderFilterDto filterDto) {
        // Validate page
        if (filterDto.getPage() < 0) {
            throw new InvalidFilterException("Page must be >= 0");
        }

        // Validate sort field
        if (!ALLOWED_SORT_FIELDS.contains(filterDto.getSortBy())) {
            throw new InvalidFilterException("Invalid sort field: " + filterDto.getSortBy() + 
                    ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        // Validate sort order
        if (!filterDto.getSortOrder().equalsIgnoreCase("ASC") && !filterDto.getSortOrder().equalsIgnoreCase("DESC")) {
            throw new InvalidFilterException("Sort order must be 'ASC' or 'DESC'");
        }

        // Validate page size
        if (filterDto.getPageSize() < 1 || filterDto.getPageSize() > MAX_PAGE_SIZE) {
            throw new InvalidFilterException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortBy);
    }

    private <T> PaginatedResponse<T> buildPaginatedResponse(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .data(page.getContent())
                .pagination(PaginatedResponse.PaginationMetadata.builder()
                        .page(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .hasNext(page.hasNext())
                        .hasPrev(page.hasPrevious())
                        .build())
                .metadata(PaginatedResponse.ResponseMetadata.builder()
                        .timestamp(LocalDateTime.now())
                        .apiVersion("v1")
                        .build())
                .build();
    }
}
