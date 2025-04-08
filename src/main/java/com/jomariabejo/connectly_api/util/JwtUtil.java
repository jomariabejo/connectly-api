package com.jomariabejo.connectly_api.util;

import com.jomariabejo.connectly_api.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    // Secret key for signing the JWT token
    private String secretKey = "your_super_secret_key_for_jwt_signing_which_should_be_long";
    private long expirationTime = 86400000L; // 24 hours in milliseconds

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the user for whom the token is generated.
     * @return the JWT token.
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .claim("role", user.getRole())  // Include additional claims like role
                .signWith(SignatureAlgorithm.HS256, secretKey)  // Use HMAC with SHA-256
                .compact();
    }

    /**
     * Extracts the claims from the token.
     *
     * @param token the JWT token.
     * @return the claims.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)  // Specify the key for validation
                .parseClaimsJws(token)  // Parse the token (automatically handles base64url)
                .getBody();
    }


    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token the JWT token.
     * @return the username.
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Checks if the JWT token is expired.
     *
     * @param token the JWT token.
     * @return true if the token is expired, false otherwise.
     */
    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validates the JWT token.
     *
     * @param token the JWT token.
     * @param username the username to validate against.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }
    // Test
}
