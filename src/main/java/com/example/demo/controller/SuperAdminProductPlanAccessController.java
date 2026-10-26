package com.example.demo.controller;

import com.example.demo.dto.ProductPlanAccessDto;
import com.example.demo.service.ProductAccessService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin/products")
public class SuperAdminProductPlanAccessController {

    private final ProductAccessService accessService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService;

    public SuperAdminProductPlanAccessController(ProductAccessService accessService, SuperAdminUtil util,
                                                  SuperAdminService superAdminService) {
        this.accessService = accessService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    @GetMapping("/{id}/plan-access")
    public ResponseEntity<ApiResponse<ProductPlanAccessDto.AccessResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Plan access fetched", accessService.getForAdmin(id), 200));
    }

    @PutMapping("/{id}/plan-access")
    public ResponseEntity<ApiResponse<ProductPlanAccessDto.AccessResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductPlanAccessDto.UpdateAccessRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        String actorName = superAdminService.profile(actorId).getFullName();
        return ResponseEntity.ok(ApiResponse.success("Plan access updated",
                accessService.replaceAllowedPlans(id, request.getPlanIds(), actorId, actorName,
                        "SUPER_ADMIN", util.getClientIp(http)), 200));
    }
}