package com.example.demo.controller;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.RentalOrderDto.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Super Admin rental order management: unrestricted, same endpoints as the Sub-Admin controller.
 *   reads -> ORDR_VIEW | confirm -> ORDR_CONFIRM | reject -> ORDR_REJECT | cancel -> ORDR_CANCEL
 *   dispatch/deliver/return/complete -> ORDR_FULFIL | mark-refunded/deposit-settle -> ORDR_REFUND | config PUT -> ORDR_CONFIG
 */
@RestController
@RequestMapping("/api/v1/super-admin/rental-orders")
public class SuperAdminRentalOrderController {
    private static final String ROLE = "SUPER_ADMIN";

    private final RentalOrderService service;
    private final SuperAdminUtil util;

    public SuperAdminRentalOrderController(RentalOrderService service, SuperAdminUtil util) {

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
        return ResponseEntity.ok(ApiResponse.success("Orders fetched",
                service.list(status, paymentStatus, fulfilmentStatus, username, orderId, from, to, page, size), 200));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats(HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", service.stats(), 200));
    }

    @GetMapping("/stuck")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> stuck(
            @RequestParam(defaultValue = "30") int olderThanMinutes, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Stuck orders fetched", service.findStuck(olderThanMinutes), 200));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ConfigResponse>> getConfig(HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Booking config fetched", service.getConfig(), 200));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<ConfigResponse>> updateConfig(
            @Valid @RequestBody ConfigRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Booking config updated",
                service.upsertConfig(request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> detail(
            @PathVariable String orderId, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order fetched", service.getDetail(orderId), 200));
    }

    @GetMapping("/{orderId}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> audit(
            @PathVariable String orderId, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Audit history fetched", service.getAuditHistory(orderId), 200));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(
            @PathVariable String orderId, @Valid @RequestBody ConfirmRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order confirmed",
                service.confirm(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> reject(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order rejected",
                service.reject(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderId, @Valid @RequestBody ReasonRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                service.cancelByAdmin(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/dispatch")
    public ResponseEntity<ApiResponse<OrderResponse>> dispatch(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order dispatched",
                service.dispatch(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order delivered",
                service.markDelivered(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/return")
    public ResponseEntity<ApiResponse<OrderResponse>> markReturned(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order returned",
                service.markReturned(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order completed",
                service.complete(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/mark-refunded")
    public ResponseEntity<ApiResponse<OrderResponse>> markRefunded(
            @PathVariable String orderId, @Valid @RequestBody RefundRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Order marked refunded",
                service.markRefunded(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    @PostMapping("/{orderId}/deposit-settle")
    public ResponseEntity<ApiResponse<OrderResponse>> settleDeposit(
            @PathVariable String orderId, @Valid @RequestBody DepositSettleRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Deposit settled",
                service.settleDeposit(orderId, request, actorId(), actorName(), ROLE, util.getClientIp(http)), 200));
    }

    // ── ADAPT: same two helpers as SuperAdminSubscriptionPurchaseController
    private String actorName() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SUPER_ADMIN" : a.getName();
    }

    private UUID actorId() {
        try { return UUID.fromString(actorName()); } catch (Exception e) { return null; }
    }
}
