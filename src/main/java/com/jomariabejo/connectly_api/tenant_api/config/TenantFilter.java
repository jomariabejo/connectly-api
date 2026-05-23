package com.jomariabejo.connectly_api.tenant_api.config;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.util.SubdomainExtractor;
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
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;

    public TenantFilter(TenantContextService tenantContextService, 
                       TenantRepository tenantRepository,
                       JwtService jwtService) {
        this.tenantContextService = tenantContextService;
        this.tenantRepository = tenantRepository;
        this.jwtService = jwtService;
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
            Long tenantId = null;

            // Method 1: Try to extract from JWT token (highest priority)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tenantId = jwtService.extractTenantId(token);
            }

            // Method 2: Try subdomain-based resolution (if no tenant in JWT)
            if (tenantId == null) {
                String subdomain = SubdomainExtractor.extractSubdomain(request);
                if (subdomain != null && !subdomain.isEmpty()) {
                    Tenant tenant = tenantRepository.findActiveBySubdomain(subdomain).orElse(null);
                    if (tenant != null) {
                        tenantId = tenant.getId();
                    }
                }
            }

            // Method 3: Fall back to X-Tenant-Id header (for backwards compatibility)
            if (tenantId == null) {
                String tenantHeader = request.getHeader(TENANT_HEADER);
                if (tenantHeader != null && !tenantHeader.isBlank()) {
                    try {
                        tenantId = Long.parseLong(tenantHeader.trim());
                    } catch (NumberFormatException e) {
                        // Invalid tenant ID format, continue without setting tenant context
                    }
                }
            }

            // Resolve and set tenant context if tenant ID was found
            if (tenantId != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof User user) {
                    boolean platformAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    tenantContextService.resolveAndSetContext(tenantId, user, platformAdmin);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
