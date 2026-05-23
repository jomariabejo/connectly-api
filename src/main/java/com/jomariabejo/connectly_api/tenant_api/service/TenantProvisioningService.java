package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.CreateTenantRequest;
import com.jomariabejo.connectly_api.tenant_api.entity.*;
import com.jomariabejo.connectly_api.tenant_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.util.SubdomainExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for provisioning new tenants with complete onboarding workflow
 * Handles tenant creation, configuration, and initialization based on business type and pricing tier
 */
@Service
@Transactional
public class TenantProvisioningService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final BusinessTypeTemplateRepository businessTypeTemplateRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final FeatureAccessRepository featureAccessRepository;

    public TenantProvisioningService(
            TenantRepository tenantRepository,
            TenantUserRepository tenantUserRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantSettingsRepository tenantSettingsRepository,
            BusinessTypeTemplateRepository businessTypeTemplateRepository,
            PricingPlanRepository pricingPlanRepository,
            FeatureAccessRepository featureAccessRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.businessTypeTemplateRepository = businessTypeTemplateRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.featureAccessRepository = featureAccessRepository;
    }

    /**
     * Provision a new tenant with complete setup
     * This method handles:
     * 1. Tenant creation with unique subdomain
     * 2. Tenant settings configuration based on business type
     * 3. Initial subscription setup
     * 4. Adding the creator as OWNER
     * 5. Setting up trial period if applicable
     */
    public Tenant provisionNewTenant(User creator, CreateTenantRequest request) {
        // Normalize and validate subdomain
        String normalizedSubdomain = SubdomainExtractor.normalizeSubdomain(request.getSubdomainOrSlug());
        if (!SubdomainExtractor.isValidSubdomain(normalizedSubdomain)) {
            throw new IllegalArgumentException("Invalid subdomain: " + normalizedSubdomain);
        }

        // Check subdomain availability
        if (tenantRepository.existsBySubdomain(normalizedSubdomain)) {
            throw new IllegalArgumentException("Subdomain already taken: " + normalizedSubdomain);
        }

        // Get business type (default to GENERAL if not specified)
        BusinessType businessType = request.getBusinessType() != null 
            ? request.getBusinessType() 
            : BusinessType.GENERAL;

        // Get pricing tier (default to STARTER if not specified)
        PricingTier pricingTier = request.getPricingTier() != null 
            ? request.getPricingTier() 
            : PricingTier.STARTER;

        // Determine trial end date (14-day trial for Starter, 30 days for others)
        LocalDateTime trialEndsAt = LocalDateTime.now().plusDays(
            pricingTier == PricingTier.STARTER ? 14 : 30
        );

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(normalizedSubdomain) // slug and subdomain can be the same
                .subdomain(normalizedSubdomain)
                .businessType(businessType)
                .pricingTier(pricingTier)
                .trialEndsAt(trialEndsAt)
                .subscriptionActive(true)
                .status(TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);

        // Create tenant settings with business-type specific defaults
        TenantSettings settings = createTenantSettings(tenant, businessType);
        tenantSettingsRepository.save(settings);
        tenant.setSettings(settings);

        // Set up subscription based on pricing tier
        setupInitialSubscriptions(tenant, pricingTier);

        // Add creator as OWNER
        TenantUser ownerMembership = TenantUser.builder()
                .tenant(tenant)
                .user(creator)
                .tenantRole(TenantRole.OWNER)
                .active(true)
                .build();
        tenantUserRepository.save(ownerMembership);

        // Load business type template and apply defaults if available
        loadAndApplyBusinessTypeDefaults(tenant, businessType);

        return tenant;
    }

    /**
     * Create tenant settings with business type defaults
     */
    private TenantSettings createTenantSettings(Tenant tenant, BusinessType businessType) {
        TenantSettings settings = new TenantSettings();
        settings.setTenant(tenant);
        settings.setTimezone("Asia/Manila"); // Philippine timezone by default
        settings.setCurrency("PHP");
        settings.setSmsNotificationsEnabled(true);
        settings.setEmailNotificationsEnabled(true);
        settings.setLanguage("en");

        // Load business-type specific settings if available
        Optional<BusinessTypeTemplate> template = businessTypeTemplateRepository
                .findByBusinessType(businessType.name());
        if (template.isPresent() && template.get().getDefaultSettings() != null) {
            settings.setBusinessTypeConfig(template.get().getDefaultSettings());
        }

        return settings;
    }

    /**
     * Set up initial subscriptions for the tenant
     * Subscribes to all features available in the pricing tier
     */
    private void setupInitialSubscriptions(Tenant tenant, PricingTier pricingTier) {
        // Subscribe to core ProductCode modules based on tier
        subscribeToProductCode(tenant, ProductCode.INVENTORY_MANAGEMENT);
        subscribeToProductCode(tenant, ProductCode.ORDER_MANAGEMENT);
        subscribeToProductCode(tenant, ProductCode.CRM);
        
        // Optional modules based on pricing tier
        if (pricingTier != PricingTier.STARTER) {
            subscribeToProductCode(tenant, ProductCode.PAYROLL);
            subscribeToProductCode(tenant, ProductCode.WORKFORCE);
        }
        
        // All tiers get ticketing
        subscribeToProductCode(tenant, ProductCode.TICKETING);
    }

    /**
     * Subscribe tenant to a product code
     */
    private void subscribeToProductCode(Tenant tenant, ProductCode productCode) {
        if (!subscriptionRepository.existsByTenantIdAndProductCodeAndActiveTrue(tenant.getId(), productCode)) {
            TenantSubscription subscription = TenantSubscription.builder()
                    .tenant(tenant)
                    .productCode(productCode)
                    .active(true)
                    .build();
            subscriptionRepository.save(subscription);
        }
    }

    /**
     * Load and apply business type specific defaults
     */
    private void loadAndApplyBusinessTypeDefaults(Tenant tenant, BusinessType businessType) {
        Optional<BusinessTypeTemplate> template = businessTypeTemplateRepository
                .findByBusinessType(businessType.name());

        if (template.isPresent()) {
            // In future, execute sample data SQL or apply other initialization
            // For now, template is loaded but not executed to avoid complexity
            // Could be enhanced with database initialization scripts per business type
        }
    }

    /**
     * Generate a unique subdomain suggestion based on tenant name
     */
    public String generateSubdomainSuggestion(String tenantName) {
        // Convert to lowercase and replace spaces/special chars with hyphens
        String suggestion = tenantName.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        // Ensure it's 3-50 characters
        if (suggestion.length() < 3) {
            suggestion = suggestion + "-app";
        } else if (suggestion.length() > 50) {
            suggestion = suggestion.substring(0, 50);
        }

        // If already taken, add number suffix (with maximum retry limit)
        String original = suggestion;
        int counter = 1;
        int maxRetries = 100;
        
        while (tenantRepository.existsBySubdomain(suggestion) && counter <= maxRetries) {
            String suffix = String.valueOf(counter);
            int maxLength = 50 - suffix.length();
            if (original.length() > maxLength) {
                suggestion = original.substring(0, maxLength) + suffix;
            } else {
                suggestion = original + suffix;
            }
            counter++;
        }

        if (counter > maxRetries) {
            throw new IllegalArgumentException("Unable to generate unique subdomain after " + maxRetries + " attempts");
        }

        return suggestion;
    }

    /**
     * Upgrade tenant to a different pricing tier
     */
    public void upgradeToTier(Tenant tenant, PricingTier newTier) {
        if (newTier.equals(tenant.getPricingTier())) {
            throw new IllegalArgumentException("Tenant is already on this tier");
        }

        // Update tenant
        tenant.setPricingTier(newTier);

        // Add new features available in the new tier
        if (newTier != PricingTier.STARTER) {
            subscribeToProductCode(tenant, ProductCode.PAYROLL);
            subscribeToProductCode(tenant, ProductCode.WORKFORCE);
        }

        tenantRepository.save(tenant);
    }
}

