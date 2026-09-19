package com.example.demo.controller;

import com.example.demo.dto.SubscriptionPlanDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.SubscriptionPlanService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.PlanPermissionCodes;
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
 * (Admin active -> Role active -> Permission active -> Module active ->
 * Role has Permission) via AuthorizationService.checkPermission(...) before
 * touching the service. A valid JWT alone is never sufficient — same rule
 * as every other Sub-Admin API in this codebase.
 *
 * Grant access by attaching the relevant PLAN_* permission codes to a Role
 * (PUT /api/v1/super-admin/roles/{id}/permissions), not by editing this file.
 */



@RestController
@RequestMapping("/api/v1/admin/subscription-plans")
public class AdminSubscriptionPlanController {

    private final SubscriptionPlanService planService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util; // reused only for getClientIp(...)

    public AdminSubscriptionPlanController(SubscriptionPlanService planService,
                                            AuthorizationService authorizationService,
                                            SuperAdminUtil util) {
        this.planService = planService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> create(
            @Valid @RequestBody SubscriptionPlanDto.CreatePlanRequest request, HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Plan created",
                planService.create(request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto.PlanListItemResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(name = "isActive", required = false) Boolean active,
            HttpServletRequest http) {
        authorize(http, PlanPermissionCodes.PLAN_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Plans fetched", planService.list(status, active), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        authorize(http, PlanPermissionCodes.PLAN_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Plan fetched", planService.get(id), 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdatePlanRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan updated",
                planService.update(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/features")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> updateFeatures(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdateFeaturesRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan features updated",
                planService.updateFeatures(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/limits")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> updateLimits(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdateLimitsRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan limits updated",
                planService.updateLimits(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> status(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.PlanStatusRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan status updated",
                planService.changeStatus(id, request.getStatus(), admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> active(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.ActiveFlagRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan active flag updated",
                planService.setActive(id, request.getActive(), admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest http) {
        AdminEntity admin = authorize(http, PlanPermissionCodes.PLAN_DELETE);
        planService.delete(id, admin.getId(), admin.getFullName(), admin.getRole().getRoleName(), util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Plan deleted", null, 200));
    }

    /** JWT identity check, then the full live DB permission chain. Never one without the other. */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}
