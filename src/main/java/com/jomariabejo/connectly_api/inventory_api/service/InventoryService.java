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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryReservationRepository reservationRepository,
                            InventoryMovementRepository movementRepository,
                            InventoryMapper inventoryMapper) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.movementRepository = movementRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getActiveInventory() {
        return inventoryItemRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(inventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryItemDto getActiveInventoryItem(String sku) {
        InventoryItem item = inventoryItemRepository.findBySkuAndActiveTrue(normalizeSku(sku))
                .orElseThrow(() -> new InventoryNotFoundException(normalizeSku(sku)));
        return inventoryMapper.toDto(item);
    }

    @Transactional
    public InventoryItemDto createInventoryItem(CreateInventoryItemRequest request) {
        String sku = normalizeSku(request.getSku());
        if (inventoryItemRepository.existsBySku(sku)) {
            throw new InvalidInventoryRequestException("Inventory item with SKU " + sku + " already exists");
        }
        InventoryItem item = InventoryItem.builder()
                .sku(sku)
                .name(requireText(request.getName(), "Name is required"))
                .description(request.getDescription())
                .unitPrice(requireNonNegative(request.getUnitPrice(), "Unit price must be non-negative"))
                .currency(normalizeCurrency(request.getCurrency()))
                .onHandQuantity(requireNonNegative(request.getOnHandQuantity(), "On-hand quantity must be non-negative"))
                .reservedQuantity(0)
                .active(request.getActive() == null || request.getActive())
                .build();
        InventoryItem saved = inventoryItemRepository.save(item);
        recordMovement(saved.getSku(), InventoryMovementType.ADJUSTMENT, saved.getOnHandQuantity(), null, null, "Initial inventory");
        return inventoryMapper.toDto(saved);
    }

    @Transactional
    public InventoryItemDto updateInventoryItem(String sku, UpdateInventoryItemRequest request) {
        InventoryItem item = findItemForUpdate(sku);
        if (request.getName() != null) {
            item.setName(requireText(request.getName(), "Name is required"));
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getUnitPrice() != null) {
            item.setUnitPrice(requireNonNegative(request.getUnitPrice(), "Unit price must be non-negative"));
        }
        if (request.getCurrency() != null) {
            item.setCurrency(normalizeCurrency(request.getCurrency()));
        }
        if (request.getOnHandQuantity() != null) {
            int newOnHand = requireNonNegative(request.getOnHandQuantity(), "On-hand quantity must be non-negative");
            if (newOnHand < item.getReservedQuantity()) {
                throw new InvalidInventoryRequestException("On-hand quantity cannot be less than reserved quantity");
            }
            int delta = newOnHand - item.getOnHandQuantity();
            item.setOnHandQuantity(newOnHand);
            if (delta != 0) {
                recordMovement(item.getSku(), InventoryMovementType.ADJUSTMENT, delta, null, null, "Inventory count updated");
            }
        }
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }
        return inventoryMapper.toDto(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemDto adjustInventory(String sku, AdjustInventoryRequest request) {
        InventoryItem item = findItemForUpdate(sku);
        int delta = request.getQuantityDelta() == null ? 0 : request.getQuantityDelta();
        int newOnHand = item.getOnHandQuantity() + delta;
        if (newOnHand < item.getReservedQuantity()) {
            throw new InvalidInventoryRequestException("Adjustment would make on-hand quantity less than reserved quantity");
        }
        item.setOnHandQuantity(newOnHand);
        InventoryItem saved = inventoryItemRepository.save(item);
        recordMovement(saved.getSku(), InventoryMovementType.ADJUSTMENT, delta, null, null, request.getReason());
        return inventoryMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public PricedOrderItem priceOrderItems(List<OrderItemDto> itemDtos) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            throw new InvalidInventoryRequestException("Order must contain at least one item");
        }
        List<OrderItemDto> pricedItems = itemDtos.stream()
                .map(this::priceOrderItem)
                .collect(Collectors.toList());
        BigDecimal total = pricedItems.stream()
                .map(OrderItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PricedOrderItem(pricedItems, total);
    }

    @Transactional
    public void reserveOrderItems(Long orderId, List<OrderItemDto> itemDtos) {
        if (reservationRepository.findByOrderId(orderId).stream()
                .anyMatch(reservation -> reservation.getStatus() == InventoryReservationStatus.RESERVED)) {
            return;
        }

        for (OrderItemDto itemDto : itemDtos) {
            String sku = normalizeSku(itemDto.getSku());
            int quantity = requirePositive(itemDto.getQuantity(), "Quantity must be greater than 0");
            InventoryItem item = findItemForUpdate(sku);
            ensureActive(item);
            int available = item.getAvailableQuantity();
            if (available < quantity) {
                throw new InsufficientInventoryException(sku, quantity, available);
            }
            item.setReservedQuantity(item.getReservedQuantity() + quantity);
            inventoryItemRepository.save(item);

            InventoryReservation reservation = reservationRepository.findByOrderIdAndSku(orderId, sku)
                    .orElseGet(() -> InventoryReservation.builder()
                            .orderId(orderId)
                            .sku(sku)
                            .quantity(quantity)
                            .build());
            reservation.setQuantity(quantity);
            reservation.setStatus(InventoryReservationStatus.RESERVED);
            reservation.setReleaseReason(null);
            reservationRepository.save(reservation);
            recordMovement(sku, InventoryMovementType.RESERVE, quantity, orderId, null, "Order reservation");
        }
    }

    @Transactional
    public void commitReservationsForOrder(Long orderId, Long paymentId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, InventoryReservationStatus.RESERVED);
        for (InventoryReservation reservation : reservations) {
            InventoryItem item = findItemForUpdate(reservation.getSku());
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getQuantity());
            item.setOnHandQuantity(item.getOnHandQuantity() - reservation.getQuantity());
            inventoryItemRepository.save(item);
            reservation.setStatus(InventoryReservationStatus.COMMITTED);
            reservationRepository.save(reservation);
            recordMovement(reservation.getSku(), InventoryMovementType.COMMIT, reservation.getQuantity(), orderId, paymentId, "Payment committed");
        }
    }

    @Transactional
    public void releaseReservationsForOrder(Long orderId, String reason) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, InventoryReservationStatus.RESERVED);
        for (InventoryReservation reservation : reservations) {
            InventoryItem item = findItemForUpdate(reservation.getSku());
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getQuantity());
            inventoryItemRepository.save(item);
            reservation.setStatus(InventoryReservationStatus.RELEASED);
            reservation.setReleaseReason(reason);
            reservationRepository.save(reservation);
            recordMovement(reservation.getSku(), InventoryMovementType.RELEASE, reservation.getQuantity(), orderId, null, reason);
        }
    }

    private OrderItemDto priceOrderItem(OrderItemDto dto) {
        String sku = normalizeSku(dto.getSku());
        int quantity = requirePositive(dto.getQuantity(), "Quantity must be greater than 0");
        InventoryItem item = inventoryItemRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new InventoryNotFoundException(sku));
        return OrderItemDto.builder()
                .sku(sku)
                .productName(item.getName())
                .quantity(quantity)
                .price(item.getUnitPrice())
                .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private InventoryItem findItemForUpdate(String sku) {
        String normalizedSku = normalizeSku(sku);
        return inventoryItemRepository.findWithLockBySku(normalizedSku)
                .orElseThrow(() -> new InventoryNotFoundException(normalizedSku));
    }

    private void ensureActive(InventoryItem item) {
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new InvalidInventoryRequestException("Inventory item with SKU " + item.getSku() + " is inactive");
        }
    }

    private void recordMovement(String sku, InventoryMovementType type, Integer quantity, Long orderId, Long paymentId, String reason) {
        movementRepository.save(InventoryMovement.builder()
                .sku(sku)
                .movementType(type)
                .quantity(quantity)
                .orderId(orderId)
                .paymentId(paymentId)
                .reason(reason)
                .build());
    }

    private String normalizeSku(String sku) {
        return requireText(sku, "SKU is required").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        return requireText(currency, "Currency is required").trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidInventoryRequestException(message);
        }
        return value.trim();
    }

    private int requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new InvalidInventoryRequestException(message);
        }
        return value;
    }

    private int requireNonNegative(Integer value, String message) {
        if (value == null || value < 0) {
            throw new InvalidInventoryRequestException(message);
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInventoryRequestException(message);
        }
        return value;
    }
}
