package com.jomariabejo.connectly_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    // JWT claim names
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String TENANT_SLUG_CLAIM = "tenant_slug";
    private static final String TENANT_ROLE_CLAIM = "tenant_role";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract tenant ID from JWT token
     * Returns null if tenant_id claim is not present
     */
    public Long extractTenantId(String token) {
        try {
            Object tenantId = extractAllClaims(token).get(TENANT_ID_CLAIM);
            if (tenantId instanceof Number) {
                return ((Number) tenantId).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract tenant slug from JWT token
     */
    public String extractTenantSlug(String token) {
        try {
            return (String) extractAllClaims(token).get(TENANT_SLUG_CLAIM);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract tenant role from JWT token
     */
    public String extractTenantRole(String token) {
        try {
            return (String) extractAllClaims(token).get(TENANT_ROLE_CLAIM);
        } catch (Exception e) {
            return null;
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generate token with tenant context
     */
    public String generateTokenWithTenant(UserDetails userDetails, Long tenantId, String tenantSlug, String tenantRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TENANT_ID_CLAIM, tenantId);
        claims.put(TENANT_SLUG_CLAIM, tenantSlug);
        if (tenantRole != null) {
            claims.put(TENANT_ROLE_CLAIM, tenantRole);
        }
        return buildToken(claims, userDetails, jwtExpiration);
    }

    /**
     * Generate token with additional custom claims and tenant context
     */
    public String generateTokenWithTenant(Map<String, Object> extraClaims, UserDetails userDetails, 
                                        Long tenantId, String tenantSlug, String tenantRole) {
        extraClaims.put(TENANT_ID_CLAIM, tenantId);
        extraClaims.put(TENANT_SLUG_CLAIM, tenantSlug);
        if (tenantRole != null) {
            extraClaims.put(TENANT_ROLE_CLAIM, tenantRole);
        }
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}