package com.example.demo.controller;

import com.example.demo.dto.InventoryDto;
import com.example.demo.service.InventoryManagementService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Super Admin: full, unrestricted, READ-ONLY access to the Inventory dashboard —
 * same trust model as SuperAdminProductController / SuperAdminRentalCalendarController.
 * No permission chain check needed since there is exactly one Super Admin.
 *
 * Routes (base: /api/v1/super-admin/inventory):
 *   GET /dashboard                 summary + charts + alerts in one call
 *   GET /summary                   top cards only
 *   GET /breakdown?by=             category | colour | size | condition | status
 *   GET /products                  stock per product (filters, sort, pagination)
 *   GET /products/{productId}      product stock detail (matrix, units, active blocks)
 *   GET /units                     every physical unit (filters, pagination)
 *   GET /units/{inventoryId}       unit detail + block history
 *   GET /upcoming-blocks?days=     blocks starting in the next N days
 *   GET /export/units              CSV download (same filters as /units)
 */
@RestController
@RequestMapping("/api/v1/super-admin/inventory")
public class SuperAdminInventoryController {

    private final InventoryManagementService inventoryService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService; // only used to resolve actor's real fullName for audit

    public SuperAdminInventoryController(InventoryManagementService inventoryService, SuperAdminUtil util,
                                         SuperAdminService superAdminService) {
        this.inventoryService = inventoryService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    // ── Dashboard / summary / breakdown ──────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<InventoryDto.DashboardResponse>> dashboard(
            @RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(required = false) Integer upcomingDays,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory dashboard fetched",
                inventoryService.dashboard(lowStockThreshold, upcomingDays), 200));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventoryDto.SummaryResponse>> summary(
            @RequestParam(required = false) Integer lowStockThreshold,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory summary fetched",
                inventoryService.summary(lowStockThreshold), 200));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<ApiResponse<List<InventoryDto.BreakdownItem>>> breakdown(
            @RequestParam String by, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory breakdown fetched",
                inventoryService.breakdown(by), 200));
    }

    // ── Products ──────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<InventoryDto.PageResponse<InventoryDto.ProductStockRow>>> products(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product stock fetched",
                inventoryService.listProducts(categoryId, search, stockStatus, lowStockThreshold,
                        page, size, sortBy, sortDir), 200));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<InventoryDto.ProductInventoryDetailResponse>> product(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer lowStockThreshold,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product inventory fetched",
                inventoryService.getProduct(productId, lowStockThreshold), 200));
    }

    // ── Units ─────────────────────────────────────────────────────────

    @GetMapping("/units")
    public ResponseEntity<ApiResponse<InventoryDto.PageResponse<InventoryDto.UnitRow>>> units(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory units fetched",
                inventoryService.listUnits(categoryId, productId, colour, size, condition, state, search,
                        page, pageSize), 200));
    }

    @GetMapping("/units/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryDto.UnitDetailResponse>> unit(
            @PathVariable UUID inventoryId, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Inventory unit fetched",
                inventoryService.getUnit(inventoryId), 200));
    }

    // ── Upcoming blocks ───────────────────────────────────────────────

    @GetMapping("/upcoming-blocks")
    public ResponseEntity<ApiResponse<List<InventoryDto.BlockRow>>> upcoming(
            @RequestParam(required = false) Integer days, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Upcoming blocks fetched",
                inventoryService.upcomingBlocks(days), 200));
    }

    // ── Export ────────────────────────────────────────────────────────

    @GetMapping("/export/units")
    public ResponseEntity<byte[]> exportUnits(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        byte[] bytes = inventoryService.exportUnitsCsv(categoryId, productId, colour, size, condition, state,
                search, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=inventory_units.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // Real lookup — writes the actual Super Admin's name into audit_logs
    // instead of the literal string "SUPER_ADMIN".
    private String actorName(UUID actorId) {
        return superAdminService.profile(actorId).getFullName();
    }
}
