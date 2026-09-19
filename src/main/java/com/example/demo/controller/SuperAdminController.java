package com.example.demo.controller;

import com.example.demo.dto.AdminDto;
import com.example.demo.dto.SuperAdminDto;
import com.example.demo.service.AdminService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Single entry point for everything a Super Admin does.
 *
 * Sections (all under /api/v1/super-admin):
 *   - Authentication:      /login, /logout, /refresh-token, /me
 *   - Dashboard:           /dashboard/summary, /dashboard/recent-activity
 *   - Audit Log:           /audit-logs, /audit-logs/{id}, /audit-logs/export
 *   - Admins (Sub-Admins): /admins/**   (delegates to AdminService/AdminDto)
 *   - Roles:               /roles/**    (SuperAdminService/SuperAdminDto)
 *   - Modules:             /modules/**  (SuperAdminService/SuperAdminDto)
 *   - Permissions:         /permissions/** (SuperAdminService/SuperAdminDto)
 *
 * NOTE:
 *   - Exactly one Super Admin exists (seeded at startup by DataInitializer) —
 *     there is intentionally no "create another Super Admin" endpoint.
 *   - There is no direct "assign permissions to an Admin" endpoint. Permissions
 *     flow only through Roles: edit a Role's permission set, or reassign an
 *     Admin's Role via PATCH /admins/{id}/role.
 *   - Admin self-login/self-profile ("/me" as an Admin) live separately in
 *     AdminController under /api/v1/admin — NOT here. The /admins/** routes
 *     here are Super-Admin-authenticated management of Sub-Admin accounts.
 *   - Exception handling is centralized in GlobalExceptionHandler
 *     (@RestControllerAdvice), so it applies here too.
 */
