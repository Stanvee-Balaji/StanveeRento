package com.example.demo.controller;

import com.example.demo.dto.InventoryDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.InventoryManagementService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.InventoryPermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sub-Admin: every endpoint re-validates the full DB-driven chain
 * (Admin active -> Role active -> Permission active -> Module active ->
 * Role has Permission) via AuthorizationService.checkPermission(...) before
 * touching the service. A valid JWT alone is never sufficient — same rule
 * as AdminProductController / AdminRentalCalendarController.
 *
 * Grant access by attaching INVENTORY_VIEW / INVENTORY_EXPORT to a Role
 * (PUT /api/v1/super-admin/roles/{id}/permissions), after creating the
 * INVENTORY module and both permission rows (see InventoryPermissionCodes).
 *
 * Routes (base: /api/v1/admin/inventory) mirror the Super Admin controller 1:1.
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryManagementService inventoryService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util; // reused only for getClientIp(...)

    public AdminInventoryController(InventoryManagementService inventoryService,
                                    AuthorizationService authorizationService,
                                    SuperAdminUtil util) {
        this.inventoryService = inventoryService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    // ── Dashboard / summary / breakdown ──────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<InventoryDto.DashboardResponse>> dashboard(
            @RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(required = false) Integer upcomingDays,
            HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Inventory dashboard fetched",
                inventoryService.dashboard(lowStockThreshold, upcomingDays), 200));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventoryDto.SummaryResponse>> summary(
            @RequestParam(required = false) Integer lowStockThreshold,
            HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Inventory summary fetched",
                inventoryService.summary(lowStockThreshold), 200));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<ApiResponse<List<InventoryDto.BreakdownItem>>> breakdown(
            @RequestParam String by, HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
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
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Product stock fetched",
                inventoryService.listProducts(categoryId, search, stockStatus, lowStockThreshold,
                        page, size, sortBy, sortDir), 200));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<InventoryDto.ProductInventoryDetailResponse>> product(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer lowStockThreshold,
            HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
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
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Inventory units fetched",
                inventoryService.listUnits(categoryId, productId, colour, size, condition, state, search,
                        page, pageSize), 200));
    }

    @GetMapping("/units/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryDto.UnitDetailResponse>> unit(
            @PathVariable UUID inventoryId, HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Inventory unit fetched",
                inventoryService.getUnit(inventoryId), 200));
    }

    // ── Upcoming blocks ───────────────────────────────────────────────

    @GetMapping("/upcoming-blocks")
    public ResponseEntity<ApiResponse<List<InventoryDto.BlockRow>>> upcoming(
            @RequestParam(required = false) Integer days, HttpServletRequest http) {
        authorize(http, InventoryPermissionCodes.INVENTORY_VIEW);
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
        AdminEntity admin = authorize(http, InventoryPermissionCodes.INVENTORY_EXPORT);
        byte[] bytes = inventoryService.exportUnitsCsv(categoryId, productId, colour, size, condition, state,
                search, admin.getId(), admin.getFullName(), admin.getRole().getRoleName(),
                util.getClientIp(http));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=inventory_units.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    /** JWT identity check, then the full live DB permission chain. Never one without the other. */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
}
