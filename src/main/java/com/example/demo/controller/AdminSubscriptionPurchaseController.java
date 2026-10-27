

package com.example.demo.controller;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.SubscriptionPurchaseDto.*;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.SubscriptionPurchaseService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.PurchasePermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sub-Admin: every endpoint re-validates the DB-driven permission chain via
 * AuthorizationService.checkPermission(...) before touching the service.
 *
 *   reads          -> SUBP_VIEW
 *   confirm        -> SUBP_CONFIRM
 *   reject         -> SUBP_REJECT
 *   revoke         -> SUBP_REVOKE
 *   mark-refunded  -> SUBP_REFUND
 */
@RestController
@RequestMapping("/api/v1/admin/subscription-purchases")
public class AdminSubscriptionPurchaseController {

    private static final String P_VIEW    = PurchasePermissionCodes.PURCHASE_VIEW;
    private static final String P_CONFIRM = PurchasePermissionCodes.PURCHASE_CONFIRM;
    private static final String P_REJECT  = PurchasePermissionCodes.PURCHASE_REJECT;
    private static final String P_REVOKE  = PurchasePermissionCodes.PURCHASE_REVOKE;
    private static final String P_REFUND  = PurchasePermissionCodes.PURCHASE_REFUND;

    private final SubscriptionPurchaseService service;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util;

    public AdminSubscriptionPurchaseController(SubscriptionPurchaseService service,
                                               AuthorizationService authorizationService,
                                               SuperAdminUtil util) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<PurchaseResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Purchases fetched",
                service.list(status, username, orderId, planId, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats(HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", service.stats(), 200));
    }

    @GetMapping("/stuck")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> stuck(
            @RequestParam(defaultValue = "30") int olderThanMinutes, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Stuck purchases fetched",
                service.findStuck(olderThanMinutes), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PurchaseDetailResponse>> detail(
            @PathVariable String orderId, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Purchase fetched", service.getDetail(orderId), 200));
    }

    @GetMapping("/{orderId}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> audit(
            @PathVariable String orderId, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Audit history fetched",
                service.getAuditHistory(orderId), 200));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<PurchaseResponse>> confirm(
            @PathVariable String orderId, @Valid @RequestBody ConfirmRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_CONFIRM);
        return ResponseEntity.ok(ApiResponse.success("Purchase confirmed",
                service.confirm(orderId, request, a.getId(), a.getFullName(),
                        a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<PurchaseResponse>> reject(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REJECT);
        return ResponseEntity.ok(ApiResponse.success("Purchase rejected",
                service.reject(orderId, request, a.getId(), a.getFullName(),
                        a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/revoke")
    public ResponseEntity<ApiResponse<PurchaseResponse>> revoke(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REVOKE);
        return ResponseEntity.ok(ApiResponse.success("Subscription revoked",
                service.revoke(orderId, request, a.getId(), a.getFullName(),
                        a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/mark-refunded")
    public ResponseEntity<ApiResponse<PurchaseResponse>> markRefunded(
            @PathVariable String orderId, @Valid @RequestBody RefundRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REFUND);
        return ResponseEntity.ok(ApiResponse.success("Purchase marked refunded",
                service.markRefunded(orderId, request, a.getId(), a.getFullName(),
                        a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    /** JWT identity check, then the full live DB permission chain. Never one without the other. */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}