@RestController
@RequestMapping("/api/v1/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final AdminService adminService;
    private final SuperAdminUtil util;

    public SuperAdminController(SuperAdminService superAdminService,
                                 AdminService adminService,
                                 SuperAdminUtil util) {
        this.superAdminService = superAdminService;
        this.adminService = adminService;
        this.util = util;
    }

    // ================= AUTHENTICATION =================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuperAdminDto.LoginResponse>> login(
            @Valid @RequestBody SuperAdminDto.LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success(
                "Login successful", superAdminService.login(request, util.getClientIp(http)), 200));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<SuperAdminDto.LogoutResponse>> logout(HttpServletRequest http) {
        String token = util.getBearerToken(http);
        util.getAuthenticatedSuperAdminId(http);
        superAdminService.logout(token, util);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null, 200));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<SuperAdminDto.LoginResponse>> refresh(
            @Valid @RequestBody SuperAdminDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed", superAdminService.refreshToken(request.getToken(), util), 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SuperAdminDto.SuperAdminProfileResponse>> me(HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", superAdminService.profile(actorId), 200));
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<SuperAdminDto.DashboardSummaryResponse>> dashboardSummary(
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard summary fetched", superAdminService.dashboardSummary(), 200));
    }

    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<ApiResponse<SuperAdminDto.RecentActivityResponse>> recentActivity(
            @RequestParam(defaultValue = "10") int limit, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Recent activity fetched", superAdminService.recentActivity(limit), 200));
    }

    // ================= AUDIT LOG =================

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<SuperAdminDto.AuditLogPageResponse>> auditLogs(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched",
                superAdminService.auditLogs(adminId, module, action, dateFrom, dateTo, page, size), 200));
    }

    @GetMapping("/audit-logs/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.AuditLogResponse>> auditLog(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Audit log fetched", superAdminService.auditLog(id), 200));
    }

    @GetMapping("/audit-logs/export")
    public ResponseEntity<ApiResponse<List<SuperAdminDto.AuditLogResponse>>> exportAuditLogs(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Audit logs exported",
                superAdminService.exportAuditLogs(adminId, module, action, dateFrom, dateTo), 200));
    }

    // ================= ADMINS (SUB-ADMINS) — delegates to AdminService =================

    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> createAdmin(
            @Valid @RequestBody AdminDto.CreateAdminRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Admin created",
                        adminService.create(request, actorId, util.getClientIp(http)), 201));
    }

    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<AdminDto.AdminPageResponse>> listAdmins(
            @RequestParam(required = false) UUID roleId,
            @RequestParam(name = "isActive", required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Admins fetched", adminService.list(roleId, active, page, size), 200));
    }

    @GetMapping("/admins/{id}")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> getAdmin(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Admin fetched", adminService.get(id), 200));
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> updateAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDto.UpdateAdminRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Admin updated",
                adminService.update(id, request, actorId, util.getClientIp(http)), 200));
    }

    // Only way to change an Admin's permissions: reassign their Role.
    @PatchMapping("/admins/{id}/role")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> changeAdminRole(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDto.ChangeRoleRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Admin role reassigned",
                adminService.changeRole(id, request, actorId, util.getClientIp(http)), 200));
    }

    @PatchMapping("/admins/{id}/status")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> adminStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDto.StatusRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Admin status updated",
                adminService.status(id, request.getActive(), actorId, util.getClientIp(http)), 200));
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(
            @PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        adminService.delete(id, actorId, util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Admin deleted", null, 200));
    }

    @PostMapping("/admins/{id}/reset-password")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> resetAdminPassword(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDto.ResetPasswordRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully",
                adminService.resetPassword(id, request, actorId, util.getClientIp(http)), 200));
    }

    // ================= ROLES =================

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<SuperAdminDto.RoleResponse>> createRole(
            @Valid @RequestBody SuperAdminDto.CreateRoleRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Role created",
                        superAdminService.createRole(request, actorId, util.getClientIp(http)), 201));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<SuperAdminDto.RoleResponse>>> listRoles(HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Roles fetched", superAdminService.listRoles(), 200));
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.RoleResponse>> getRole(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Role fetched", superAdminService.getRole(id), 200));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.UpdateRoleRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                superAdminService.updateRole(id, request, actorId, util.getClientIp(http)), 200));
    }

    // Full replace of this Role's permission set. Every Admin on this Role
    // is affected immediately — no per-Admin update needed.
    @PutMapping("/roles/{id}/permissions")
    public ResponseEntity<ApiResponse<SuperAdminDto.RoleResponse>> updateRolePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.UpdateRolePermissionsRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated",
                superAdminService.updateRolePermissions(id, request, actorId, util.getClientIp(http)), 200));
    }

    @PatchMapping("/roles/{id}/status")
    public ResponseEntity<ApiResponse<SuperAdminDto.RoleResponse>> roleStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.RoleStatusRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Role status updated",
                superAdminService.statusRole(id, request.getActive(), actorId, util.getClientIp(http)), 200));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        superAdminService.deleteRole(id, actorId, util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Role deleted", null, 200));
    }

    // ================= MODULES =================

    @PostMapping("/modules")
    public ResponseEntity<ApiResponse<SuperAdminDto.ModuleResponse>> createModule(
            @Valid @RequestBody SuperAdminDto.CreateModuleRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Module created",
                        superAdminService.createModule(request, actorId, util.getClientIp(http)), 201));
    }

    @GetMapping("/modules")
    public ResponseEntity<ApiResponse<List<SuperAdminDto.ModuleResponse>>> listModules(HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Modules fetched", superAdminService.listModules(), 200));
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.ModuleResponse>> getModule(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Module fetched", superAdminService.getModule(id), 200));
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.ModuleResponse>> updateModule(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.UpdateModuleRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Module updated",
                superAdminService.updateModule(id, request, actorId, util.getClientIp(http)), 200));
    }

    @PatchMapping("/modules/{id}/status")
    public ResponseEntity<ApiResponse<SuperAdminDto.ModuleResponse>> moduleStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.ModuleStatusRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Module status updated",
                superAdminService.statusModule(id, request.getActive(), actorId, util.getClientIp(http)), 200));
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        superAdminService.deleteModule(id, actorId, util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Module deleted", null, 200));
    }

    // ================= PERMISSIONS =================

    @PostMapping("/permissions")
    public ResponseEntity<ApiResponse<SuperAdminDto.PermissionResponse>> createPermission(
            @Valid @RequestBody SuperAdminDto.CreatePermissionRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Permission created",
                        superAdminService.createPermission(request, actorId, util.getClientIp(http)), 201));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<SuperAdminDto.PermissionResponse>>> listPermissions(
            @RequestParam(required = false) UUID moduleId, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Permissions fetched", superAdminService.listPermissions(moduleId), 200));
    }

    @GetMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.PermissionResponse>> getPermission(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success(
                "Permission fetched", superAdminService.getPermission(id), 200));
    }

    @PutMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<SuperAdminDto.PermissionResponse>> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.UpdatePermissionRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Permission updated",
                superAdminService.updatePermission(id, request, actorId, util.getClientIp(http)), 200));
    }

    @PatchMapping("/permissions/{id}/status")
    public ResponseEntity<ApiResponse<SuperAdminDto.PermissionResponse>> permissionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SuperAdminDto.PermissionStatusRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Permission status updated",
                superAdminService.statusPermission(id, request.getActive(), actorId, util.getClientIp(http)), 200));
    }

    @DeleteMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        superAdminService.deletePermission(id, actorId, util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Permission deleted", null, 200));
    }
}