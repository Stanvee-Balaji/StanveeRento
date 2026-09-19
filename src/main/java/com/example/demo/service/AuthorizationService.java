package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Central, database-driven authorization used by EVERY Sub-Admin API.
 * A valid JWT alone is NEVER sufficient — every request re-checks the
 * live chain: Admin -> Role -> RolePermission -> Permission -> Module.
 *
 * Steps performed (in order), matching the required spec exactly:
 *  1. JWT is valid.
 *  2. Admin exists.
 *  3. Admin is active.
 *  4. Assigned Role exists and is active.
 *  5. Required Module exists and is active.
 *  6. Required Permission exists and is active.
 *  7. The Role has the required Permission (role_permissions lookup).
 *  8. If all checks pass -> allow.
 *  9. Otherwise -> 403 Forbidden (ForbiddenException).
 */
@Service
public class AuthorizationService {

    private final JwtUtil jwtUtil;
    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public AuthorizationService(
            JwtUtil jwtUtil,
            AdminRepository adminRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository) {

        this.jwtUtil = jwtUtil;
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Reads the raw Authorization header (no "Bearer " prefix, consistent
     * with the rest of this codebase), validates the Admin JWT, and returns
     * the authenticated Admin's ID. Throws UnauthorizedException on any
     * token problem.
     */
    public UUID getAuthenticatedAdminId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new SuperAdminService.UnauthorizedException("Authorization token is required");
        }
        String token = header.trim();

        try {
            Claims claims = jwtUtil.getClaims(token);

            if (!"ACCESS".equals(jwtUtil.getTokenType(token))) {
                throw new SuperAdminService.UnauthorizedException("Access token is required");
            }
            if (!"ADMIN".equals(jwtUtil.getRoleType(token))) {
                throw new SuperAdminService.UnauthorizedException("Admin access token is required");
            }
            return UUID.fromString(claims.get("userId", String.class));
        } catch (SuperAdminService.UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new SuperAdminService.UnauthorizedException("Invalid or expired token");
        }
    }

    /**
     * Runs the full 7-check authorization chain for a given Admin and
     * required permission code (e.g. "PRODUCT_EDIT"). Throws
     * ForbiddenException (-> HTTP 403) on any failed check.
     */
    @Transactional(readOnly = true)
    public AdminEntity checkPermission(UUID adminId, String permissionCode) {

        // 2 & 3. Admin exists and is active.
        AdminEntity admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new SuperAdminService.ForbiddenException("Admin not found"));

        if (!admin.isActive()) {
            throw new SuperAdminService.ForbiddenException("Admin account is inactive");
        }

        // 4. Assigned Role exists and is active.
        RoleEntity role = admin.getRole();
        if (role == null || role.getDeletedAt() != null) {
            throw new SuperAdminService.ForbiddenException("Assigned role no longer exists");
        }
        if (!role.isActive()) {
            throw new SuperAdminService.ForbiddenException("Assigned role is inactive");
        }

        // 5 & 6. Required Permission exists, is active, and its Module exists and is active.
        PermissionEntity permission = permissionRepository
                .findByCodeIgnoreCaseAndDeletedAtIsNull(permissionCode)
                .orElseThrow(() -> new SuperAdminService.ForbiddenException("Permission not found: " + permissionCode));

        if (!permission.isActive()) {
            throw new SuperAdminService.ForbiddenException("Permission is inactive: " + permissionCode);
        }

        ModuleEntity module = permission.getModule();
        if (module == null || module.getDeletedAt() != null || !module.isActive()) {
            throw new SuperAdminService.ForbiddenException("Module is inactive for permission: " + permissionCode);
        }

        // 7. The Role must have this exact Permission mapped (and both must be active,
        //    re-checked here defensively in case of any race with the queries above).
        boolean granted = rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndRole_ActiveTrueAndPermission_ActiveTrue(
                        role.getId(), permission.getId());

        if (!granted) {
            throw new SuperAdminService.ForbiddenException(
                    "Role '" + role.getRoleName() + "' does not have permission: " + permissionCode);
        }

        // 8. All checks passed.
        return admin;
    }
}
