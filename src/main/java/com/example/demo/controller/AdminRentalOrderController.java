package com.example.demo.controller;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.RentalOrderDto.*;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.util.RentalOrderPermissionCodes;
import com.example.demo.service.RentalOrderService;
import com.example.demo.util.ApiResponse;
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
 * Sub-Admin rental order management: every endpoint re-validates the DB-driven permission chain.
 *   reads -> ORDR_VIEW | confirm -> ORDR_CONFIRM | reject -> ORDR_REJECT | cancel -> ORDR_CANCEL
 *   dispatch/deliver/return/complete -> ORDR_FULFIL | mark-refunded/deposit-settle -> ORDR_REFUND | config PUT -> ORDR_CONFIG
 */
@RestController
@RequestMapping("/api/v1/admin/rental-orders")
public class AdminRentalOrderController {
    private static final String P_VIEW = RentalOrderPermissionCodes.ORDER_VIEW;
    private static final String P_CONFIRM = RentalOrderPermissionCodes.ORDER_CONFIRM;
    private static final String P_REJECT = RentalOrderPermissionCodes.ORDER_REJECT;
    private static final String P_CANCEL = RentalOrderPermissionCodes.ORDER_CANCEL;
    private static final String P_FULFIL = RentalOrderPermissionCodes.ORDER_FULFIL;
    private static final String P_REFUND = RentalOrderPermissionCodes.ORDER_REFUND;
    private static final String P_CONFIG = RentalOrderPermissionCodes.ORDER_CONFIG;

    private final AuthorizationService authorizationService;
    private final RentalOrderService service;
    private final SuperAdminUtil util;

    public AdminRentalOrderController(RentalOrderService service, AuthorizationService authorizationService, SuperAdminUtil util) {
        this.authorizationService = authorizationService;
        this.service = service;
        this.util = util;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<OrderResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String fulfilmentStatus,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched",
                service.list(status, paymentStatus, fulfilmentStatus, username, orderId, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats(HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", service.stats(), 200));
    }

    @GetMapping("/stuck")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> stuck(
            @RequestParam(defaultValue = "30") int olderThanMinutes, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Stuck orders fetched", service.findStuck(olderThanMinutes), 200));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ConfigResponse>> getConfig(HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Booking config fetched", service.getConfig(), 200));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<ConfigResponse>> updateConfig(
            @Valid @RequestBody ConfigRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_CONFIG);
        return ResponseEntity.ok(ApiResponse.success("Booking config updated",
                service.upsertConfig(request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> detail(
            @PathVariable String orderId, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Order fetched", service.getDetail(orderId), 200));
    }

    @GetMapping("/{orderId}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> audit(
            @PathVariable String orderId, HttpServletRequest http) {
        authorize(http, P_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Audit history fetched", service.getAuditHistory(orderId), 200));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(
            @PathVariable String orderId, @Valid @RequestBody ConfirmRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_CONFIRM);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed",
                service.confirm(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> reject(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REJECT);
        return ResponseEntity.ok(ApiResponse.success("Order rejected",
                service.reject(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_CANCEL);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                service.cancelByAdmin(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/dispatch")
    public ResponseEntity<ApiResponse<OrderResponse>> dispatch(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_FULFIL);
        return ResponseEntity.ok(ApiResponse.success("Order dispatched",
                service.dispatch(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_FULFIL);
        return ResponseEntity.ok(ApiResponse.success("Order delivered",
                service.markDelivered(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/return")
    public ResponseEntity<ApiResponse<OrderResponse>> markReturned(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_FULFIL);
        return ResponseEntity.ok(ApiResponse.success("Order returned",
                service.markReturned(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_FULFIL);
        return ResponseEntity.ok(ApiResponse.success("Order completed",
                service.complete(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/mark-refunded")
    public ResponseEntity<ApiResponse<OrderResponse>> markRefunded(
            @PathVariable String orderId, @Valid @RequestBody RefundRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REFUND);
        return ResponseEntity.ok(ApiResponse.success("Order marked refunded",
                service.markRefunded(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/deposit-settle")
    public ResponseEntity<ApiResponse<OrderResponse>> settleDeposit(
            @PathVariable String orderId, @Valid @RequestBody DepositSettleRequest request, HttpServletRequest http) {
        AdminEntity a = authorize(http, P_REFUND);
        return ResponseEntity.ok(ApiResponse.success("Deposit settled",
                service.settleDeposit(orderId, request, a.getId(), a.getFullName(), a.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    /** JWT identity check, then the full live DB permission chain. Never one without the other. */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}
