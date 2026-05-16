package com.jomariabejo.connectly_api.orders_api.controller;

import com.jomariabejo.connectly_api.orders_api.dto.CreateOrderDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderFilterDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderListItemDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderResponseDto;
import com.jomariabejo.connectly_api.orders_api.dto.PaginatedResponse;
import com.jomariabejo.connectly_api.orders_api.dto.UpdateOrderStatusDto;
import com.jomariabejo.connectly_api.orders_api.service.OrderService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthenticationService authenticationService;

    public OrderController(OrderService orderService, AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.authenticationService = authenticationService;
    }

    /**
     * Create a new order
     * POST /v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody @Valid CreateOrderDto createOrderDto) {
        User currentUser = authenticationService.getAuthenticatedUser();
        OrderResponseDto responseDto = orderService.createOrder(createOrderDto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Get order by ID
     * GET /v1/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto responseDto = orderService.getOrderById(id);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Get all orders with filtering and pagination
     * GET /v1/orders?page=0&size=20&sort=createdDate&sortOrder=DESC&status=PENDING&marketplace=amazon&dateFrom=2026-01-01&dateTo=2026-05-16
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderListItemDto>> getAllOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long customerId) {

        User currentUser = authenticationService.getAuthenticatedUser();

        // Build filter DTO from request params
        OrderFilterDto filterDto = OrderFilterDto.builder()
                .page(page)
                .pageSize(size)
                .sortBy(sort)
                .sortOrder(sortOrder)
                .marketplaceSource(marketplace)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .customerId(customerId)
                .build();

        // Parse status enum if provided
        if (status != null && !status.isEmpty()) {
            try {
                filterDto.setStatus(com.jomariabejo.connectly_api.orders_api.entity.OrderStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        PaginatedResponse<OrderListItemDto> response = orderService.getAllOrders(filterDto, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update order status
     * PATCH /v1/orders/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrderStatusDto updateDto) {
        User currentUser = authenticationService.getAuthenticatedUser();
        OrderResponseDto responseDto = orderService.updateOrderStatus(id, updateDto, currentUser);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Get order status history (audit trail)
     * GET /v1/orders/{id}/status-history?page=0&size=50
     */
    @GetMapping("/{id}/status-history")
    public ResponseEntity<PaginatedResponse<com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto>> getStatusHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        PaginatedResponse<com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto> response = orderService.getStatusHistory(id, page, size);
        return ResponseEntity.ok(response);
    }
}
