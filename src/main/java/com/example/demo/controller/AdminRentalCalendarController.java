package com.example.demo.controller;

import com.example.demo.dto.RentalCalendarDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.RentalCalendarService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.CalendarPermissionCodes;
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
 * Sub-Admin: Rental Calendar & Availability, gated per-endpoint by
 * CalendarPermissionCodes via AuthorizationService.checkPermission(...),
 * re-checked live against role_permissions on every call — same pattern as
 * AdminProductController.
 */
@RestController
@RequestMapping("/api/v1/admin/rental-calendar")
public class AdminRentalCalendarController {

    private final RentalCalendarService calendarService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util;

    public AdminRentalCalendarController(RentalCalendarService calendarService,
                                          AuthorizationService authorizationService,
                                          SuperAdminUtil util) {
        this.calendarService = calendarService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    // ── Availability ─────────────────────────────────────────────────

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<RentalCalendarDto.AvailabilityResponse>> availability(
            @RequestParam UUID productId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            HttpServletRequest http) {
        authorize(http, CalendarPermissionCodes.CALENDAR_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Availability fetched",
                calendarService.getAvailability(productId, from, to), 200));
    }

    @PostMapping("/validate-booking")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ValidateBookingResponse>> validateBooking(
            @Valid @RequestBody RentalCalendarDto.ValidateBookingRequest request, HttpServletRequest http) {
        authorize(http, CalendarPermissionCodes.CALENDAR_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Booking window validated",
                calendarService.validateBooking(request), 200));
    }

    // ── Blocks ────────────────────────────────────────────────────────

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> listBlocks(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID inventoryId,
            HttpServletRequest http) {
        authorize(http, CalendarPermissionCodes.CALENDAR_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Blocks fetched",
                calendarService.listActiveBlocks(productId, inventoryId), 200));
    }

    @PostMapping("/blocks/by-ids")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> blockByIds(
            @Valid @RequestBody RentalCalendarDto.BlockInventoryRequest request, HttpServletRequest http) {
        Actor actor = authorize(http, CalendarPermissionCodes.CALENDAR_BLOCK);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Inventory blocked",
                calendarService.blockByIds(request, actor.id, actor.name, "ADMIN", util.getClientIp(http)), 201));
    }

    @PostMapping("/blocks/by-quantity")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.BlockResponse>>> blockByQuantity(
            @Valid @RequestBody RentalCalendarDto.BlockByQuantityRequest request, HttpServletRequest http) {
        Actor actor = authorize(http, CalendarPermissionCodes.CALENDAR_BLOCK);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Inventory blocked",
                calendarService.blockByQuantity(request, actor.id, actor.name, "ADMIN", util.getClientIp(http)), 201));
    }

    @PatchMapping("/blocks/{blockId}/unblock")
    public ResponseEntity<ApiResponse<RentalCalendarDto.BlockResponse>> unblock(
            @PathVariable UUID blockId,
            @RequestBody(required = false) RentalCalendarDto.UnblockRequest request,
            HttpServletRequest http) {
        Actor actor = authorize(http, CalendarPermissionCodes.CALENDAR_UNBLOCK);
        return ResponseEntity.ok(ApiResponse.success("Inventory unblocked",
                calendarService.unblock(blockId, request, actor.id, actor.name, "ADMIN", util.getClientIp(http)), 200));
    }

    // ── Config ────────────────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<RentalCalendarDto.ConfigResponse>>> listConfigs(HttpServletRequest http) {
        authorize(http, CalendarPermissionCodes.CALENDAR_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Configs fetched", calendarService.listConfigs(), 200));
    }

    @GetMapping("/config/resolve")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ConfigResponse>> resolveConfig(
            @RequestParam UUID productId, HttpServletRequest http) {
        authorize(http, CalendarPermissionCodes.CALENDAR_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Resolved config fetched",
                calendarService.resolveConfig(productId), 200));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<RentalCalendarDto.ConfigResponse>> upsertConfig(
            @Valid @RequestBody RentalCalendarDto.UpsertConfigRequest request, HttpServletRequest http) {
        Actor actor = authorize(http, CalendarPermissionCodes.CALENDAR_CONFIG);
        return ResponseEntity.ok(ApiResponse.success("Config saved",
                calendarService.upsertConfig(request, actor.id, actor.name, "ADMIN", util.getClientIp(http)), 200));
    }

    // ── helpers ───────────────────────────────────────────────────────

    private record Actor(UUID id, String name) {}

    private Actor authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        AdminEntity admin = authorizationService.checkPermission(adminId, permissionCode);
        return new Actor(admin.getId(), admin.getFullName());
    }
}
