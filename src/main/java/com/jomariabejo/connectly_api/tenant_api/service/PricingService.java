package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.tenant_api.entity.*;
import com.jomariabejo.connectly_api.tenant_api.exception.ProductNotSubscribedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for managing pricing, subscriptions, and feature access
 * Handles tier-based feature validation and quota enforcement
 */
@Service
@Transactional(readOnly = true)
public class PricingService {
    private final TenantRepository tenantRepository;
    private final FeatureAccessRepository featureAccessRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    public PricingService(
            TenantRepository tenantRepository,
            FeatureAccessRepository featureAccessRepository,
            PricingPlanRepository pricingPlanRepository,
            UsageTrackingRepository usageTrackingRepository,
            TenantSubscriptionRepository subscriptionRepository) {
        this.tenantRepository = tenantRepository;
        this.featureAccessRepository = featureAccessRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.usageTrackingRepository = usageTrackingRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Check if tenant has access to a specific feature
     */
    public boolean hasFeatureAccess(Long tenantId, String featureCode) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (!tenant.isSubscriptionValid()) {
            return false;
        }

        // Check if feature is enabled for this tier
        return featureAccessRepository.existsByPricingTierAndFeatureCodeAndEnabledTrue(
                tenant.getPricingTier().name(), 
                featureCode
        );
    }

    /**
     * Require feature access (throws exception if not available)
     */
    public void requireFeatureAccess(Long tenantId, String featureCode) {
        if (!hasFeatureAccess(tenantId, featureCode)) {
            throw new ProductNotSubscribedException(
                    "Feature not available in current plan: " + featureCode
            );
        }
    }

    /**
     * Get enabled features for a tenant
     */
    public Set<String> getEnabledFeatures(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (!tenant.isSubscriptionValid()) {
            return Collections.emptySet();
        }

        return featureAccessRepository.findEnabledFeatureCodes(tenant.getPricingTier().name());
    }

    /**
     * Check if tenant can add more users (based on plan)
     */
    public boolean canAddMoreUsers(Long tenantId, int currentUsers) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        int maxUsers = tenant.getPricingTier().getMaxUsers();
        return maxUsers == -1 || currentUsers < maxUsers;
    }

    /**
     * Check if tenant can add more locations (based on plan)
     */
    public boolean canAddMoreLocations(Long tenantId, int currentLocations) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        int maxLocations = tenant.getPricingTier().getMaxLocations();
        return maxLocations == -1 || currentLocations < maxLocations;
    }

    /**
     * Check storage quota
     */
    public boolean isWithinStorageQuota(Long tenantId, long requiredGb) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        int maxStorageGb = tenant.getPricingTier().getMaxStorageGb();
        if (maxStorageGb == -1) {
            return true; // Unlimited
        }

        // Get current usage
        UsageTracking usage = usageTrackingRepository
                .findByTenantIdAndTrackingMonth(tenantId, LocalDate.now().withDayOfMonth(1))
                .orElse(null);

        if (usage == null) {
            return requiredGb <= maxStorageGb;
        }

        return (usage.getStorageUsedGb() + requiredGb) <= maxStorageGb;
    }

    /**
     * Record usage (API calls, SMS, etc.)
     */
    @Transactional
    public void recordApiCall(Long tenantId) {
        recordUsage(tenantId, 1, 0, 0);
    }

    /**
     * Record SMS usage
     */
    @Transactional
    public void recordSmsSent(Long tenantId, int count) {
        recordUsage(tenantId, 0, 0, count);
    }

    /**
     * Record storage usage
     */
    @Transactional
    public void recordStorageUsed(Long tenantId, long gb) {
        recordUsage(tenantId, 0, gb, 0);
    }

    /**
     * Record usage for all metrics at once
     */
    @Transactional
    public void recordUsage(Long tenantId, long apiCalls, long storageGb, int smsCount) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);

        UsageTracking usage = usageTrackingRepository
                .findByTenantIdAndTrackingMonth(tenantId, currentMonth)
                .orElseGet(() -> UsageTracking.builder()
                        .tenantId(tenantId)
                        .trackingMonth(currentMonth)
                        .build());

        if (apiCalls > 0) {
            usage.setApiCallsUsed(usage.getApiCallsUsed() + apiCalls);
        }
        if (storageGb > 0) {
            usage.setStorageUsedGb(usage.getStorageUsedGb() + storageGb);
        }
        if (smsCount > 0) {
            usage.setSmsSent(usage.getSmsSent() + smsCount);
        }

        usageTrackingRepository.save(usage);
    }

    /**
     * Get current usage for the month
     */
    public UsageTracking getCurrentMonthUsage(Long tenantId) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        return usageTrackingRepository
                .findByTenantIdAndTrackingMonth(tenantId, currentMonth)
                .orElseGet(() -> UsageTracking.builder()
                        .tenantId(tenantId)
                        .trackingMonth(currentMonth)
                        .build());
    }

    /**
     * Get pricing plan details
     */
    public PricingPlan getPricingPlan(String tier) {
        return pricingPlanRepository.findByTierAndActiveTrue(tier)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + tier));
    }

    /**
     * Get all active pricing plans
     */
    public List<PricingPlan> getAllActivePricingPlans() {
        return pricingPlanRepository.findAll().stream()
                .filter(PricingPlan::getActive)
                .sorted(Comparator.comparing(p -> p.getPricePhp().intValue()))
                .toList();
    }

    /**
     * Check if tenant is on trial
     */
    public boolean isOnTrial(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        return tenant.isOnTrial();
    }

    /**
     * Get days remaining in trial
     */
    public int getTrialDaysRemaining(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (tenant.getTrialEndsAt() == null) {
            return 0;
        }

        return (int) java.time.temporal.ChronoUnit.DAYS
                .between(java.time.LocalDateTime.now(), tenant.getTrialEndsAt());
    }
}
