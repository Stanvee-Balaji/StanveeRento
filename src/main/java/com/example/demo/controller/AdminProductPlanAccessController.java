package com.example.demo.controller;

import com.example.demo.dto.ProductPlanAccessDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.ProductAccessService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.ProductPermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductPlanAccessController {

    private final ProductAccessService accessService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util;

    public AdminProductPlanAccessController(ProductAccessService accessService,
                                             AuthorizationService authorizationService,
                                             SuperAdminUtil util) {
        this.accessService = accessService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    @GetMapping("/{id}/plan-access")
    public ResponseEntity<ApiResponse<ProductPlanAccessDto.AccessResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        authorize(http, ProductPermissionCodes.PRODUCT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Plan access fetched", accessService.getForAdmin(id), 200));
    }

    @PutMapping("/{id}/plan-access")
    public ResponseEntity<ApiResponse<ProductPlanAccessDto.AccessResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductPlanAccessDto.UpdateAccessRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Plan access updated",
                accessService.replaceAllowedPlans(id, request.getPlanIds(), admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}