# Multi-Tenant SaaS Architecture Implementation Guide

## Overview

The Connectly API has been enhanced with a comprehensive multi-tenant SaaS architecture designed specifically for Philippine SMEs. This implementation supports three distinct business segments:

- **Sari-Sari Stores**: Inventory and credit tracking for neighborhood convenience stores
- **Vulcanizing Shops**: Service and parts inventory management
- **Catering Businesses**: Event and menu management with costing

## Architecture Components

### 1. Tenant Identification & Resolution

#### Subdomain-Based Tenancy (Primary Method)
Tenants are identified via subdomain routing:
```
juans-sari-sari.yourplatform.ph
marias-catering.yourplatform.ph
quick-vulcanizing.yourplatform.ph
```

#### Tenant Resolution Flow
1. Extract subdomain from HTTP request
2. Validate against tenants table
3. Set TenantContext for ThreadLocal storage
4. Apply RLS policies at database level

**Implementation File**: `SubdomainExtractor.java`

### 2. Database Schema

#### Core Tenant Tables

**tenants**: Master tenant record
- `id`: Primary key
- `name`: Tenant business name
- `slug`: URL-friendly identifier
- `subdomain`: Unique subdomain (indexed for fast lookup)
- `business_type`: ENUM (SARI_SARI_STORE, VULCANIZING_SHOP, CATERING_BUSINESS, GENERAL)
- `pricing_tier`: ENUM (STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE)
- `trial_ends_at`: Trial expiration date
- `subscription_active`: Boolean flag for subscription status
- `status`: ENUM (ACTIVE, INACTIVE, SUSPENDED)

**tenant_settings**: Configuration per tenant
- Timezone, currency, language preferences
- SMS/email notification settings
- Business type specific configuration (JSONB)

**tenant_users**: User-tenant membership
- Links users to tenants with roles
- Supports OWNER, ADMIN, STAFF roles

**pricing_plans**: Available tiers for Philippine market
```
STARTER  ₱499/month    (1 location, 5 users, 1GB storage)
PROFESSIONAL ₱1,499/month (3 locations, unlimited users, 10GB storage)
BUSINESS ₱3,999/month  (unlimited locations, unlimited users, 50GB storage)
ENTERPRISE Custom pricing
```

**feature_access**: Tier-based feature control
- Determines which features are available per pricing tier
- Examples: API_ACCESS, CREDIT_TRACKING, ADVANCED_ANALYTICS

**usage_tracking**: Monthly resource consumption
- API calls, storage usage, SMS sent
- Used for quota enforcement and billing

**tenant_audit_log**: Security and compliance tracking
- Logs all significant tenant actions
- IP address, user agent, before/after values
- Supports compliance reporting

**business_type_templates**: Industry-specific configurations
- Default settings per business type
- Optional sample data for onboarding

### 3. Authentication & JWT

#### Enhanced JWT Claims
```json
{
  "sub": "user@example.com",
  "tenant_id": 123,
  "tenant_slug": "juans-sari-sari",
  "tenant_role": "OWNER",
  "exp": 1234567890
}
```

#### Token Generation
```java
jwtService.generateTokenWithTenant(
    userDetails,
    tenantId,
    tenantSlug,
    tenantRole
);
```

**Implementation Files**:
- `JwtService.java`: Methods to extract and include tenant claims
- `TenantFilter.java`: Multi-stage tenant resolution

### 4. Tenant Provisioning

#### Automated Onboarding Workflow
1. **Subdomain Assignment**: Unique subdomain generated and validated
2. **Business Type Setup**: Industry-specific configuration applied
3. **Pricing Tier Setup**: Trial period configured (14-30 days)
4. **Feature Subscription**: Core features enabled based on tier
5. **Owner Assignment**: Creator set as OWNER role
6. **Settings Initialization**: Timezone, language, notifications configured

**Implementation File**: `TenantProvisioningService.java`

#### Example Usage
```java
CreateTenantRequest request = CreateTenantRequest.builder()
    .tenantName("Juan's Sari-Sari Store")
    .subdomain("juans-sari-sari")
    .businessType(BusinessType.SARI_SARI_STORE)
    .pricingTier(PricingTier.STARTER)
    .build();

Tenant tenant = provisioningService.provisionNewTenant(creator, request);
```

### 5. Pricing & Feature Management

#### Tier-Based Feature Access
```java
// Check if feature is available
pricingService.hasFeatureAccess(tenantId, "API_ACCESS");

// Require feature or throw exception
pricingService.requireFeatureAccess(tenantId, "CREDIT_TRACKING");

// Get all enabled features
Set<String> features = pricingService.getEnabledFeatures(tenantId);
```

#### Quota Enforcement
```java
// Storage quota
pricingService.isWithinStorageQuota(tenantId, requiredGb);

// User limit
pricingService.canAddMoreUsers(tenantId, currentUsers);

// Location limit
pricingService.canAddMoreLocations(tenantId, currentLocations);
```

**Implementation File**: `PricingService.java`

### 6. Data Isolation

#### Application-Level Isolation
- TenantContext provides thread-local tenant storage
- All repositories implicitly filter by tenant_id
- Middleware validates tenant_id matches request context

