package com.example.demo.controller;

import com.example.demo.dto.UserProductDto;
import com.example.demo.service.UserProductService;
import com.example.demo.util.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public-facing REST API for the rental product catalogue.
 *
 * Auth model
 * ──────────
 * All endpoints here are intentionally PUBLIC (no JWT required).
 * Users can browse and search the catalogue without signing in.
 * If you later need user-scoped features (wishlist, personalised
 * recommendations, booking), add a separate @PreAuthorize / JWT
 * filter in the SecurityConfig for those routes only — keep this
 * controller public.
 *
 * Visibility contract
 * ───────────────────
 * Only products where is_active = true AND is_visible = true are
 * ever returned. The service layer enforces this; the controller
 * just forwards parameters.
 *
 * Base path: /api/v1/products
 *
 * Endpoints
 * ─────────
 *  GET  /api/v1/products                   → paginated catalogue
 *  GET  /api/v1/products/{id}              → product detail
 *  GET  /api/v1/products/{id}/availability → date-range availability
 *  GET  /api/v1/products/categories        → active categories (for filters)
 *  GET  /api/v1/products/filters           → all filter options in one call
 */
@RestController
@RequestMapping("/api/v1/products")
public class UserProductController {

    private final UserProductService productService;

    public UserProductController(UserProductService productService) {
        this.productService = productService;
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/products
    //  Paginated, filtered catalogue listing
    // ─────────────────────────────────────────────

    /**
     * Returns a page of active + visible products.
     *
     * Query params (all optional):
     *   categoryId  UUID    – filter by category
     *   colour      String  – filter by colour (case-insensitive)
     *   occasion    String  – filter by occasion (case-insensitive)
     *   search      String  – partial match on name or description
     *   minPrice    Number  – per-day price ≥ minPrice
     *   maxPrice    Number  – per-day price ≤ maxPrice
     *   page        int     – zero-based page index (default 0)
     *   size        int     – items per page, max 50 (default 12)
     *   sortBy      String  – name | perDayPrice | createdAt (default createdAt)
     *   sortDir     String  – asc | desc (default desc)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProductDto.ProductPageResponse>> listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        UserProductDto.ProductPageResponse result = productService.listProducts(
                categoryId, colour, occasion, search,
                minPrice, maxPrice,
                page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success("Products fetched", result, 200));
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/products/categories
    //  Active categories that have visible products
    //  ⚠ Must be declared BEFORE /{id} to avoid
    //    Spring treating "categories" as a UUID path variable.
    // ─────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<UserProductDto.CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched", productService.listCategories(), 200));
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/products/filters
    //  All filter options in a single call
    //  (colours, occasions, price bounds, categories)
    //  ⚠ Must also be before /{id}.
    // ─────────────────────────────────────────────

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<UserProductDto.FilterOptionsResponse>> filterOptions() {
        return ResponseEntity.ok(
                ApiResponse.success("Filter options fetched", productService.filterOptions(), 200));
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/products/{id}
    //  Single product with full detail + size slots
    // ─────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProductDto.ProductDetailResponse>> getProduct(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success("Product fetched", productService.getProduct(id), 200));
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/products/{id}/availability
    //  Check which SKUs are free for a date window
    // ─────────────────────────────────────────────

    /**
     * Query params:
     *   fromDate  String (ISO-8601 LocalDateTime, e.g. 2025-10-01T10:00:00) – required
     *   toDate    String (ISO-8601 LocalDateTime, e.g. 2025-10-05T10:00:00) – required
     *   size      String – optional size filter (e.g. "M", "XL")
     *
     * Response: list of rentable SKUs for the requested window.
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<UserProductDto.AvailabilityResponse>> checkAvailability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String size) {

        UserProductDto.AvailabilityResponse result =
                productService.checkAvailability(id, fromDate, toDate, size);

        return ResponseEntity.ok(ApiResponse.success("Availability fetched", result, 200));
    }
}
