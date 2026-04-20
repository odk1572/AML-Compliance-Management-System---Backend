package com.app.aml.security.jwt;

import com.github.f4b6a3.uuid.UuidCreator; // New Import
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating, parsing, and validating JSON Web Tokens (JWT).
 * Uses UUIDv7 for JTIs to ensure time-ordered session tracking and DB performance.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Generates a new JWT containing the user's identity, tenant routing data, and session ID.
     *
     * @param userId   The UUID string of the user (becomes the 'sub' claim)
     * @param tenantId The UUID string of the bank/tenant (null for SUPER_ADMINs)
     * @param role     The user's role (e.g., COMPLIANCE_OFFICER, SUPER_ADMIN)
     * @return A signed JWT string
     */
    public String generateToken(String userId, String tenantId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();

        if (tenantId != null && !tenantId.trim().isEmpty()) {
            extraClaims.put("tenantId", tenantId);
        }
        extraClaims.put("role", role);

        // CHANGE: Using UUIDv7 (Time-Ordered Epoch) instead of randomUUID (v4).
        // This is highly beneficial for your session revocation tables' indexing.
        String jti = UuidCreator.getTimeOrderedEpoch().toString();

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userId)
                .setId(jti) // JWT ID (jti)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts all claims from a valid token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the cryptographic signature and expiration of the token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid or expired JWT Token: {}", e.getMessage());
            return false;
        }
    }

    // Convenience Getters
    public String extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", String.class);
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}