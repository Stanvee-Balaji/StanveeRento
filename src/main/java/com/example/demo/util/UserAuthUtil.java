package com.example.demo.util;

import com.example.demo.service.SuperAdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Identifies the logged-in Stanvee USER from the "Authorization: Bearer <jwt>" header.
 *
 * The user token is created by JwtUtil.generateToken(username) and its "sub" claim
 * is the username. Admin / Super Admin tokens are signed with a different key, so
 * they fail validation here and can never act as a user.
 */
@Component
public class UserAuthUtil {

    private final JwtUtil jwtUtil;

    public UserAuthUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Returns the username or throws UnauthorizedException (401). */
    public String requireUsername(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new SuperAdminService.UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = header.substring(7).trim();
        String username = jwtUtil.validateAndGetUsername(token);
        if (username == null || username.isBlank()) {
            throw new SuperAdminService.UnauthorizedException("Invalid or expired token");
        }
        return username;
    }
}
