package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantAuditLog;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for logging audit events and security tracking
 * Provides compliance tracking and security monitoring capabilities
 */
@Service
@Transactional
public class AuditLoggingService {
    private final TenantAuditLogRepository auditLogRepository;
    private final TenantContextService tenantContextService;

    public AuditLoggingService(
            TenantAuditLogRepository auditLogRepository,
            TenantContextService tenantContextService) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContextService = tenantContextService;
    }

    /**
     * Log an audit event
     */
    public TenantAuditLog logEvent(Long tenantId, Long userId, String action, 
                                   String resourceType, Long resourceId, 
                                   Map<String, Object> oldValues, 
                                   Map<String, Object> newValues,
                                   String ipAddress, String userAgent) {
        TenantAuditLog log = TenantAuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("SUCCESS")
                .build();

        return auditLogRepository.save(log);
    }

    /**
     * Log an audit event with error
     */
    public TenantAuditLog logEventWithError(Long tenantId, Long userId, String action,
                                           String resourceType, Long resourceId,
                                           String ipAddress, String userAgent,
                                           String errorMessage) {
        TenantAuditLog log = TenantAuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();

        return auditLogRepository.save(log);
    }

    /**
     * Log login event
     */
    public void logLogin(Long tenantId, Long userId, String ipAddress, String userAgent) {
        logEvent(tenantId, userId, TenantAuditLog.ACTION_LOGIN, null, null, null, null, ipAddress, userAgent);
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(Long tenantId, String ipAddress, String userAgent, String reason) {
        logEventWithError(tenantId, null, TenantAuditLog.ACTION_FAILED_LOGIN, 
                         null, null, ipAddress, userAgent, reason);
    }

    /**
     * Log user creation
     */
    public void logUserCreated(Long tenantId, Long createdBy, Long createdUserId, String ipAddress, String userAgent) {
        logEvent(tenantId, createdBy, TenantAuditLog.ACTION_INVITE_USER,
                "User", createdUserId, null, null, ipAddress, userAgent);
    }

    /**
     * Log user removal
     */
    public void logUserRemoved(Long tenantId, Long removedBy, Long removedUserId, String ipAddress, String userAgent) {
        logEvent(tenantId, removedBy, TenantAuditLog.ACTION_REMOVE_USER,
                "User", removedUserId, null, null, ipAddress, userAgent);
    }

    /**
     * Log settings change
     */
    public void logSettingsChange(Long tenantId, Long userId, Map<String, Object> oldValues,
                                 Map<String, Object> newValues, String ipAddress, String userAgent) {
        logEvent(tenantId, userId, TenantAuditLog.ACTION_SETTINGS_CHANGE,
                "TenantSettings", tenantId, oldValues, newValues, ipAddress, userAgent);
    }

    /**
     * Log data export
     */
    public void logDataExport(Long tenantId, Long userId, String exportType, String ipAddress, String userAgent) {
        Map<String, Object> details = Map.of("export_type", exportType);
        logEvent(tenantId, userId, TenantAuditLog.ACTION_DATA_EXPORT,
                "Export", null, null, details, ipAddress, userAgent);
    }

    /**
     * Log plan change
     */
    public void logPlanChange(Long tenantId, Long userId, String oldPlan, String newPlan, String ipAddress, String userAgent) {
        Map<String, Object> oldValues = Map.of("plan", oldPlan);
        Map<String, Object> newValues = Map.of("plan", newPlan);
        logEvent(tenantId, userId, TenantAuditLog.ACTION_CHANGE_PLAN,
                "Subscription", tenantId, oldValues, newValues, ipAddress, userAgent);
    }

    /**
     * Get audit logs for a tenant (paginated)
     */
    public Page<TenantAuditLog> getAuditLogs(Long tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get audit logs for a specific action
     */
    public Page<TenantAuditLog> getAuditLogsByAction(Long tenantId, String action, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    /**
     * Extract IP address from request
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp != null && !clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
            return clientIp.split(",")[0].trim();
        }

        clientIp = request.getHeader("X-Real-IP");
        if (clientIp != null && !clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
            return clientIp;
        }

        clientIp = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            return "127.0.0.1";
        }

        return clientIp;
    }

    /**
     * Extract user agent from request
     */
    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }
}
