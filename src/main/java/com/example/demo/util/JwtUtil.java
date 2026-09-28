package com.example.demo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    
    
    
    
    
    // Change this in production — move to an env var if you want,
    // but no external config file is required to run this.
    private static final String SECRET = "StanveeWalletJwtSecretKey_ChangeMe_2026";
    private static final long EXPIRY_MILLIS = 24 * 60 * 60 * 1000L;

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        long exp = now + EXPIRY_MILLIS;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + escape(username) + "\",\"iat\":" + now + ",\"exp\":" + exp + "}";

        String headerB64 = ENC.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signingInput = headerB64 + "." + payloadB64;
        String signature = sign(signingInput);

        return signingInput + "." + signature;
    }

    /**
     * Returns the username if the token is valid and not expired.
     * Returns null if invalid, tampered, or expired.
     */
    public String validateAndGetUsername(String token) {
        if (token == null || token.isBlank()) return null;

        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = sign(signingInput);
        if (!expectedSig.equals(parts[2])) {
            return null; // tampered / wrong secret
        }

        String payloadJson = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);

        Long exp = extractLongField(payloadJson, "exp");
        if (exp == null || System.currentTimeMillis() > exp) {
            return null; // expired
        }

        return extractStringField(payloadJson, "sub");
    }

    public boolean isValid(String token) {
        return validateAndGetUsername(token) != null;
    }

    // ── internal helpers ──

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return ENC.encodeToString(sig);
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }

    // Tiny hand-written extractors — avoids pulling in a JSON lib just for this.
    private String extractStringField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    private Long extractLongField(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)))) end++;
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    } 
}
