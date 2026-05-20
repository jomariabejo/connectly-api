package com.jomariabejo.connectly_api.tenant_api.config;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantContextService tenantContextService;

    public TenantFilter(TenantContextService tenantContextService) {
        this.tenantContextService = tenantContextService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/v1/auth/")
                || path.contains("/v1/public/")
                || path.contains("/v1/payments/webhooks/")
                || path.contains("/actuator/")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof User user) {
                    boolean platformAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    Long tenantId = Long.parseLong(tenantHeader.trim());
                    tenantContextService.resolveAndSetContext(tenantId, user, platformAdmin);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
