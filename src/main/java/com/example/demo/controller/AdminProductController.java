package com.example.demo.controller;

import com.example.demo.dto.ProductDto;
import com.example.demo.entity.AdminEntity;
import com.example.demo.service.AuthorizationService;
import com.example.demo.service.ProductBulkUploadService;
import com.example.demo.service.ProductService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.ProductPermissionCodes;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sub-Admin: every endpoint re-validates the full DB-driven chain
 * (Admin active -> Role active -> Permission active -> Module active ->
 * Role has Permission) via AuthorizationService.checkPermission(...) before
 * touching the service. A valid JWT alone is never sufficient — same rule
 * as every other Sub-Admin API in this codebase (see AdminSubscriptionPlanController).
 *
 * Grant access by attaching the relevant PRODUCT_* permission codes to a
 * Role (PUT /api/v1/super-admin/roles/{id}/permissions), not by editing
 * this file. Each code must first exist as a row under the PRODUCT module
 * (Super Admin -> Modules & Permissions) — see ProductPermissionCodes for
 * the exact spelling required.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util; // reused only for getClientIp(...)

    public AdminProductController(ProductService productService,
                                   AuthorizationService authorizationService,
                                   SuperAdminUtil util) {
        this.productService = productService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> create(
            @Valid @RequestBody ProductDto.CreateProductRequest request, HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created",
                productService.create(request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto.ProductListItemResponse>>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(name = "isActive", required = false) Boolean active,
            @RequestParam(name = "isVisible", required = false) Boolean visible,
            HttpServletRequest http) {
        authorize(http, ProductPermissionCodes.PRODUCT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Products fetched",
                productService.list(categoryId, active, visible), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        authorize(http, ProductPermissionCodes.PRODUCT_VIEW);
        return ResponseEntity.ok(ApiResponse.success("Product fetched", productService.get(id), 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateProductRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Product updated",
                productService.update(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/images")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> updateImages(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateImagesRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_MANAGE_IMAGES);
        return ResponseEntity.ok(ApiResponse.success("Product images updated",
                productService.updateImages(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> updateInventory(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateInventoryRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_MANAGE_INVENTORY);
        return ResponseEntity.ok(ApiResponse.success("Product inventory updated",
                productService.updateInventory(id, request, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> active(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.ActiveFlagRequest request,
            HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_EDIT);
        return ResponseEntity.ok(ApiResponse.success("Product active flag updated",
                productService.setActive(id, request.getActive(), admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_DELETE);
        productService.delete(id, admin.getId(), admin.getFullName(), admin.getRole().getRoleName(), util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null, 200));
    }

    /** JWT identity check, then the full live DB permission chain. Never one without the other. */
    private AdminEntity authorize(HttpServletRequest http, String permissionCode) {
        UUID adminId = authorizationService.getAuthenticatedAdminId(http);
        return authorizationService.checkPermission(adminId, permissionCode);
    }
    
    @Autowired
    ProductBulkUploadService bulkUploadService;
    
    
    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> downloadTemplate(HttpServletRequest http) {
        authorize(http, ProductPermissionCodes.PRODUCT_VIEW);
        byte[] bytes = bulkUploadService.generateTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=product_bulk_upload_template.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping(value = "/bulk-upload/preview", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductDto.BulkUploadPreviewResponse>> previewBulk(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest http) {
        authorize(http, ProductPermissionCodes.PRODUCT_BULK_UPLOAD);
        return ResponseEntity.ok(ApiResponse.success("Preview generated", bulkUploadService.preview(file), 200));
    }

    @PostMapping(value = "/bulk-upload/import", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductDto.BulkImportResponse>> importBulk(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest http) {
        AdminEntity admin = authorize(http, ProductPermissionCodes.PRODUCT_BULK_UPLOAD);
        return ResponseEntity.ok(ApiResponse.success("Bulk import completed",
                bulkUploadService.importFile(file, admin.getId(), admin.getFullName(),
                        admin.getRole().getRoleName(), util.getClientIp(http)), 200));
    }
}
