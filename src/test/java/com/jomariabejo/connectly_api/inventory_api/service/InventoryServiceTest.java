package com.jomariabejo.connectly_api.inventory_api.service;

import com.jomariabejo.connectly_api.inventory_api.dto.AdjustInventoryRequest;
import com.jomariabejo.connectly_api.inventory_api.dto.CreateInventoryItemRequest;
import com.jomariabejo.connectly_api.inventory_api.dto.InventoryItemDto;
import com.jomariabejo.connectly_api.inventory_api.dto.PricedOrderItem;
import com.jomariabejo.connectly_api.inventory_api.dto.UpdateInventoryItemRequest;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryItem;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryMovement;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryMovementType;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryReservation;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryReservationStatus;
import com.jomariabejo.connectly_api.inventory_api.exception.InsufficientInventoryException;
import com.jomariabejo.connectly_api.inventory_api.exception.InvalidInventoryRequestException;
import com.jomariabejo.connectly_api.inventory_api.exception.InventoryNotFoundException;
import com.jomariabejo.connectly_api.inventory_api.mapper.InventoryMapper;
import com.jomariabejo.connectly_api.inventory_api.repository.InventoryItemRepository;
import com.jomariabejo.connectly_api.inventory_api.repository.InventoryMovementRepository;
import com.jomariabejo.connectly_api.inventory_api.repository.InventoryReservationRepository;
import com.jomariabejo.connectly_api.orders_api.dto.OrderItemDto;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InventoryServiceTest {
    private final InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
    private final InventoryReservationRepository reservationRepository = mock(InventoryReservationRepository.class);
    private final InventoryMovementRepository movementRepository = mock(InventoryMovementRepository.class);
    private final TenantContextService tenantContextService = mock(TenantContextService.class);
    private final InventoryService inventoryService = new InventoryService(
            inventoryItemRepository,
            reservationRepository,
            movementRepository,
            new InventoryMapper(),
            tenantContextService
    );

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        when(tenantContextService.requireTenantId()).thenReturn(1L);
        when(tenantContextService.requireTenant()).thenReturn(tenant);
    }

    @Test
    void createInventoryItemNormalizesFieldsAndRecordsInitialMovement() {
        when(inventoryItemRepository.existsByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(false);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemDto response = inventoryService.createInventoryItem(CreateInventoryItemRequest.builder()
                .sku(" notebook-1 ")
                .name("Notebook")
                .description("Ruled")
                .unitPrice(new BigDecimal("49.98"))
                .currency("php")
                .onHandQuantity(10)
                .build());

        assertThat(response.getSku()).isEqualTo("NOTEBOOK-1");
        assertThat(response.getCurrency()).isEqualTo("PHP");
        assertThat(response.getReservedQuantity()).isZero();
        assertThat(response.getAvailableQuantity()).isEqualTo(10);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(10);
    }

    @Test
    void createInventoryItemRejectsDuplicateSku() {
        when(inventoryItemRepository.existsByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventoryItem(CreateInventoryItemRequest.builder()
                .sku("NOTEBOOK-1")
                .name("Notebook")
                .unitPrice(new BigDecimal("49.98"))
                .currency("PHP")
                .onHandQuantity(10)
                .build()))
                .isInstanceOf(InvalidInventoryRequestException.class)
                .hasMessageContaining("already exists");

        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
        verifyNoInteractions(movementRepository);
    }

    @Test
    void getActiveInventoryOnlyReturnsActiveItems() {
        InventoryItem item = item("NOTEBOOK-1", 10, 2, true);
        when(inventoryItemRepository.findByTenantIdAndActiveTrueOrderByNameAsc(1L)).thenReturn(List.of(item));

        List<InventoryItemDto> response = inventoryService.getActiveInventory();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getSku()).isEqualTo("NOTEBOOK-1");
        assertThat(response.get(0).getAvailableQuantity()).isEqualTo(8);
    }

    @Test
    void getActiveInventoryItemThrowsWhenMissingOrInactive() {
        when(inventoryItemRepository.findByTenantIdAndSkuAndActiveTrue(1L, "NOTEBOOK-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getActiveInventoryItem("notebook-1"))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("NOTEBOOK-1");
    }

    @Test
    void updateInventoryItemRejectsOnHandBelowReservedQuantity() {
        InventoryItem item = item("NOTEBOOK-1", 10, 4, true);
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> inventoryService.updateInventoryItem("NOTEBOOK-1", UpdateInventoryItemRequest.builder()
                .onHandQuantity(3)
                .build()))
                .isInstanceOf(InvalidInventoryRequestException.class)
                .hasMessageContaining("cannot be less than reserved quantity");

        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void adjustInventoryUpdatesOnHandAndRecordsMovement() {
        InventoryItem item = item("NOTEBOOK-1", 10, 2, true);
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemDto response = inventoryService.adjustInventory("notebook-1", AdjustInventoryRequest.builder()
                .quantityDelta(5)
                .reason("Cycle count")
                .build());

        assertThat(response.getOnHandQuantity()).isEqualTo(15);
        assertThat(response.getAvailableQuantity()).isEqualTo(13);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(5);
        assertThat(movementCaptor.getValue().getReason()).isEqualTo("Cycle count");
    }

    @Test
    void priceOrderItemsUsesInventoryPriceAndComputesTotal() {
        when(inventoryItemRepository.findByTenantIdAndSkuAndActiveTrue(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item("NOTEBOOK-1", 10, 0, true)));

        PricedOrderItem pricedOrder = inventoryService.priceOrderItems(List.of(OrderItemDto.builder()
                .sku("notebook-1")
                .quantity(2)
                .price(new BigDecimal("0.01"))
                .subtotal(new BigDecimal("0.02"))
                .build()));

        assertThat(pricedOrder.getTotalAmount()).isEqualByComparingTo("99.96");
        assertThat(pricedOrder.getItems().get(0).getSku()).isEqualTo("NOTEBOOK-1");
        assertThat(pricedOrder.getItems().get(0).getProductName()).isEqualTo("Notebook");
        assertThat(pricedOrder.getItems().get(0).getPrice()).isEqualByComparingTo("49.98");
        assertThat(pricedOrder.getItems().get(0).getSubtotal()).isEqualByComparingTo("99.96");
    }

    @Test
    void reserveOrderItemsIncrementsReservedQuantityAndStoresReservation() {
        InventoryItem item = item("NOTEBOOK-1", 10, 2, true);
        when(reservationRepository.findByOrderId(99L)).thenReturn(List.of());
        when(reservationRepository.findByOrderIdAndSku(99L, "NOTEBOOK-1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item));

        inventoryService.reserveOrderItems(99L, List.of(OrderItemDto.builder()
                .sku("NOTEBOOK-1")
                .quantity(3)
                .build()));

        assertThat(item.getReservedQuantity()).isEqualTo(5);

        ArgumentCaptor<InventoryReservation> reservationCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationRepository).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().getOrderId()).isEqualTo(99L);
        assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovementType.RESERVE);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void reserveOrderItemsRejectsInactiveSkuAndInsufficientStock() {
        InventoryItem inactiveItem = item("NOTEBOOK-1", 10, 0, false);
        when(reservationRepository.findByOrderId(99L)).thenReturn(List.of());
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(inactiveItem));

        assertThatThrownBy(() -> inventoryService.reserveOrderItems(99L, List.of(OrderItemDto.builder()
                .sku("NOTEBOOK-1")
                .quantity(1)
                .build())))
                .isInstanceOf(InvalidInventoryRequestException.class)
                .hasMessageContaining("inactive");

        InventoryItem lowStockItem = item("PEN-1", 3, 2, true);
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "PEN-1")).thenReturn(Optional.of(lowStockItem));

        assertThatThrownBy(() -> inventoryService.reserveOrderItems(99L, List.of(OrderItemDto.builder()
                .sku("PEN-1")
                .quantity(2)
                .build())))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("available 1");
    }

    @Test
    void reserveOrderItemsIsIdempotentWhenOrderHasOpenReservation() {
        InventoryReservation reservation = InventoryReservation.builder()
                .orderId(99L)
                .sku("NOTEBOOK-1")
                .quantity(1)
                .status(InventoryReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByOrderId(99L)).thenReturn(List.of(reservation));

        inventoryService.reserveOrderItems(99L, List.of(OrderItemDto.builder()
                .sku("NOTEBOOK-1")
                .quantity(1)
                .build()));

        verifyNoInteractions(inventoryItemRepository, movementRepository);
    }

    @Test
    void commitReservationsDecrementsReservedAndOnHandOnce() {
        InventoryReservation reservation = InventoryReservation.builder()
                .orderId(99L)
                .sku("NOTEBOOK-1")
                .quantity(2)
                .status(InventoryReservationStatus.RESERVED)
                .build();
        InventoryItem item = item("NOTEBOOK-1", 10, 2, true);
        when(reservationRepository.findByOrderIdAndStatus(99L, InventoryReservationStatus.RESERVED))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of());
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item));

        inventoryService.commitReservationsForOrder(99L, 1L);
        inventoryService.commitReservationsForOrder(99L, 1L);

        assertThat(item.getOnHandQuantity()).isEqualTo(8);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.COMMITTED);
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void releaseReservationsDecrementsReservedWithoutChangingOnHand() {
        InventoryReservation reservation = InventoryReservation.builder()
                .orderId(99L)
                .sku("NOTEBOOK-1")
                .quantity(2)
                .status(InventoryReservationStatus.RESERVED)
                .build();
        InventoryItem item = item("NOTEBOOK-1", 10, 2, true);
        when(reservationRepository.findByOrderIdAndStatus(99L, InventoryReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryItemRepository.findWithLockByTenantIdAndSku(1L, "NOTEBOOK-1")).thenReturn(Optional.of(item));

        inventoryService.releaseReservationsForOrder(99L, "Payment status FAILED");

        assertThat(item.getOnHandQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(reservation.getReleaseReason()).isEqualTo("Payment status FAILED");
    }

    private InventoryItem item(String sku, int onHand, int reserved, boolean active) {
        return InventoryItem.builder()
                .sku(sku)
                .name("Notebook")
                .unitPrice(new BigDecimal("49.98"))
                .currency("PHP")
                .onHandQuantity(onHand)
                .reservedQuantity(reserved)
                .active(active)
                .build();
    }
}
