



package com.example.demo.controller;

import com.example.demo.dto.SubscriptionPlanDto;
import com.example.demo.service.SubscriptionPlanService;
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
 * Super Admin: full, unrestricted CRUD over Subscription Plans — same trust
 * model as Roles/Modules/Permissions in SuperAdminController. No permission
 * chain check needed since there is exactly one Super Admin.
 */
@RestController
@RequestMapping("/api/v1/super-admin/subscription-plans")
public class SuperAdminSubscriptionPlanController {

    private final SubscriptionPlanService planService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService; // only used to resolve actor's real fullName for audit

    public SuperAdminSubscriptionPlanController(SubscriptionPlanService planService, SuperAdminUtil util,
                                                  SuperAdminService superAdminService) {
        this.planService = planService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> create(
            @Valid @RequestBody SubscriptionPlanDto.CreatePlanRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Plan created",
                planService.create(request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto.PlanListItemResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(name = "isActive", required = false) Boolean active,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plans fetched", planService.list(status, active), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan fetched", planService.get(id), 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdatePlanRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan updated",
                planService.update(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/features")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> updateFeatures(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdateFeaturesRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan features updated",
                planService.updateFeatures(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/limits")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> updateLimits(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.UpdateLimitsRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan limits updated",
                planService.updateLimits(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> status(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.PlanStatusRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan status updated",
                planService.changeStatus(id, request.getStatus(), actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto.PlanResponse>> active(
            @PathVariable UUID id, @Valid @RequestBody SubscriptionPlanDto.ActiveFlagRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan active flag updated",
                planService.setActive(id, request.getActive(), actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        planService.delete(id, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Plan deleted", null, 200));
    }

    // Real lookup — writes the actual Super Admin's name into audit_logs
    // instead of the literal string "SUPER_ADMIN".
    private String actorName(UUID actorId) {
        return superAdminService.profile(actorId).getFullName();
    }
}
