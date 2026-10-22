package com.example.demo.controller;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.SubscriptionPurchaseDto.*;
import com.example.demo.service.SubscriptionPurchaseService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Super Admin: unrestricted (no permission lookup), same endpoints as the Sub-Admin controller.
 * Secure the /api/v1/super-admin/** prefix in SecurityConfig exactly like your other super-admin controllers.
 */
@RestController
@RequestMapping("/api/v1/super-admin/subscription-purchases")
public class SuperAdminSubscriptionPurchaseController {

    private static final String ROLE = "SUPER_ADMIN";

    private final SubscriptionPurchaseService service;
    private final SuperAdminUtil util;

    public SuperAdminSubscriptionPurchaseController(SubscriptionPurchaseService service, SuperAdminUtil util) {
        this.service = service;
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
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Purchases fetched",
                service.list(status, username, orderId, planId, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", service.stats(), 200));
    }

    @GetMapping("/stuck")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> stuck(@RequestParam(defaultValue = "30") int olderThanMinutes) {
        return ResponseEntity.ok(ApiResponse.success("Stuck purchases fetched", service.findStuck(olderThanMinutes), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PurchaseDetailResponse>> detail(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Purchase fetched", service.getDetail(orderId), 200));
    }

    @GetMapping("/{orderId}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> audit(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Audit history fetched", service.getAuditHistory(orderId), 200));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<PurchaseResponse>> confirm(
            @PathVariable String orderId, @Valid @RequestBody ConfirmRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Purchase confirmed",
                service.confirm(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<PurchaseResponse>> reject(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Purchase rejected",
                service.reject(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/revoke")
    public ResponseEntity<ApiResponse<PurchaseResponse>> revoke(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Subscription revoked",
                service.revoke(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/mark-refunded")
    public ResponseEntity<ApiResponse<PurchaseResponse>> markRefunded(
            @PathVariable String orderId, @Valid @RequestBody RefundRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Purchase marked refunded",
                service.markRefunded(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    // ── ADAPT: replace these two with however your other super-admin controllers read the logged-in super admin
    //    (e.g. a SuperAdminUtil / JWT helper). Default: the JWT principal name; actorId is null if it is not a UUID.
    private String actorName() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SUPER_ADMIN" : a.getName();
    }

    private UUID actorId() {
        try {
            return UUID.fromString(actorName());
        } catch (Exception e) {
            return null;
        }
    }
}
