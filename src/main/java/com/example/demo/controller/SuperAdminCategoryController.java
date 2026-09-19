package com.example.demo.controller;

import com.example.demo.dto.CategoryDto;
import com.example.demo.service.CategoryService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Super Admin: full, unrestricted CRUD over Categories — same trust model as
 * Roles/Modules/Permissions/SubscriptionPlan/Products in SuperAdminController.
 * No permission chain check needed since there is exactly one Super Admin.
 *
 * Routes:
 *   POST   /api/v1/super-admin/categories            → create
 *   GET    /api/v1/super-admin/categories            → list  (?isActive=true|false)
 *   GET    /api/v1/super-admin/categories/{id}       → get one
 *   PUT    /api/v1/super-admin/categories/{id}       → update name
 *   PATCH  /api/v1/super-admin/categories/{id}/active → toggle active flag
 *   DELETE /api/v1/super-admin/categories/{id}       → soft delete
 */
@RestController
@RequestMapping("/api/v1/super-admin/categories")
public class SuperAdminCategoryController {

    private final CategoryService categoryService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService; // only used to resolve actor's real fullName for audit

    public SuperAdminCategoryController(CategoryService categoryService,
                                        SuperAdminUtil util,
                                        SuperAdminService superAdminService) {
        this.categoryService = categoryService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    // ── CREATE ──────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> create(
            @Valid @RequestBody CategoryDto.CreateCategoryRequest request,
            HttpServletRequest http) {

        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Category created",
                        categoryService.create(request, actorId, actorName(actorId), "SUPER_ADMIN",
                                util.getClientIp(http)), 201));
    }

    // ── LIST ─────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto.CategoryListItemResponse>>> list(
            @RequestParam(name = "isActive", required = false) Boolean active,
            HttpServletRequest http) {

        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched",
                        categoryService.list(active), 200));
    }

    // ── GET ONE ──────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {

        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(
                ApiResponse.success("Category fetched",
                        categoryService.get(id), 200));
    }

    // ── UPDATE ───────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDto.UpdateCategoryRequest request,
            HttpServletRequest http) {

        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(
                ApiResponse.success("Category updated",
                        categoryService.update(id, request, actorId, actorName(actorId), "SUPER_ADMIN",
                                util.getClientIp(http)), 200));
    }

    // ── ACTIVE TOGGLE ────────────────────────────

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<CategoryDto.CategoryResponse>> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDto.ActiveFlagRequest request,
            HttpServletRequest http) {

        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(
                ApiResponse.success("Category active flag updated",
                        categoryService.setActive(id, request.getActive(), actorId, actorName(actorId),
                                "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    // ── SOFT DELETE ──────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id, HttpServletRequest http) {

        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        categoryService.delete(id, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null, 200));
    }

    // ── PRIVATE HELPERS ──────────────────────────

    /** Resolves the actual Super Admin's full name for the audit trail. */
    private String actorName(UUID actorId) {
        return superAdminService.profile(actorId).getFullName();
    }
}
