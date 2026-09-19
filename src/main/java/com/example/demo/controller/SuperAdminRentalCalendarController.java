package com.example.demo.controller;

import com.example.demo.dto.RentalCalendarDto;
import com.example.demo.service.RentalCalendarService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Super Admin: full, unrestricted access to Rental Calendar & Availability —
 * same trust model as SuperAdminProductController /
 * SuperAdminSubscriptionPlanController. No permission chain check needed
 * since there is exactly one Super Admin; they inherit everything Admin can
 * do here plus nothing further is needed at this module's level.
 */
@RestController
@RequestMapping("/api/v1/super-admin/rental-calendar")
public class SuperAdminRentalCalendarController {

    private final RentalCalendarService calendarService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService; // only used to resolve actor's real fullName for audit

    public SuperAdminRentalCalendarController(RentalCalendarService calendarService, SuperAdminUtil util,
                                               SuperAdminService superAdminService) {
        this.calendarService = calendarService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    // ── Availability ─────────────────────────────────────────────────

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<RentalCalendarDto.AvailabilityResponse>> availability(
            @RequestParam UUID productId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Availability fetched",
                calendarService.getAvailability(productId, from, to), 200));
    }

    @PostMapping("/validate-booking")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ValidateBookingResponse>> validateBooking(
            @Valid @RequestBody RentalCalendarDto.ValidateBookingRequest request, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Booking window validated",
                calendarService.validateBooking(request), 200));
    }

    // ── Blocks ────────────────────────────────────────────────────────

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> listBlocks(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID inventoryId,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Blocks fetched",
                calendarService.listActiveBlocks(productId, inventoryId), 200));
    }

    @PostMapping("/blocks/by-ids")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> blockByIds(
            @Valid @RequestBody RentalCalendarDto.BlockInventoryRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Inventory blocked",
                calendarService.blockByIds(request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 201));
    }

    @PostMapping("/blocks/by-quantity")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> blockByQuantity(
            @Valid @RequestBody RentalCalendarDto.BlockByQuantityRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Inventory blocked",
                calendarService.blockByQuantity(request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 201));
    }

    @PatchMapping("/blocks/{blockId}/unblock")
    public ResponseEntity<ApiResponse<RentalCalendarDto.BlockResponse>> unblock(
            @PathVariable UUID blockId,
            @RequestBody(required = false) RentalCalendarDto.UnblockRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory unblocked",
                calendarService.unblock(blockId, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    // ── Config ────────────────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.ConfigResponse>>> listConfigs(HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Configs fetched", calendarService.listConfigs(), 200));
    }

    @GetMapping("/config/resolve")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ConfigResponse>> resolveConfig(
            @RequestParam UUID productId, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Resolved config fetched",
                calendarService.resolveConfig(productId), 200));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ConfigResponse>> upsertConfig(
            @Valid @RequestBody RentalCalendarDto.UpsertConfigRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Config saved",
                calendarService.upsertConfig(request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    // Real lookup — writes the actual Super Admin's name into audit_logs
    // instead of the literal string "SUPER_ADMIN".
    private String actorName(UUID actorId) {
        return superAdminService.profile(actorId).getFullName();
    }
}
