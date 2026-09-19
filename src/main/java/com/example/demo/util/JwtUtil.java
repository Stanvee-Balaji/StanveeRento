package com.example.demo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-expiration-ms:900000}") long accessExpirationMs,
            @Value("${security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {

        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UUID userId, String roleType, String email) {
        return generate(userId, roleType, email, "ACCESS", accessExpirationMs);
    }

    public String generateRefreshToken(UUID userId, String roleType, String email) {
        return generate(userId, roleType, email, "REFRESH", refreshExpirationMs);
    }

    private String generate(UUID userId, String roleType, String email, String tokenType, long expiry) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("userId", userId.toString())
                .claim("roleType", roleType)
                .claim("email", email)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expires)
                .signWith(key)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).get("userId", String.class));
    }

    public String getRoleType(String token) {
        return getClaims(token).get("roleType", String.class);
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getTokenType(String token) {
        return getClaims(token).get("tokenType", String.class);
    }

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(getTokenType(token));
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }
}
