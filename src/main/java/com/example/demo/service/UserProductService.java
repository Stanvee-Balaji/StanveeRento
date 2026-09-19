package com.example.demo.service;

import com.example.demo.dto.UserProductDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only service for the public user-facing product catalogue.
 *
 * Rules enforced here (NOT delegated to the controller):
 *  1.  Only products where is_active = true AND is_visible = true are exposed.
 *  2.  Inventory shown in detail / availability checks must be:
 *        – is_active = true
 *        – is_blocked = false  (or blocked window doesn't overlap the requested dates)
 *        – status = 'AVAILABLE'
 *  3.  Deleted products (deletedAt IS NOT NULL) are never shown.
 *  4.  Only categories where is_active = true are returned.
 *
 * No writes are performed here; all @Transactional annotations are read-only.
 */
@Service
@Transactional(readOnly = true)
public class UserProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;

    public UserProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository imageRepository,
            InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // ═══════════════════════════════════════════
    //  CATALOGUE LIST  (paginated + filtered)
    // ═══════════════════════════════════════════

    /**
     * Returns a page of visible, active products.
     *
     * @param categoryId  filter by category (optional)
     * @param colour      filter by colour (case-insensitive, optional)
     * @param occasion    filter by occasion (case-insensitive, optional)
     * @param search      partial name / description match (optional)
     * @param minPrice    per-day price lower bound (optional)
     * @param maxPrice    per-day price upper bound (optional)
     * @param page        zero-based page number (default 0)
     * @param size        page size 1-50 (default 12)
     * @param sortBy      field to sort on: "name" | "perDayPrice" | "createdAt" (default "createdAt")
     * @param sortDir     "asc" | "desc" (default "desc")
     */
    public UserProductDto.ProductPageResponse listProducts(
            UUID categoryId,
            String colour,
            String occasion,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        // ── sanitise pagination ──────────────────────────────────────────
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        // ── build dynamic query ──────────────────────────────────────────
        Specification<ProductEntity> spec = buildCatalogueSpec(
                categoryId, colour, occasion, search, minPrice, maxPrice);

        Page<ProductEntity> resultPage = productRepository.findAll(spec, pageable);

        // ── map to response ──────────────────────────────────────────────
        List<UserProductDto.ProductSummaryResponse> content = resultPage.getContent()
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        UserProductDto.ProductPageResponse response = new UserProductDto.ProductPageResponse();
        response.setContent(content);
        response.setPage(resultPage.getNumber());
        response.setSize(resultPage.getSize());
        response.setTotalElements(resultPage.getTotalElements());
        response.setTotalPages(resultPage.getTotalPages());
        response.setHasNext(resultPage.hasNext());
        response.setHasPrevious(resultPage.hasPrevious());
        return response;
    }

    // ═══════════════════════════════════════════
    //  PRODUCT DETAIL
    // ═══════════════════════════════════════════

    /**
     * Returns the full product detail. Throws {@link ResourceNotFoundException}
     * if the product doesn't exist, is deleted, inactive, or not visible.
     */
    public UserProductDto.ProductDetailResponse getProduct(UUID id) {
        ProductEntity product = findVisibleProduct(id);
        return toDetail(product);
    }

    // ═══════════════════════════════════════════
    //  CATEGORIES
    // ═══════════════════════════════════════════

    /**
     * Returns all active, non-deleted categories that have at least one
     * visible+active product (useful for building filter dropdowns).
     */
    public List<UserProductDto.CategoryResponse> listCategories() {
        return categoryRepository.findByDeletedAtIsNullAndActiveTrue()
                .stream()
                .map(cat -> {
                    long count = productRepository.countByCategory_IdAndActiveTrueAndVisibleTrueAndDeletedAtIsNull(cat.getId());
                    UserProductDto.CategoryResponse r = new UserProductDto.CategoryResponse();
                    r.setId(cat.getId());
                    r.setName(cat.getName());
                    r.setProductCount(count);
                    return r;
                })
                // only show categories that currently have visible products
                .filter(r -> r.getProductCount() > 0)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════
    //  FILTER OPTIONS
    // ═══════════════════════════════════════════

    /**
     * Returns all distinct filter values derived from the live catalogue —
     * colours, occasions, price bounds, and categories.
     */
    public UserProductDto.FilterOptionsResponse filterOptions() {

        // All visible+active products (no deletedAt)
        Specification<ProductEntity> spec = buildCatalogueSpec(null, null, null, null, null, null);
        List<ProductEntity> all = productRepository.findAll(spec);

        List<String> colours = all.stream()
                .map(ProductEntity::getColour)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> occasions = all.stream()
                .map(ProductEntity::getOccasion)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        BigDecimal minPrice = all.stream()
                .map(ProductEntity::getPerDayPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = all.stream()
                .map(ProductEntity::getPerDayPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        UserProductDto.FilterOptionsResponse response = new UserProductDto.FilterOptionsResponse();
        response.setCategories(listCategories());
        response.setColours(colours);
        response.setOccasions(occasions);
        response.setMinPrice(minPrice);
        response.setMaxPrice(maxPrice);
        return response;
    }

    // ═══════════════════════════════════════════
    //  AVAILABILITY CHECK
    // ═══════════════════════════════════════════

    /**
     * Checks which inventory items (SKUs) of a product are rentable for the
     * requested date window.
     *
     * An item is considered available when ALL of the following are true:
     *   – status == "AVAILABLE"
     *   – is_active == true
     *   – is_blocked == false  OR  the blocked window [blockedFrom, blockedTo]
     *     does not overlap the requested [fromDate, toDate]
     *
     * @param productId  the product to check
     * @param fromDate   rental start (inclusive)
     * @param toDate     rental end   (inclusive)
     * @param size       optional size filter
     */
    public UserProductDto.AvailabilityResponse checkAvailability(
            UUID productId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String size) {

        // Validate date order
        if (!toDate.isAfter(fromDate)) {
            throw new BadRequestException("toDate must be after fromDate");
        }

        ProductEntity product = findVisibleProduct(productId);

        List<InventoryEntity> allInventory = inventoryRepository.findByProduct_Id(productId);

        List<UserProductDto.AvailableInventoryItem> availableItems = allInventory.stream()
                .filter(inv -> isAvailableForDates(inv, fromDate, toDate))
                .filter(inv -> size == null || size.isBlank() || inv.getSize().equalsIgnoreCase(size.trim()))
                .map(inv -> {
                    UserProductDto.AvailableInventoryItem item = new UserProductDto.AvailableInventoryItem();
                    item.setInventoryId(inv.getId());
                    item.setSize(inv.getSize());
                    item.setSku(inv.getSku());
                    item.setCondition(inv.getCondition());
                    return item;
                })
                .collect(Collectors.toList());

        UserProductDto.AvailabilityResponse response = new UserProductDto.AvailabilityResponse();
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setTotalAvailable(availableItems.size());
        response.setAvailableItems(availableItems);
        return response;
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    /**
     * Core visibility guard — used by getProduct() and checkAvailability().
     * Returns the entity only when: not deleted, active AND visible.
     */
    private ProductEntity findVisibleProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .filter(ProductEntity::isActive)
                .filter(ProductEntity::isVisible)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    /**
     * Builds the JPA Specification for the catalogue list.
     * Always restricts to: deletedAt IS NULL, active = true, visible = true.
     */
    private Specification<ProductEntity> buildCatalogueSpec(
            UUID categoryId,
            String colour,
            String occasion,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Mandatory visibility guards
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(root.get("visible")));

            // Optional filters
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (colour != null && !colour.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("colour")),
                        colour.trim().toLowerCase()));
            }
            if (occasion != null && !occasion.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("occasion")),
                        occasion.trim().toLowerCase()));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("perDayPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("perDayPrice"), maxPrice));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /** Resolves the Sort from user-supplied strings with safe defaults. */
    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "name"        -> "name";
            case "perdayprice" -> "perDayPrice";
            default            -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    /**
     * True when the inventory unit is currently rentable (no date window).
     * Used inside toSummary() and toDetail() to compute availableCount.
     */
    private boolean isGenerallyAvailable(InventoryEntity inv) {
        if (!inv.isActive()) return false;
        if (inv.isBlocked()) return false;
        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
        return true;
    }

    /**
     * True when the inventory unit is rentable for the given date range.
     * Overlap check: blocked window [bF, bT] overlaps [from, to] when
     *   bF <= toDate AND bT >= fromDate
     */
    private boolean isAvailableForDates(InventoryEntity inv, LocalDateTime from, LocalDateTime to) {
        if (!inv.isActive()) return false;
        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
        if (inv.isBlocked()) {
            // If blockedFrom / blockedTo are set, check overlap
            LocalDateTime bF = inv.getBlockedFrom();
            LocalDateTime bT = inv.getBlockedTo();
            if (bF == null || bT == null) return false; // blocked with no dates → always blocked
            // No overlap iff bT < from OR bF > to
            boolean noOverlap = bT.isBefore(from) || bF.isAfter(to);
            return noOverlap;
        }
        return true;
    }

    // ── Mapping helpers ────────────────────────────────────────────────

    private UserProductDto.ProductSummaryResponse toSummary(ProductEntity product) {

        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());

        long available = inventory.stream().filter(this::isGenerallyAvailable).count();

        // Distinct sizes available right now
        long availableSizes = inventory.stream()
                .filter(this::isGenerallyAvailable)
                .map(InventoryEntity::getSize)
                .distinct()
                .count();

        String coverImageUrl = images.stream()
                .findFirst()
                .map(ProductImageEntity::getImageUrl)
                .orElse(null);

        UserProductDto.ProductSummaryResponse r = new UserProductDto.ProductSummaryResponse();
        r.setId(product.getId());
        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        r.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        r.setName(product.getName());
        r.setColour(product.getColour());
        r.setOccasion(product.getOccasion());
        r.setPerDayPrice(product.getPerDayPrice());
        r.setWeekendPrice(product.getWeekendPrice());
        r.setOfferPrice(product.getOfferPrice());
        r.setSecurityDeposit(product.getSecurityDeposit());
        r.setCoverImageUrl(coverImageUrl);
        r.setAvailableSizeCount((int) availableSizes);
        r.setTotalInventoryCount(inventory.size());
        return r;
    }

    private UserProductDto.ProductDetailResponse toDetail(ProductEntity product) {

        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());

        // Build images list
        List<UserProductDto.ImageResponse> imageResponses = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ProductImageEntity img = images.get(i);
            UserProductDto.ImageResponse ir = new UserProductDto.ImageResponse();
            ir.setId(img.getId());
            ir.setImageUrl(img.getImageUrl());
            ir.setSortOrder(i);
            imageResponses.add(ir);
        }

        // Group inventory by size → SizeSlotResponse
        Map<String, List<InventoryEntity>> bySize = inventory.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getSize().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<UserProductDto.SizeSlotResponse> sizeSlots = bySize.entrySet().stream()
                .map(entry -> {
                    String sz = entry.getKey();
                    List<InventoryEntity> units = entry.getValue();
                    long avail = units.stream().filter(this::isGenerallyAvailable).count();
                    UserProductDto.SizeSlotResponse slot = new UserProductDto.SizeSlotResponse();
                    slot.setSize(sz);
                    slot.setTotalCount(units.size());
                    slot.setAvailableCount((int) avail);
                    slot.setInStock(avail > 0);
                    return slot;
                })
                .collect(Collectors.toList());

        UserProductDto.ProductDetailResponse r = new UserProductDto.ProductDetailResponse();
        r.setId(product.getId());
        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        r.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        r.setName(product.getName());
        r.setDescription(product.getDescription());
        r.setColour(product.getColour());
        r.setOccasion(product.getOccasion());
        r.setPerDayPrice(product.getPerDayPrice());
        r.setWeekendPrice(product.getWeekendPrice());
        r.setOfferPrice(product.getOfferPrice());
        r.setSecurityDeposit(product.getSecurityDeposit());
        r.setImages(imageResponses);
        r.setSizes(sizeSlots);
        r.setCreatedAt(product.getCreatedAt());
        return r;
    }

    // ═══════════════════════════════════════════
    //  EXCEPTIONS (mirrors SuperAdminService pattern)
    // ═══════════════════════════════════════════

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}
