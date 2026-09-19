package com.example.demo.controller;

import com.example.demo.dto.CategoryDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.CategoryService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.CategoryPermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;






/**
 * Sub-Admin: every endpoint re-validates the full DB-driven chain
 * (Admin active → Role active → Permission active → Module active →
 * Role has Permission) via AuthorizationService.checkPermission(...) before
 * touching CategoryService. A valid JWT alone is never sufficient — same
 * rule as AdminProductController / AdminSubscriptionPlanController.
 *
 * Grant access by attaching the relevant CATEGORY_* permission codes to a
 * Role (PUT /api/v1/super-admin/roles/{id}/permissions), not by editing
 * this file. Each code must first exist as a row under the CATEGORY module
 * (Super Admin → Modules & Permissions) — see CategoryPermissionCodes for
 * the exact spelling required.
 *
 * Routes:
 *   POST   /api/v1/admin/categories            → create        (CATEGORY_CREATE)
 *   GET    /api/v1/admin/categories            → list          (CATEGORY_VIEW)
 *   GET    /api/v1/admin/categories/{id}       → get one       (CATEGORY_VIEW)
 *   PUT    /api/v1/admin/categories/{id}       → rename        (CATEGORY_EDIT)
 *   PATCH  /api/v1/admin/categories/{id}/active → toggle active (CATEGORY_EDIT)
 *   DELETE /api/v1/admin/categories/{id}       → soft delete   (CATEGORY_DELETE)
 */





@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util; // reused only for getClientIp(...)

    public AdminCategoryController(CategoryService categoryService,
                                   AuthorizationService authorizationService,
                                   SuperAdminUtil util) {
        this.categoryService = categoryService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    // ── CREATE ──────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> create(
            @Valid @RequestBody CategoryDto.CreateCategoryRequest request,
            HttpServletRequest http) {

        AdminEntity admin = authorize(http, CategoryPermissionCodes.CATEGORY_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Category created",
                        categoryService.create(request,
                                admin.getId(), admin.getFullName(),
                                admin.getRole().getRoleName(),
                                util.getClientIp(http)), 201));
    }

    // ── LIST ─────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto.CategoryListItemResponse>>> list(
            @RequestParam(name = "isActive", required = false) Boolean active,
            HttpServletRequest http) {

        authorize(http, CategoryPermissionCodes.CATEGORY_VIEW);
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched",
                        categoryService.list(active), 200));
    }

    // ── GET ONE ──────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {

        authorize(http, CategoryPermissionCodes.CATEGORY_VIEW);
        return ResponseEntity.ok(
                ApiResponse.success("Category fetched",
                        categoryService.get(id), 200));
    }

    // ── UPDATE (rename) ──────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDto.UpdateCategoryRequest request,
            HttpServletRequest http) {

        AdminEntity admin = authorize(http, CategoryPermissionCodes.CATEGORY_EDIT);
        return ResponseEntity.ok(
                ApiResponse.success("Category updated",
                        categoryService.update(id, request,
                                admin.getId(), admin.getFullName(),
                                admin.getRole().getRoleName(),
                                util.getClientIp(http)), 200));
    }

    // ── ACTIVE TOGGLE ────────────────────────────

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDto.ActiveFlagRequest request,
            HttpServletRequest http) {

        AdminEntity admin = authorize(http, CategoryPermissionCodes.CATEGORY_EDIT);
        return ResponseEntity.ok(
                ApiResponse.success("Category active flag updated",
                        categoryService.setActive(id, request.getActive(),
                                admin.getId(), admin.getFullName(),
                                admin.getRole().getRoleName(),
                                util.getClientIp(http)), 200));
    }

    // ── SOFT DELETE ──────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id, HttpServletRequest http) {

        AdminEntity admin = authorize(http, CategoryPermissionCodes.CATEGORY_DELETE);
        categoryService.delete(id,
                admin.getId(), admin.getFullName(),
                admin.getRole().getRoleName(),
                util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null, 200));
    }

    // ── PRIVATE HELPERS ──────────────────────────

    /**
     * Step 1 — verify the JWT and extract the admin's ID.
     * Step 2 — walk the full live DB chain: Admin active → Role active →
     *           Permission active → Module active → Role has Permission.
     * Returns the fully loaded AdminEntity (with role) on success; throws on any failure.
     * Never skip either step — a valid token without a live permission check is not enough.
     */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}
