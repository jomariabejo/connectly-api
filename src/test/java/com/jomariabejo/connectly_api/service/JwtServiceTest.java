package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>No mocks -- the service only talks to jjwt. Its two {@code @Value} fields are seeded with
 * {@link ReflectionTestUtils} because plain JUnit does no property injection.
 *
 * <p>Note {@code getSignInKey()} runs the configured secret through {@code Decoders.BASE64}, so the
 * key here must be valid Base64 that decodes to at least 32 bytes for HS256.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    /** 48 bytes once Base64-decoded, comfortably above the 32-byte HS256 minimum. */
    private static final String SECRET =
            "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ONE_HOUR_MS);

        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    @Test
    @DisplayName("round-trips the subject, which is the user's email")
    void roundTripsSubject() {
        String token = jwtService.generateToken(user);

        // User.getUsername() returns the email, so that is what lands in the subject claim.
        assertThat(jwtService.extractUsername(token)).isEqualTo("someone@example.com");
    }

    @Test
    @DisplayName("carries extra claims through")
    void carriesExtraClaims() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), user);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("sets the expiry one configured lifetime ahead of issue time")
    void setsExpiryFromConfiguration() {
        String token = jwtService.generateToken(user);

        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        // jjwt truncates both to whole seconds, so compare in seconds.
        assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(ONE_HOUR_MS);
        assertThat(jwtService.getExpirationTime()).isEqualTo(ONE_HOUR_MS);
    }

    @Test
    @DisplayName("accepts a fresh token for the matching user")
    void acceptsMatchingUser() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("rejects a token issued for somebody else")
    void rejectsOtherUser() {
        String token = jwtService.generateToken(user);

        User other = new User("other", "hashed", "other@example.com");
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expired = jwtService.generateToken(user);

        // jjwt refuses to even parse a token that is past its expiry.
        assertThatThrownBy(() -> jwtService.isTokenValid(expired, user))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("rejects a token signed with a different key")
    void rejectsForeignSignature() {
        JwtService otherIssuer = new JwtService();
        ReflectionTestUtils.setField(otherIssuer, "secretKey",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ReflectionTestUtils.setField(otherIssuer, "jwtExpiration", ONE_HOUR_MS);

        String foreign = otherIssuer.generateToken(user);

        // JwtException rather than the concrete SignatureException: jjwt moved that class between
        // io.jsonwebtoken and io.jsonwebtoken.security, and the supertype is stable across both.
        assertThatThrownBy(() -> jwtService.extractUsername(foreign))
                .isInstanceOf(JwtException.class);
    }
}
