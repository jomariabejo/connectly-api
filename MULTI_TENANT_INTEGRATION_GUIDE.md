# Multi-Tenant Integration Guide

## Quick Start: Converting Existing Endpoints

### Step 1: Add Tenant Context to Service Methods

**Before (Single-tenant):**
```java
@Service
public class InventoryService {
    public List<InventoryItem> getAllItems() {
        return repository.findAll();
    }
}
```

**After (Multi-tenant):**
```java
@Service
public class InventoryService {
    private final TenantContextService tenantContextService;
    
    public List<InventoryItem> getAllItems() {
        Long tenantId = tenantContextService.requireTenantId();
        return repository.findByTenantId(tenantId);
    }
}
```

### Step 2: Update Repository Queries

**Before:**
```java
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findAll();
}
```

**After:**
```java
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId ORDER BY i.createdAt DESC")
    List<InventoryItem> findByTenantId(@Param("tenantId") Long tenantId);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId AND i.sku = :sku")
    Optional<InventoryItem> findByTenantIdAndSku(@Param("tenantId") Long tenantId, @Param("sku") String sku);
}
```

### Step 3: Validate Feature Access in Controllers

**Before:**
```java
@RestController
public class InventoryController {
    @GetMapping("/items")
    public List<InventoryItem> getItems() {
        return service.getAllItems();
    }
}
```

**After:**
```java
@RestController
public class InventoryController {
    private final InventoryService service;
    private final PricingService pricingService;
    
    @GetMapping("/items")
    public List<InventoryItem> getItems() {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Validate feature access
        pricingService.requireFeatureAccess(tenantId, FeatureAccess.BASIC_INVENTORY);
        
        return service.getAllItems();
    }
}
```

### Step 4: Log Audit Events

```java
@RestController
public class InventoryController {
    private final AuditLoggingService auditService;
    
    @PostMapping("/items")
    public InventoryItem createItem(@RequestBody CreateItemRequest request, HttpServletRequest httpRequest) {
        Long tenantId = tenantContextService.requireTenantId();
        Long userId = getCurrentUserId();
        
        InventoryItem item = service.createItem(request);
        
        // Log audit event
        auditService.logEvent(
            tenantId,
            userId,
            TenantAuditLog.ACTION_CREATE,
            "InventoryItem",
            item.getId(),
            null,
            itemToMap(item),
            AuditLoggingService.getClientIpAddress(httpRequest),
            AuditLoggingService.getUserAgent(httpRequest)
        );
        
        return item;
    }
}
```

## Pattern: Tenant-Aware Repositories

### Using Spring Data Query Annotations

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find all orders for a tenant
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
    List<Order> findAllForTenant(@Param("tenantId") Long tenantId, Pageable pageable);
    
    // Find orders by status for a tenant
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.status = :status")
    List<Order> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
    
    // Check ownership before delete
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o WHERE o.id = :orderId AND o.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);
}
```

## Pattern: Service Layer with Tenant Validation

```java
@Service
@Transactional
public class OrderService {
    private final OrderRepository repository;
    private final TenantContextService tenantContextService;
    private final PricingService pricingService;
    
    public Order createOrder(CreateOrderRequest request) {
        // Always get current tenant
        Long tenantId = tenantContextService.requireTenantId();
        
        // Validate feature access
        pricingService.requireFeatureAccess(tenantId, "ORDER_MANAGEMENT");
        
        // Create order with tenant isolation
        Order order = Order.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .total(request.getTotal())
                .build();
        
        return repository.save(order);
    }
    
    public Order getOrder(Long orderId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        return repository.findById(orderId)
                .filter(order -> order.getTenantId().equals(tenantId))
                .orElseThrow(() -> new TenantAccessDeniedException("Order not found or access denied"));
    }
    
    public void deleteOrder(Long orderId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Verify ownership before delete
        if (!repository.existsByIdAndTenantId(orderId, tenantId)) {
            throw new TenantAccessDeniedException("Cannot delete order from another tenant");
        }
        
        repository.deleteById(orderId);
    }
}
```

## Pattern: REST Controller with Tenant Context

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    
    @GetMapping
    public ResponseEntity<List<OrderDto>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Long tenantId = tenantContextService.requireTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        List<Order> orders = service.findAllForTenant(tenantId, pageable);
        return ResponseEntity.ok(orders.stream().map(this::toDto).toList());
    }
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        Long tenantId = tenantContextService.requireTenantId();
        Long userId = getCurrentUserId();
        
        Order order = service.createOrder(request);
        
        // Log audit event
        auditService.logEvent(
            tenantId,
            userId,
            TenantAuditLog.ACTION_CREATE,
            "Order",
            order.getId(),
            null,
            orderToMap(order),
            AuditLoggingService.getClientIpAddress(httpRequest),
            AuditLoggingService.getUserAgent(httpRequest)
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(order));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long tenantId = tenantContextService.requireTenantId();
        Order order = service.getOrder(id);
        
        // Log access
        auditService.logEvent(
            tenantId,
            getCurrentUserId(),
            TenantAuditLog.ACTION_SETTINGS_CHANGE, // or custom action
            "Order",
            id,
            null,
            null,
            AuditLoggingService.getClientIpAddress(httpRequest),
            AuditLoggingService.getUserAgent(httpRequest)
        );
        
        return ResponseEntity.ok(toDto(order));
    }
}
```