#### Database-Level Isolation (PostgreSQL RLS)
```sql
-- Example RLS policy
CREATE POLICY tenant_isolation ON orders
  USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

Implementation recommended for Phase 4:
- Create RLS policies per table
- Set application.current_tenant before queries
- Enforce at database level for defense-in-depth

### 7. Audit Logging & Compliance

#### Event Types
- `LOGIN`: User authentication
- `FAILED_LOGIN`: Authentication failure
- `CREATE`, `UPDATE`, `DELETE`: Data changes
- `INVITE_USER`, `REMOVE_USER`: Team management
- `CHANGE_PLAN`: Subscription changes
- `SETTINGS_CHANGE`: Configuration updates
- `DATA_EXPORT`: Compliance exports

#### Usage
```java
auditService.logLogin(tenantId, userId, ipAddress, userAgent);
auditService.logSettingsChange(tenantId, userId, oldValues, newValues, ...);
auditService.logDataExport(tenantId, userId, exportType, ...);
```

**Implementation File**: `AuditLoggingService.java`

## Security Considerations

### Defense-in-Depth Strategy

1. **Application Layer**: TenantContext middleware validates tenant
2. **Database Layer**: PostgreSQL RLS enforces isolation
3. **Audit Layer**: All cross-tenant access attempts logged
4. **JWT Layer**: Tenant claims prevent token hijacking

### Key Security Practices

1. **Never hard-code tenant ID**: Always extract from request context
2. **Always validate tenant match**: JWT tenant_id must match subdomain
3. **Log failed access**: Audit suspicious access patterns
4. **Validate subdomains**: Prevent DNS enumeration attacks
5. **Use HTTPS only**: For subdomain-based routing

## Scaling Strategy

### Current Architecture
- Single PostgreSQL database
- Shared tables for Starter tier tenants
- Dedicated schemas for Professional/Business tiers (future)

### Scaling Path
1. **Phase 1 (Now)**: Shared database, row-level isolation
2. **Phase 2**: Schema-per-tenant for Professional tier
3. **Phase 3**: Database sharding by tenant_id
4. **Phase 4**: Read replicas for analytics

### Sharding Key Design
```
shard_id = tenant_id % num_shards

Example: 3 shards
- Shard 1: tenants 1, 4, 7, 10...
- Shard 2: tenants 2, 5, 8, 11...
- Shard 3: tenants 3, 6, 9, 12...
```

## Philippine Market Considerations

### Pricing Tiers
- **Starter**: For individual stores (₱499/month)
- **Professional**: For growing businesses (₱1,499/month)
- **Business**: For established operations (₱3,999/month)

### Add-On Revenue Streams
- SMS Credits: ₱0.50/SMS
- Premium Support: ₱500/month
- Custom Reports: ₱2,000 one-time
- Data Migration: ₱5,000 setup fee
- On-site Training: ₱3,000 per session

### Localization
- Default timezone: Asia/Manila (UTC+8)
- Default currency: PHP (Philippine Peso)
- Language support: Tagalog (future), English
- Payment methods: GCash, PayMaya (future integration)

## Integration Checklist

### Required Changes to Existing Code

1. **Authentication Controllers**: 
   - Update login to include tenant context in JWT
   - Generate tenant context on registration

2. **Existing Repositories**:
   - Add `@Where(clause = "tenant_id = :tenantId")` annotations
   - Ensure tenant_id filters in all queries

3. **API Endpoints**:
   - Add subdomain extraction to request processing
   - Validate tenant_id in JWT matches request context

4. **Database Initialization**:
   - Run V15 migration to add new tables
   - Populate business_type_templates
   - Initialize pricing_plans

### Testing Strategy

1. **Unit Tests**: Feature access, quota validation
2. **Integration Tests**: Tenant isolation, data leakage
3. **Security Tests**: Cross-tenant access attempts
4. **Performance Tests**: Subdomain resolution, RLS overhead

## Future Enhancements

1. **White-Labeling**: Custom branding per tenant
2. **Custom Domains**: Allow tenants to use custom domains
3. **Multi-Currency**: Support multiple currencies per tenant
4. **API Rate Limiting**: Per-tenant rate limiting
5. **Webhook Events**: Tenant-specific webhooks for integrations
6. **SSO Integration**: SAML/OAuth for enterprise tenants
7. **Backup & Recovery**: Per-tenant backup strategies

## Troubleshooting

### Issue: Tenant Context Not Set
**Solution**: Verify TenantFilter is in security chain and runs after JWT filter

### Issue: Subdomain Resolution Fails
**Solution**: Check Host header and DNS subdomain wildcard configuration

### Issue: Feature Access Denied
**Solution**: Verify feature_access records exist for pricing tier

### Issue: Data Leakage Between Tenants
**Solution**: Check that all repository queries include tenant_id filter

## References

- **Problem Statement**: Comprehensive SaaS architecture design for Philippine market
- **Architecture Pattern**: Hybrid multi-tenancy with row-level security
- **Industry Standard**: Similar to Notion, Slack, Jira implementations
