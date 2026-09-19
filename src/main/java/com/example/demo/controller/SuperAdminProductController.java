package com.example.demo.controller;

import com.example.demo.dto.ProductDto;
import com.example.demo.service.ProductBulkUploadService;
import com.example.demo.service.ProductService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
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
 * Super Admin: full, unrestricted CRUD over Products — same trust model as
 * Roles/Modules/Permissions/SubscriptionPlan in SuperAdminController. No
 * permission chain check needed since there is exactly one Super Admin.
 */
@RestController
@RequestMapping("/api/v1/super-admin/products")
public class SuperAdminProductController {

    private final ProductService productService;
    private final SuperAdminUtil util;
    private final SuperAdminService superAdminService; // only used to resolve actor's real fullName for audit

    public SuperAdminProductController(ProductService productService, SuperAdminUtil util,
                                        SuperAdminService superAdminService) {
        this.productService = productService;
        this.util = util;
        this.superAdminService = superAdminService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> create(
            @Valid @RequestBody ProductDto.CreateProductRequest request, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created",
                productService.create(request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto.ProductListItemResponse>>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(name = "isActive", required = false) Boolean active,
            @RequestParam(name = "isVisible", required = false) Boolean visible,
            HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Products fetched",
                productService.list(categoryId, active, visible), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> get(
            @PathVariable UUID id, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product fetched", productService.get(id), 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateProductRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product updated",
                productService.update(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/images")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> updateImages(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateImagesRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product images updated",
                productService.updateImages(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PutMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> updateInventory(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.UpdateInventoryRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product inventory updated",
                productService.updateInventory(id, request, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<ProductDto.ProductResponse>> active(
            @PathVariable UUID id, @Valid @RequestBody ProductDto.ActiveFlagRequest request,
            HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Product active flag updated",
                productService.setActive(id, request.getActive(), actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        productService.delete(id, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null, 200));
    }

    // Real lookup — writes the actual Super Admin's name into audit_logs
    // instead of the literal string "SUPER_ADMIN".
    private String actorName(UUID actorId) {
        return superAdminService.profile(actorId).getFullName();
    }
    
    @Autowired
    ProductBulkUploadService bulkUploadService;
    
    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> downloadTemplate(HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        byte[] bytes = bulkUploadService.generateTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=product_bulk_upload_template.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping(value = "/bulk-upload/preview", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductDto.BulkUploadPreviewResponse>> previewBulk(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest http) {
        util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Preview generated", bulkUploadService.preview(file), 200));
    }

    @PostMapping(value = "/bulk-upload/import", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductDto.BulkImportResponse>> importBulk(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest http) {
        UUID actorId = util.getAuthenticatedSuperAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Bulk import completed",
                bulkUploadService.importFile(file, actorId, actorName(actorId), "SUPER_ADMIN", util.getClientIp(http)), 200));
    }
}
