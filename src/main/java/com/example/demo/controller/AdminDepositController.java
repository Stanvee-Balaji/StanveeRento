package com.example.demo.controller;

import com.example.demo.dto.DepositDto.*;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.DepositService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.DepositPermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sub-Admin: every endpoint re-validates the DB permission chain first.
 *   reads -> DEP_VIEW, deduct -> DEP_DEDUCT, refund -> DEP_REFUND
 */
@RestController
@RequestMapping("/api/v1/admin/deposits")
public class AdminDepositController {

    private final DepositService service;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util;

    public AdminDepositController(DepositService service, AuthorizationService authorizationService, SuperAdminUtil util) {
        this.service = service;
        this.authorizationService = authorizationService;
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
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest http) {
        authorize(http, DepositPermissionCodes.DEPOSIT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Deposits fetched",
                service.list(status, username, orderId, onlyWithBalance, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DepositStatsResponse>> stats(HttpServletRequest http) {
        authorize(http, DepositPermissionCodes.DEPOSIT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Deposit stats fetched", service.stats(), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DepositResponse>> detail(@PathVariable String orderId, HttpServletRequest http) {
        authorize(http, DepositPermissionCodes.DEPOSIT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Deposit fetched", service.getDetail(orderId), 200));
    }

    @PostMapping("/{orderId}/deduct")
    public ResponseEntity<ApiResponse<DepositResponse>> deduct(
            @PathVariable String orderId, @Valid @RequestBody DeductRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, DepositPermissionCodes.DEPOSIT_DEDUCT);
        return ResponseEntity.ok(ApiResponse.success("Deposit deducted",
                service.deduct(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<ApiResponse<DepositResponse>> refund(
            @PathVariable String orderId, @Valid @RequestBody DepositRefundRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, DepositPermissionCodes.DEPOSIT_REFUND);
        return ResponseEntity.ok(ApiResponse.success("Deposit refund recorded",
                service.refund(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}
