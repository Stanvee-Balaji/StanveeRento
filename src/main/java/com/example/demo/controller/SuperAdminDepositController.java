package com.example.demo.controller;

import com.example.demo.dto.DepositDto.*;
import com.example.demo.service.DepositService;
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
import java.util.UUID;

/** Super Admin: unrestricted, same endpoints as the Sub-Admin controller. Secure /api/v1/super-admin/** in SecurityConfig. */
@RestController
@RequestMapping("/api/v1/super-admin/deposits")
public class SuperAdminDepositController {

    private static final String ROLE = "SUPER_ADMIN";

    private final DepositService service;
    private final SuperAdminUtil util;

    public SuperAdminDepositController(DepositService service, SuperAdminUtil util) {
        this.service = service;
        this.util = util;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<DepositResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String orderId,
            @RequestParam(defaultValue = "false") boolean onlyWithBalance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Deposits fetched",
                service.list(status, username, orderId, onlyWithBalance, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DepositStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success("Deposit stats fetched", service.stats(), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DepositResponse>> detail(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Deposit fetched", service.getDetail(orderId), 200));
    }

    @PostMapping("/{orderId}/deduct")
    public ResponseEntity<ApiResponse<DepositResponse>> deduct(
            @PathVariable String orderId, @Valid @RequestBody DeductRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Deposit deducted",
                service.deduct(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<ApiResponse<DepositResponse>> refund(
            @PathVariable String orderId, @Valid @RequestBody DepositRefundRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Deposit refund recorded",
                service.refund(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    // same actor lookup as SuperAdminSubscriptionPurchaseController — replace if you have a SuperAdminUtil helper
    private String actorName() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SUPER_ADMIN" : a.getName();
    }

    private UUID actorId() {
        try { return UUID.fromString(actorName()); } catch (Exception e) { return null; }
    }
}