## Error Handling: Tenant Access Denied

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Access Denied: " + ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
    }
    
    @ExceptionHandler(ProductNotSubscribedException.class)
    public ResponseEntity<ErrorResponse> handleProductNotSubscribed(ProductNotSubscribedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ErrorResponse.builder()
                    .status(HttpStatus.PAYMENT_REQUIRED.value())
                    .message("Feature not available: " + ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
    }
}
```

## Testing: Multi-Tenant Test Cases

```java
@SpringBootTest
public class OrderServiceMultiTenantTest {
    
    @Autowired
    private OrderService service;
    
    @Autowired
    private TenantContextService tenantContextService;
    
    private Tenant tenant1;
    private Tenant tenant2;
    private User user1;
    private User user2;
    
    @BeforeEach
    public void setup() {
        // Create two separate tenants
        tenant1 = tenantRepository.save(Tenant.builder()
                .name("Tenant 1")
                .slug("tenant-1")
                .subdomain("tenant-1")
                .build());
        
        tenant2 = tenantRepository.save(Tenant.builder()
                .name("Tenant 2")
                .slug("tenant-2")
                .subdomain("tenant-2")
                .build());
    }
    
    @Test
    public void testOrderIsolationBetweenTenants() {
        // Create order as tenant1
        TenantContext.set(tenant1.getId(), TenantRole.OWNER, new HashSet<>());
        Order order1 = service.createOrder(new CreateOrderRequest(...));
        TenantContext.clear();
        
        // Try to access as tenant2
        TenantContext.set(tenant2.getId(), TenantRole.OWNER, new HashSet<>());
        assertThrows(TenantAccessDeniedException.class, () -> service.getOrder(order1.getId()));
        TenantContext.clear();
    }
    
    @Test
    public void testFeatureAccessControl() {
        // Tenant on STARTER plan shouldn't access ADVANCED_ANALYTICS
        TenantContext.set(tenant1.getId(), TenantRole.OWNER, Set.of(ProductCode.ORDER_MANAGEMENT));
        
        assertThrows(ProductNotSubscribedException.class, 
            () -> pricingService.requireFeatureAccess(tenant1.getId(), "ADVANCED_ANALYTICS"));
        
        TenantContext.clear();
    }
}
```

## Configuration: Application Properties

```yaml
# application.properties
tenant.base-domain=yourplatform.ph
tenant.subdomain-enabled=true
tenant.trial-days-starter=14
tenant.trial-days-others=30

# JWT Configuration
jwt.secret-key=${JWT_SECRET}
security.jwt.expiration=86400000

# Database
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL95Dialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

## Deployment: Subdomain Configuration

### Nginx Configuration Example
```nginx
server {
    listen 80;
    server_name ~^(?<subdomain>.+)\.yourplatform\.ph$;
    
    location / {
        proxy_pass http://connectly-api-backend;
        proxy_set_header X-Subdomain $subdomain;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### DNS Configuration
```
*.yourplatform.ph    A    192.168.1.1
yourplatform.ph      A    192.168.1.1
```

## Checklist: Multi-Tenant Integration

- [ ] All repositories have tenant_id filters
- [ ] All services use TenantContextService to get tenantId
- [ ] All controllers validate feature access with PricingService
- [ ] Audit events logged for sensitive operations
- [ ] Tests verify data isolation between tenants
- [ ] Error handlers for TenantAccessDeniedException
- [ ] Documentation updated with tenant context
- [ ] Database migrations applied successfully
- [ ] JWT tokens include tenant claims
- [ ] Subdomain resolution configured in load balancer
- [ ] RLS policies created for PostgreSQL (optional, for extra security)

## Common Pitfalls to Avoid

1. ❌ Forgetting to filter by tenant_id in queries
   ✅ Always use tenantContextService.requireTenantId()

2. ❌ Allowing bulk operations without tenant checks
   ✅ Validate tenant ownership before delete/update

3. ❌ Storing tenant context in class fields
   ✅ Always use TenantContext for thread-local storage

4. ❌ Leaking tenant info in error messages
   ✅ Log details server-side, show generic messages to client

5. ❌ Skipping audit logs for compliance
   ✅ Log all admin actions and data exports

6. ❌ Hardcoding feature availability
   ✅ Check feature_access table at runtime

7. ❌ Caching without tenant awareness
   ✅ Include tenant_id in cache keys
