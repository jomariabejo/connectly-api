package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.repository.OrderRepository;
import com.jomariabejo.connectly_api.inventory_api.repository.InventoryItemRepository;
import com.jomariabejo.connectly_api.crm_api.service.CrmCustomerService;
import com.jomariabejo.connectly_api.ticketing_api.service.TicketService;
import com.jomariabejo.connectly_api.workforce_api.service.WorkforceService;
import com.jomariabejo.connectly_api.payroll_api.service.PayrollService;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.dto.*;
import com.jomariabejo.connectly_api.tenant_api.entity.*;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantSubscriptionRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantContextService tenantContextService;
    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CrmCustomerService crmCustomerService;
    private final TicketService ticketService;
    private final WorkforceService workforceService;
    private final PayrollService payrollService;

    public TenantService(
            TenantRepository tenantRepository,
            TenantUserRepository tenantUserRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantContextService tenantContextService,
            OrderRepository orderRepository,
            InventoryItemRepository inventoryItemRepository,
            CrmCustomerService crmCustomerService,
            TicketService ticketService,
            WorkforceService workforceService,
            PayrollService payrollService) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantContextService = tenantContextService;
        this.orderRepository = orderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.crmCustomerService = crmCustomerService;
        this.ticketService = ticketService;
        this.workforceService = workforceService;
        this.payrollService = payrollService;
    }

    @Transactional(readOnly = true)
    public List<TenantSummaryDto> getTenantsForUser(User user) {
        return tenantUserRepository.findByUserIdWithTenantAndSubscriptions(user.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponseDto getCurrentTenant() {
        Tenant tenant = tenantContextService.requireTenant();
        Set<ProductCode> products = subscriptionRepository.findByTenantIdAndActiveTrue(tenant.getId()).stream()
                .map(TenantSubscription::getProductCode)
                .collect(Collectors.toSet());
        return toResponse(tenant, products);
    }

    @Transactional
    public TenantResponseDto createTenant(CreateTenantRequest request, User owner) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Tenant slug already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .status(TenantStatus.ACTIVE)
                .build();

        TenantSettings settings = TenantSettings.builder()
                .tenant(tenant)
                .build();
        tenant.setSettings(settings);

        Set<ProductCode> products = request.getProducts() != null && !request.getProducts().isEmpty()
                ? request.getProducts()
                : EnumSet.of(ProductCode.ORDER_MANAGEMENT, ProductCode.INVENTORY_MANAGEMENT);

        for (ProductCode code : products) {
            TenantSubscription sub = TenantSubscription.builder()
                    .tenant(tenant)
                    .productCode(code)
                    .active(true)
                    .build();
            tenant.getSubscriptions().add(sub);
        }

        TenantUser membership = TenantUser.builder()
                .tenant(tenant)
                .user(owner)
                .tenantRole(TenantRole.OWNER)
                .active(true)
                .build();
        tenant.getMembers().add(membership);

        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved, products);
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto getAdminDashboard() {
        Long tenantId = tenantContextService.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        Set<ProductCode> products = TenantContext.getSubscribedProducts();
        Map<String, Long> metrics = new LinkedHashMap<>();

        if (products.contains(ProductCode.ORDER_MANAGEMENT)) {
            metrics.put("totalOrders", orderRepository.countByTenantId(tenantId));
            metrics.put("pendingOrders", orderRepository.countByTenantIdAndStatus(tenantId,
                    com.jomariabejo.connectly_api.orders_api.entity.OrderStatus.PENDING));
        }
        if (products.contains(ProductCode.INVENTORY_MANAGEMENT)) {
            metrics.put("inventoryItems", inventoryItemRepository.countByTenantIdAndActiveTrue(tenantId));
        }
        if (products.contains(ProductCode.CRM)) {
            metrics.put("crmCustomers", crmCustomerService.countCustomers(tenantId));
        }
        if (products.contains(ProductCode.TICKETING)) {
            metrics.put("totalTickets", ticketService.countTickets(tenantId));
            metrics.put("openTickets", ticketService.countOpenTickets(tenantId));
        }
        if (products.contains(ProductCode.WORKFORCE)) {
            metrics.put("schedules", workforceService.countSchedules(tenantId));
            metrics.put("pendingLeave", workforceService.countPendingLeave(tenantId));
        }
        if (products.contains(ProductCode.PAYROLL)) {
            metrics.put("payrollEmployees", payrollService.countEmployees(tenantId));
            metrics.put("payrollRuns", payrollService.countPayrollRuns(tenantId));
        }

        return AdminDashboardDto.builder()
                .tenantId(tenantId)
                .tenantName(tenant.getName())
                .subscribedProducts(products)
                .metrics(metrics)
                .build();
    }

    private TenantSummaryDto toSummary(TenantUser membership) {
        Tenant tenant = membership.getTenant();
        Set<ProductCode> products = tenant.getSubscriptions().stream()
                .filter(TenantSubscription::getActive)
                .map(TenantSubscription::getProductCode)
                .collect(Collectors.toSet());
        return TenantSummaryDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .role(membership.getTenantRole())
                .subscribedProducts(products)
                .build();
    }

    private TenantResponseDto toResponse(Tenant tenant, Set<ProductCode> products) {
        String timezone = tenant.getSettings() != null ? tenant.getSettings().getTimezone() : "UTC";
        String currency = tenant.getSettings() != null ? tenant.getSettings().getCurrency() : "USD";
        return TenantResponseDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .timezone(timezone)
                .currency(currency)
                .subscribedProducts(products)
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
