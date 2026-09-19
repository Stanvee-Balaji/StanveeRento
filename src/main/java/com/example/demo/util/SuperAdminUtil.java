package com.example.demo.util;

import com.example.demo.service.SuperAdminService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SuperAdminUtil {

    private final JwtUtil jwtUtil;
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public SuperAdminUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Reads the Authorization header as the RAW token — no "Bearer " prefix
     * expected or stripped. Clients must send:
     *   Authorization: <token>
     * NOT:
     *   Authorization: Bearer <token>
     */
    public String getBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new SuperAdminService.UnauthorizedException("Authorization token is required");
        }
        return header.trim();
    }

    public UUID getAuthenticatedSuperAdminId(HttpServletRequest request) {
        String token = getBearerToken(request);

        try {
            Claims claims = jwtUtil.getClaims(token);

            if (isBlacklisted(jwtUtil.getTokenId(token))) {
                throw new SuperAdminService.UnauthorizedException("Token has been logged out");
            }

            if (!"ACCESS".equals(jwtUtil.getTokenType(token))) {
                throw new SuperAdminService.UnauthorizedException("Access token is required");
            }

            if (!"SUPER_ADMIN".equals(jwtUtil.getRoleType(token))) {
                throw new SuperAdminService.ForbiddenException("Super Admin access required");
            }

            return UUID.fromString(claims.get("userId", String.class));
        } catch (SuperAdminService.UnauthorizedException | SuperAdminService.ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("[SuperAdminUtil] Token validation failed: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new SuperAdminService.UnauthorizedException("Invalid or expired token");
        }
        
    }

    public void blacklistToken(String token) {
        blacklist.put(jwtUtil.getTokenId(token), jwtUtil.getExpiration(token).getTime());
    }

    public boolean isBlacklisted(String tokenId) {
        Long expiry = blacklist.get(tokenId);

        if (expiry == null) {
            return false;
        }

        if (expiry < System.currentTimeMillis()) {
            blacklist.remove(tokenId);
            return false;
        }

        return true;
    }

    public String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        
        return request.getRemoteAddr();
    }
    
    
}
