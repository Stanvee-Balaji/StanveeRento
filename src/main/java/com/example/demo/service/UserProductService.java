//package com.example.demo.service;
//
//import com.example.demo.dto.UserProductDto;
//import com.example.demo.entity.*;
//import com.example.demo.repository.*;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Read-only service for the public user-facing product catalogue.
// *
// * Rules enforced here (NOT delegated to the controller):
// *  1.  Only products where is_active = true AND is_visible = true are exposed.
// *  2.  Inventory shown in detail / availability checks must be:
// *        – is_active = true
// *        – is_blocked = false  (or blocked window doesn't overlap the requested dates)
// *        – status = 'AVAILABLE'
// *  3.  Deleted products (deletedAt IS NOT NULL) are never shown.
// *  4.  Only categories where is_active = true are returned.
// *
// * No writes are performed here; all @Transactional annotations are read-only.
// */
//@Service
//@Transactional(readOnly = true)
//public class UserProductService {
//
//    private final ProductRepository productRepository;
//    private final CategoryRepository categoryRepository;
//    private final ProductImageRepository imageRepository;
//    private final InventoryRepository inventoryRepository;
//
//    public UserProductService(
//            ProductRepository productRepository,
//            CategoryRepository categoryRepository,
//            ProductImageRepository imageRepository,
//            InventoryRepository inventoryRepository) {
//        this.productRepository = productRepository;
//        this.categoryRepository = categoryRepository;
//        this.imageRepository = imageRepository;
//        this.inventoryRepository = inventoryRepository;
//    }
//
//    // ═══════════════════════════════════════════
//    //  CATALOGUE LIST  (paginated + filtered)
//    // ═══════════════════════════════════════════
//
//    /**
//     * Returns a page of visible, active products.
//     *
//     * @param categoryId  filter by category (optional)
//     * @param colour      filter by colour (case-insensitive, optional)
//     * @param occasion    filter by occasion (case-insensitive, optional)
//     * @param search      partial name / description match (optional)
//     * @param minPrice    per-day price lower bound (optional)
//     * @param maxPrice    per-day price upper bound (optional)
//     * @param page        zero-based page number (default 0)
//     * @param size        page size 1-50 (default 12)
//     * @param sortBy      field to sort on: "name" | "perDayPrice" | "createdAt" (default "createdAt")
//     * @param sortDir     "asc" | "desc" (default "desc")
//     */
//    public UserProductDto.ProductPageResponse listProducts(
//            UUID categoryId,
//            String colour,
//            String occasion,
//            String search,
//            BigDecimal minPrice,
//            BigDecimal maxPrice,
//            int page,
//            int size,
//            String sortBy,
//            String sortDir) {
//
//        // ── sanitise pagination ──────────────────────────────────────────
//        int safePage = Math.max(page, 0);
//        int safeSize = Math.min(Math.max(size, 1), 50);
//        Sort sort = buildSort(sortBy, sortDir);
//        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
//
//        // ── build dynamic query ──────────────────────────────────────────
//        Specification<ProductEntity> spec = buildCatalogueSpec(
//                categoryId, colour, occasion, search, minPrice, maxPrice);
//
//        Page<ProductEntity> resultPage = productRepository.findAll(spec, pageable);
//
//        // ── map to response ──────────────────────────────────────────────
//        List<UserProductDto.ProductSummaryResponse> content = resultPage.getContent()
//                .stream()
//                .map(this::toSummary)
//                .collect(Collectors.toList());
//
//        UserProductDto.ProductPageResponse response = new UserProductDto.ProductPageResponse();
//        response.setContent(content);
//        response.setPage(resultPage.getNumber());
//        response.setSize(resultPage.getSize());
//        response.setTotalElements(resultPage.getTotalElements());
//        response.setTotalPages(resultPage.getTotalPages());
//        response.setHasNext(resultPage.hasNext());
//        response.setHasPrevious(resultPage.hasPrevious());
//        return response;
//    }
//
//    // ═══════════════════════════════════════════
//    //  PRODUCT DETAIL
//    // ═══════════════════════════════════════════
//
//    /**
//     * Returns the full product detail. Throws {@link ResourceNotFoundException}
//     * if the product doesn't exist, is deleted, inactive, or not visible.
//     */
//    public UserProductDto.ProductDetailResponse getProduct(UUID id) {
//        ProductEntity product = findVisibleProduct(id);
//        return toDetail(product);
//    }
//
//    // ═══════════════════════════════════════════
//    //  CATEGORIES
//    // ═══════════════════════════════════════════
//
//    /**
//     * Returns all active, non-deleted categories that have at least one
//     * visible+active product (useful for building filter dropdowns).
//     */
//    public List<UserProductDto.CategoryResponse> listCategories() {
//        return categoryRepository.findByDeletedAtIsNullAndActiveTrue()
//                .stream()
//                .map(cat -> {
//                    long count = productRepository.countByCategory_IdAndActiveTrueAndVisibleTrueAndDeletedAtIsNull(cat.getId());
//                    UserProductDto.CategoryResponse r = new UserProductDto.CategoryResponse();
//                    r.setId(cat.getId());
//                    r.setName(cat.getName());
//                    r.setProductCount(count);
//                    return r;
//                })
//                // only show categories that currently have visible products
//                .filter(r -> r.getProductCount() > 0)
//                .collect(Collectors.toList());
//    }
//
//    // ═══════════════════════════════════════════
//    //  FILTER OPTIONS
//    // ═══════════════════════════════════════════
//
//    /**
//     * Returns all distinct filter values derived from the live catalogue —
//     * colours, occasions, price bounds, and categories.
//     */
//    public UserProductDto.FilterOptionsResponse filterOptions() {
//
//        // All visible+active products (no deletedAt)
//        Specification<ProductEntity> spec = buildCatalogueSpec(null, null, null, null, null, null);
//        List<ProductEntity> all = productRepository.findAll(spec);
//
//        List<String> colours = all.stream()
//                .map(ProductEntity::getColour)
//                .filter(Objects::nonNull)
//                .distinct()
//                .sorted()
//                .collect(Collectors.toList());
//
//        List<String> occasions = all.stream()
//                .map(ProductEntity::getOccasion)
//                .filter(Objects::nonNull)
//                .distinct()
//                .sorted()
//                .collect(Collectors.toList());
//
//        BigDecimal minPrice = all.stream()
//                .map(ProductEntity::getPerDayPrice)
//                .filter(Objects::nonNull)
//                .min(BigDecimal::compareTo)
//                .orElse(BigDecimal.ZERO);
//
//        BigDecimal maxPrice = all.stream()
//                .map(ProductEntity::getPerDayPrice)
//                .filter(Objects::nonNull)
//                .max(BigDecimal::compareTo)
//                .orElse(BigDecimal.ZERO);
//
//        UserProductDto.FilterOptionsResponse response = new UserProductDto.FilterOptionsResponse();
//        response.setCategories(listCategories());
//        response.setColours(colours);
//        response.setOccasions(occasions);
//        response.setMinPrice(minPrice);
//        response.setMaxPrice(maxPrice);
//        return response;
//    }
//
//    // ═══════════════════════════════════════════
//    //  AVAILABILITY CHECK
//    // ═══════════════════════════════════════════
//
//    /**
//     * Checks which inventory items (SKUs) of a product are rentable for the
//     * requested date window.
//     *
//     * An item is considered available when ALL of the following are true:
//     *   – status == "AVAILABLE"
//     *   – is_active == true
//     *   – is_blocked == false  OR  the blocked window [blockedFrom, blockedTo]
//     *     does not overlap the requested [fromDate, toDate]
//     *
//     * @param productId  the product to check
//     * @param fromDate   rental start (inclusive)
//     * @param toDate     rental end   (inclusive)
//     * @param size       optional size filter
//     */
//    public UserProductDto.AvailabilityResponse checkAvailability(
//            UUID productId,
//            LocalDateTime fromDate,
//            LocalDateTime toDate,
//            String size) {
//
//        // Validate date order
//        if (!toDate.isAfter(fromDate)) {
//            throw new BadRequestException("toDate must be after fromDate");
//        }
//
//        ProductEntity product = findVisibleProduct(productId);
//
//        List<InventoryEntity> allInventory = inventoryRepository.findByProduct_Id(productId);
//
//        List<UserProductDto.AvailableInventoryItem> availableItems = allInventory.stream()
//                .filter(inv -> isAvailableForDates(inv, fromDate, toDate))
//                .filter(inv -> size == null || size.isBlank() || inv.getSize().equalsIgnoreCase(size.trim()))
//                .map(inv -> {
//                    UserProductDto.AvailableInventoryItem item = new UserProductDto.AvailableInventoryItem();
//                    item.setInventoryId(inv.getId());
//                    item.setSize(inv.getSize());
//                    item.setSku(inv.getSku());
//                    item.setCondition(inv.getCondition());
//                    return item;
//                })
//                .collect(Collectors.toList());
//
//        UserProductDto.AvailabilityResponse response = new UserProductDto.AvailabilityResponse();
//        response.setProductId(product.getId());
//        response.setProductName(product.getName());
//        response.setFromDate(fromDate);
//        response.setToDate(toDate);
//        response.setTotalAvailable(availableItems.size());
//        response.setAvailableItems(availableItems);
//        return response;
//    }
//
//    // ═══════════════════════════════════════════
//    //  INTERNAL HELPERS
//    // ═══════════════════════════════════════════
//
//    /**
//     * Core visibility guard — used by getProduct() and checkAvailability().
//     * Returns the entity only when: not deleted, active AND visible.
//     */
//    private ProductEntity findVisibleProduct(UUID id) {
//        return productRepository.findByIdAndDeletedAtIsNull(id)
//                .filter(ProductEntity::isActive)
//                .filter(ProductEntity::isVisible)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
//    }
//
//    /**
//     * Builds the JPA Specification for the catalogue list.
//     * Always restricts to: deletedAt IS NULL, active = true, visible = true.
//     */
//    private Specification<ProductEntity> buildCatalogueSpec(
//            UUID categoryId,
//            String colour,
//            String occasion,
//            String search,
//            BigDecimal minPrice,
//            BigDecimal maxPrice) {
//
//        return (root, query, cb) -> {
//            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
//
//            // Mandatory visibility guards
//            predicates.add(cb.isNull(root.get("deletedAt")));
//            predicates.add(cb.isTrue(root.get("active")));
//            predicates.add(cb.isTrue(root.get("visible")));
//
//            // Optional filters
//            if (categoryId != null) {
//                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
//            }
//            if (colour != null && !colour.isBlank()) {
//                predicates.add(cb.equal(
//                        cb.lower(root.get("colour")),
//                        colour.trim().toLowerCase()));
//            }
//            if (occasion != null && !occasion.isBlank()) {
//                predicates.add(cb.equal(
//                        cb.lower(root.get("occasion")),
//                        occasion.trim().toLowerCase()));
//            }
//            if (search != null && !search.isBlank()) {
//                String pattern = "%" + search.trim().toLowerCase() + "%";
//                predicates.add(cb.or(
//                        cb.like(cb.lower(root.get("name")), pattern),
//                        cb.like(cb.lower(root.get("description")), pattern)
//                ));
//            }
//            if (minPrice != null) {
//                predicates.add(cb.greaterThanOrEqualTo(root.get("perDayPrice"), minPrice));
//            }
//            if (maxPrice != null) {
//                predicates.add(cb.lessThanOrEqualTo(root.get("perDayPrice"), maxPrice));
//            }
//
//            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
//        };
//    }
//
//    /** Resolves the Sort from user-supplied strings with safe defaults. */
//    private Sort buildSort(String sortBy, String sortDir) {
//        String field = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
//            case "name"        -> "name";
//            case "perdayprice" -> "perDayPrice";
//            default            -> "createdAt";
//        };
//        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
//                ? Sort.Direction.ASC
//                : Sort.Direction.DESC;
//        return Sort.by(direction, field);
//    }
//
//    /**
//     * True when the inventory unit is currently rentable (no date window).
//     * Used inside toSummary() and toDetail() to compute availableCount.
//     */
//    private boolean isGenerallyAvailable(InventoryEntity inv) {
//        if (!inv.isActive()) return false;
//        if (inv.isBlocked()) return false;
//        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
//        return true;
//    }
//
//    /**
//     * True when the inventory unit is rentable for the given date range.
//     * Overlap check: blocked window [bF, bT] overlaps [from, to] when
//     *   bF <= toDate AND bT >= fromDate
//     */
//    private boolean isAvailableForDates(InventoryEntity inv, LocalDateTime from, LocalDateTime to) {
//        if (!inv.isActive()) return false;
//        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
//        if (inv.isBlocked()) {
//            // If blockedFrom / blockedTo are set, check overlap
//            LocalDateTime bF = inv.getBlockedFrom();
//            LocalDateTime bT = inv.getBlockedTo();
//            if (bF == null || bT == null) return false; // blocked with no dates → always blocked
//            // No overlap iff bT < from OR bF > to
//            boolean noOverlap = bT.isBefore(from) || bF.isAfter(to);
//            return noOverlap;
//        }
//        return true;
//    }
//
//    // ── Mapping helpers ────────────────────────────────────────────────
//
//    private UserProductDto.ProductSummaryResponse toSummary(ProductEntity product) {
//
//        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
//        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());
//
//        long available = inventory.stream().filter(this::isGenerallyAvailable).count();
//
//        // Distinct sizes available right now
//        long availableSizes = inventory.stream()
//                .filter(this::isGenerallyAvailable)
//                .map(InventoryEntity::getSize)
//                .distinct()
//                .count();
//
//        String coverImageUrl = images.stream()
//                .findFirst()
//                .map(ProductImageEntity::getImageUrl)
//                .orElse(null);
//
//        UserProductDto.ProductSummaryResponse r = new UserProductDto.ProductSummaryResponse();
//        r.setId(product.getId());
//        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
//        r.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
//        r.setName(product.getName());
//        r.setColour(product.getColour());
//        r.setOccasion(product.getOccasion());
//        r.setPerDayPrice(product.getPerDayPrice());
//        r.setWeekendPrice(product.getWeekendPrice());
//        r.setOfferPrice(product.getOfferPrice());
//        r.setSecurityDeposit(product.getSecurityDeposit());
//        r.setCoverImageUrl(coverImageUrl);
//        r.setAvailableSizeCount((int) availableSizes);
//        r.setTotalInventoryCount(inventory.size());
//        return r;
//    }
//    
//    // ═══════════════════════════════════════════
//    //  PRODUCT DETAILS PAGE (images + pricing + sizes + related)
//    // ═══════════════════════════════════════════
//
//    public UserProductDto.ProductFullDetailsResponse getProductDetails(
//            UUID id, LocalDateTime fromDate, LocalDateTime toDate) {
//
//        if ((fromDate == null) != (toDate == null)) {
//            throw new BadRequestException("Provide both fromDate and toDate, or neither");
//        }
//        boolean hasWindow = fromDate != null;
//        if (hasWindow && !toDate.isAfter(fromDate)) {
//            throw new BadRequestException("toDate must be after fromDate");
//        }
//
//        ProductEntity product = findVisibleProduct(id);
//        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(id);
//        List<ProductImageEntity> images = imageRepository.findByProduct_Id(id);
//
//        // ── images ──
//        List<UserProductDto.ImageInfo> imageList = new ArrayList<>();
//        for (int i = 0; i < images.size(); i++) {
//            UserProductDto.ImageInfo ii = new UserProductDto.ImageInfo();
//            ii.setId(images.get(i).getId());
//            ii.setImageUrl(images.get(i).getImageUrl());
//            ii.setSortOrder(i);
//            ii.setCover(i == 0);
//            imageList.add(ii);
//        }
//
//        // ── sizes + units ──
//        Map<String, List<InventoryEntity>> bySize = inventory.stream()
//                .filter(inv -> inv.getSize() != null)
//                .collect(Collectors.groupingBy(
//                        inv -> inv.getSize().trim(),
//                        LinkedHashMap::new,
//                        Collectors.toList()));
//
//        int totalAvailable = 0;
//        List<UserProductDto.SizeInfo> sizeList = new ArrayList<>();
//        for (Map.Entry<String, List<InventoryEntity>> e : bySize.entrySet()) {
//            UserProductDto.SizeInfo si = new UserProductDto.SizeInfo();
//            int avail = 0;
//            for (InventoryEntity inv : e.getValue()) {
//                boolean ok = hasWindow
//                        ? isAvailableForDates(inv, fromDate, toDate)
//                        : isGenerallyAvailable(inv);
//                UserProductDto.UnitInfo u = new UserProductDto.UnitInfo();
//                u.setInventoryId(inv.getId());
//                u.setSku(inv.getSku());
//                u.setCondition(inv.getCondition());
//                u.setAvailable(ok);
//                si.getUnits().add(u);
//                if (ok) avail++;
//            }
//            si.setSize(e.getKey());
//            si.setTotalCount(e.getValue().size());
//            si.setAvailableCount(avail);
//            si.setInStock(avail > 0);
//            totalAvailable += avail;
//            sizeList.add(si);
//        }
//
//        // ── pricing ──
//        BigDecimal perDay = product.getPerDayPrice();
//        BigDecimal offer = product.getOfferPrice();
//        boolean hasOffer = offer != null && offer.signum() > 0
//                && perDay != null && perDay.signum() > 0
//                && offer.compareTo(perDay) < 0;
//
//        UserProductDto.PricingInfo pricing = new UserProductDto.PricingInfo();
//        pricing.setPerDayPrice(perDay);
//        pricing.setWeekendPrice(product.getWeekendPrice());
//        pricing.setOfferPrice(offer);
//        pricing.setSecurityDeposit(product.getSecurityDeposit());
//        pricing.setEffectivePrice(hasOffer ? offer : perDay);
//        pricing.setDiscountPercent(hasOffer
//                ? perDay.subtract(offer)
//                        .multiply(BigDecimal.valueOf(100))
//                        .divide(perDay, 0, java.math.RoundingMode.HALF_UP)
//                        .intValue()
//                : 0);
//
//        // ── category ──
//        UserProductDto.CategoryInfo cat = null;
//        if (product.getCategory() != null) {
//            cat = new UserProductDto.CategoryInfo();
//            cat.setId(product.getCategory().getId());
//            cat.setName(product.getCategory().getName());
//        }
//
//        // ── related products (same category, max 6, excluding current) ──
//        List<UserProductDto.ProductSummaryResponse> related = new ArrayList<>();
//        if (product.getCategory() != null) {
//            Specification<ProductEntity> spec = buildCatalogueSpec(
//                    product.getCategory().getId(), null, null, null, null, null);
//            Page<ProductEntity> relatedPage = productRepository.findAll(
//                    spec, PageRequest.of(0, 7, Sort.by(Sort.Direction.DESC, "createdAt")));
//            related = relatedPage.getContent().stream()
//                    .filter(p -> !p.getId().equals(id))
//                    .limit(6)
//                    .map(this::toSummary)
//                    .collect(Collectors.toList());
//        }
//
//        // ── window ──
//        UserProductDto.AvailabilityWindow win = new UserProductDto.AvailabilityWindow();
//        win.setFromDate(fromDate);
//        win.setToDate(toDate);
//
//        // ── final response ──
//        UserProductDto.ProductFullDetailsResponse r = new UserProductDto.ProductFullDetailsResponse();
//        r.setId(product.getId());
//        r.setName(product.getName());
//        r.setDescription(product.getDescription());
//        r.setColour(product.getColour());
//        r.setOccasion(product.getOccasion());
//        r.setCategory(cat);
//        r.setPricing(pricing);
//        r.setImages(imageList);
//        r.setSizes(sizeList);
//        r.setTotalUnits(inventory.size());
//        r.setTotalAvailableUnits(totalAvailable);
//        r.setInStock(totalAvailable > 0);
//        r.setAvailabilityWindow(win);
//        r.setCreatedAt(product.getCreatedAt());
//        r.setRelatedProducts(related);
//        return r;
//    }
//
//    private UserProductDto.ProductDetailResponse toDetail(ProductEntity product) {
//
//        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
//        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());
//
//        // Build images list
//        List<UserProductDto.ImageResponse> imageResponses = new ArrayList<>();
//        for (int i = 0; i < images.size(); i++) {
//            ProductImageEntity img = images.get(i);
//            UserProductDto.ImageResponse ir = new UserProductDto.ImageResponse();
//            ir.setId(img.getId());
//            ir.setImageUrl(img.getImageUrl());
//            ir.setSortOrder(i);
//            imageResponses.add(ir);
//        }
//
//        // Group inventory by size → SizeSlotResponse
//        Map<String, List<InventoryEntity>> bySize = inventory.stream()
//                .collect(Collectors.groupingBy(
//                        inv -> inv.getSize().trim(),
//                        LinkedHashMap::new,
//                        Collectors.toList()));
//
//        List<UserProductDto.SizeSlotResponse> sizeSlots = bySize.entrySet().stream()
//                .map(entry -> {
//                    String sz = entry.getKey();
//                    List<InventoryEntity> units = entry.getValue();
//                    long avail = units.stream().filter(this::isGenerallyAvailable).count();
//                    UserProductDto.SizeSlotResponse slot = new UserProductDto.SizeSlotResponse();
//                    slot.setSize(sz);
//                    slot.setTotalCount(units.size());
//                    slot.setAvailableCount((int) avail);
//                    slot.setInStock(avail > 0);
//                    return slot;
//                })
//                .collect(Collectors.toList());
//
//        UserProductDto.ProductDetailResponse r = new UserProductDto.ProductDetailResponse();
//        r.setId(product.getId());
//        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
//        r.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
//        r.setName(product.getName());
//        r.setDescription(product.getDescription());
//        r.setColour(product.getColour());
//        r.setOccasion(product.getOccasion());
//        r.setPerDayPrice(product.getPerDayPrice());
//        r.setWeekendPrice(product.getWeekendPrice());
//        r.setOfferPrice(product.getOfferPrice());
//        r.setSecurityDeposit(product.getSecurityDeposit());
//        r.setImages(imageResponses);
//        r.setSizes(sizeSlots);
//        r.setCreatedAt(product.getCreatedAt());
//        return r;
//    }
//
//    // ═══════════════════════════════════════════
//    //  EXCEPTIONS (mirrors SuperAdminService pattern)
//    // ═══════════════════════════════════════════
//
//    public static class ResourceNotFoundException extends RuntimeException {
//        public ResourceNotFoundException(String message) { super(message); }
//    }
//
//    public static class BadRequestException extends RuntimeException {
//        public BadRequestException(String message) { super(message); }
//    }
//}



package com.example.demo.service;

import com.example.demo.dto.UserProductDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
 * Multi-colour aware: inventory rows carry a colour, images may carry a colour.
 *
 * Rules enforced here:
 *  1. Only products where is_active = true AND is_visible = true are exposed.
 *  2. Inventory counted as available: is_active, not blocked (or block window
 *     doesn't overlap), status = AVAILABLE.
 *  3. Deleted products are never shown.
 *  4. Only active categories are returned.
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
    //  CATALOGUE LIST
    // ═══════════════════════════════════════════

    public UserProductDto.ProductPageResponse listProducts(
            UUID categoryId, String colour, String occasion, String search,
            BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size, String sortBy, String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Specification<ProductEntity> spec = buildCatalogueSpec(
                categoryId, colour, occasion, search, minPrice, maxPrice);

        Page<ProductEntity> resultPage = productRepository.findAll(spec, pageable);

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

    public UserProductDto.ProductDetailResponse getProduct(UUID id) {
        ProductEntity product = findVisibleProduct(id);
        return toDetail(product);
    }

    // ═══════════════════════════════════════════
    //  CATEGORIES
    // ═══════════════════════════════════════════

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
                .filter(r -> r.getProductCount() > 0)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════
    //  FILTER OPTIONS
    // ═══════════════════════════════════════════

    public UserProductDto.FilterOptionsResponse filterOptions() {

        Specification<ProductEntity> spec = buildCatalogueSpec(null, null, null, null, null, null);
        List<ProductEntity> all = productRepository.findAll(spec);

        // colours: product-level colour + every active inventory colour (case-insensitive distinct)
        Map<String, String> colourMap = new TreeMap<>();
        for (ProductEntity p : all) {
            if (p.getColour() != null && !p.getColour().isBlank()) {
                colourMap.putIfAbsent(p.getColour().trim().toLowerCase(), p.getColour().trim());
            }
            for (InventoryEntity inv : inventoryRepository.findByProduct_Id(p.getId())) {
                if (inv.isActive() && inv.getColour() != null && !inv.getColour().isBlank()) {
                    colourMap.putIfAbsent(inv.getColour().trim().toLowerCase(), inv.getColour().trim());
                }
            }
        }
        List<String> colours = new ArrayList<>(colourMap.values());

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
     * @param colour optional colour filter (case-insensitive)
     * @param size   optional size filter (case-insensitive)
     */
    public UserProductDto.AvailabilityResponse checkAvailability(
            UUID productId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String size,
            String colour) {

        if (!toDate.isAfter(fromDate)) {
            throw new BadRequestException("toDate must be after fromDate");
        }

        ProductEntity product = findVisibleProduct(productId);
        List<InventoryEntity> allInventory = inventoryRepository.findByProduct_Id(productId);

        List<UserProductDto.AvailableInventoryItem> availableItems = allInventory.stream()
                .filter(inv -> isAvailableForDates(inv, fromDate, toDate))
                .filter(inv -> size == null || size.isBlank()
                        || (inv.getSize() != null && inv.getSize().trim().equalsIgnoreCase(size.trim())))
                .filter(inv -> colour == null || colour.isBlank()
                        || colourOf(inv, product).map(c -> c.equalsIgnoreCase(colour.trim())).orElse(false))
                .map(inv -> {
                    UserProductDto.AvailableInventoryItem item = new UserProductDto.AvailableInventoryItem();
                    item.setInventoryId(inv.getId());
                    item.setColour(colourOf(inv, product).orElse(null));
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

    private ProductEntity findVisibleProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .filter(ProductEntity::isActive)
                .filter(ProductEntity::isVisible)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Specification<ProductEntity> buildCatalogueSpec(
            UUID categoryId, String colour, String occasion, String search,
            BigDecimal minPrice, BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory visibility guards
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(root.get("visible")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // colour: product's own colour OR any active inventory unit of that colour
            if (colour != null && !colour.isBlank()) {
                String c = colour.trim().toLowerCase();
                Subquery<Integer> sq = query.subquery(Integer.class);
                Root<InventoryEntity> inv = sq.from(InventoryEntity.class);
                sq.select(cb.literal(1)).where(
                        cb.equal(inv.get("product").get("id"), root.get("id")),
                        cb.isTrue(inv.get("active")),
                        cb.equal(cb.lower(inv.get("colour")), c));
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("colour")), c),
                        cb.exists(sq)));
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

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

    private boolean isGenerallyAvailable(InventoryEntity inv) {
        if (!inv.isActive()) return false;
        if (inv.isBlocked()) return false;
        return "AVAILABLE".equalsIgnoreCase(inv.getStatus());
    }

    private boolean isAvailableForDates(InventoryEntity inv, LocalDateTime from, LocalDateTime to) {
        if (!inv.isActive()) return false;
        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
        if (inv.isBlocked()) {
            LocalDateTime bF = inv.getBlockedFrom();
            LocalDateTime bT = inv.getBlockedTo();
            if (bF == null || bT == null) return false; // blocked with no dates → always blocked
            return bT.isBefore(from) || bF.isAfter(to);  // no overlap
        }
        return true;
    }

    // ── colour helpers ─────────────────────────────────────────────────

    private static String clean(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** Colour of a unit; falls back to the product-level colour when the unit has none. */
    private Optional<String> colourOf(InventoryEntity inv, ProductEntity product) {
        String c = clean(inv.getColour());
        if (c == null) c = clean(product.getColour());
        return Optional.ofNullable(c);
    }

    /** Distinct colours (case-insensitive, original casing kept, order preserved). */
    private List<String> colourList(ProductEntity product, List<InventoryEntity> inventory) {
        Map<String, String> map = new LinkedHashMap<>();
        for (InventoryEntity inv : inventory) {
            if (!inv.isActive()) continue;
            String c = clean(inv.getColour());
            if (c != null) map.putIfAbsent(c.toLowerCase(), c);
        }
        if (map.isEmpty()) {
            String pc = clean(product.getColour());
            if (pc != null) map.put(pc.toLowerCase(), pc);
        }
        return new ArrayList<>(map.values());
    }

    // ── size / stock builders ──────────────────────────────────────────

    /** Groups units by size → SizeInfo (with per-unit availability). */
    private List<UserProductDto.SizeInfo> buildSizeInfos(
            List<InventoryEntity> units, ProductEntity product,
            boolean hasWindow, LocalDateTime from, LocalDateTime to) {

        Map<String, List<InventoryEntity>> bySize = units.stream()
                .filter(inv -> inv.getSize() != null)
                .collect(Collectors.groupingBy(
                        inv -> inv.getSize().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<UserProductDto.SizeInfo> out = new ArrayList<>();
        for (Map.Entry<String, List<InventoryEntity>> e : bySize.entrySet()) {
            UserProductDto.SizeInfo si = new UserProductDto.SizeInfo();
            int avail = 0;
            for (InventoryEntity inv : e.getValue()) {
                boolean ok = hasWindow ? isAvailableForDates(inv, from, to) : isGenerallyAvailable(inv);
                UserProductDto.UnitInfo u = new UserProductDto.UnitInfo();
                u.setInventoryId(inv.getId());
                u.setColour(colourOf(inv, product).orElse(null));
                u.setSku(inv.getSku());
                u.setCondition(inv.getCondition());
                u.setAvailable(ok);
                si.getUnits().add(u);
                if (ok) avail++;
            }
            si.setSize(e.getKey());
            si.setTotalCount(e.getValue().size());
            si.setAvailableCount(avail);
            si.setInStock(avail > 0);
            out.add(si);
        }
        return out;
    }

    /** Colour + size lines with counts (general availability, no date window). */
    private List<UserProductDto.StockGroup> buildStock(ProductEntity product, List<InventoryEntity> inventory) {
        Map<String, UserProductDto.StockGroup> map = new LinkedHashMap<>();
        for (InventoryEntity inv : inventory) {
            if (inv.getSize() == null) continue;
            String colour = colourOf(inv, product).orElse(null);
            String size = inv.getSize().trim();
            String key = (colour == null ? "" : colour.toLowerCase()) + "|" + size.toLowerCase();
            UserProductDto.StockGroup g = map.computeIfAbsent(key, k -> {
                UserProductDto.StockGroup ng = new UserProductDto.StockGroup();
                ng.setColour(colour);
                ng.setSize(size);
                return ng;
            });
            g.setTotalCount(g.getTotalCount() + 1);
            if (isGenerallyAvailable(inv)) g.setAvailableCount(g.getAvailableCount() + 1);
        }
        map.values().forEach(g -> g.setInStock(g.getAvailableCount() > 0));
        return new ArrayList<>(map.values());
    }

    // ── mapping helpers ────────────────────────────────────────────────

    private UserProductDto.ProductSummaryResponse toSummary(ProductEntity product) {

        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());

        long availableSizes = inventory.stream()
                .filter(this::isGenerallyAvailable)
                .map(InventoryEntity::getSize)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
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
        r.setColours(colourList(product, inventory));
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

    // ═══════════════════════════════════════════
    //  PRODUCT DETAILS PAGE (images + pricing + colours + sizes + related)
    // ═══════════════════════════════════════════

    public UserProductDto.ProductFullDetailsResponse getProductDetails(
            UUID id, LocalDateTime fromDate, LocalDateTime toDate) {

        if ((fromDate == null) != (toDate == null)) {
            throw new BadRequestException("Provide both fromDate and toDate, or neither");
        }
        boolean hasWindow = fromDate != null;
        if (hasWindow && !toDate.isAfter(fromDate)) {
            throw new BadRequestException("toDate must be after fromDate");
        }

        ProductEntity product = findVisibleProduct(id);
        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(id);
        List<ProductImageEntity> images = imageRepository.findByProduct_Id(id);

        // ── images ──
        List<UserProductDto.ImageInfo> imageList = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            UserProductDto.ImageInfo ii = new UserProductDto.ImageInfo();
            ii.setId(images.get(i).getId());
            ii.setImageUrl(images.get(i).getImageUrl());
            ii.setColour(clean(images.get(i).getColour()));
            ii.setSortOrder(i);
            ii.setCover(i == 0);
            imageList.add(ii);
        }

        // ── sizes (aggregated across colours) ──
        List<UserProductDto.SizeInfo> sizeList = buildSizeInfos(inventory, product, hasWindow, fromDate, toDate);
        int totalAvailable = sizeList.stream().mapToInt(UserProductDto.SizeInfo::getAvailableCount).sum();

        // ── colour variants ──
        Map<String, List<InventoryEntity>> byColour = inventory.stream()
                .filter(inv -> inv.getSize() != null)
                .collect(Collectors.groupingBy(
                        inv -> colourOf(inv, product).orElse(""),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<UserProductDto.ColourVariant> variants = new ArrayList<>();
        for (Map.Entry<String, List<InventoryEntity>> e : byColour.entrySet()) {
            String colourName = e.getKey().isEmpty() ? null : e.getKey();

            UserProductDto.ColourVariant v = new UserProductDto.ColourVariant();
            v.setColour(colourName);
            v.setSizes(buildSizeInfos(e.getValue(), product, hasWindow, fromDate, toDate));
            v.setTotalCount(e.getValue().size());
            v.setAvailableCount(v.getSizes().stream().mapToInt(UserProductDto.SizeInfo::getAvailableCount).sum());
            v.setInStock(v.getAvailableCount() > 0);

            // this colour's images + shared images (colour = null)
            List<UserProductDto.ImageInfo> vImages = imageList.stream()
                    .filter(ii -> ii.getColour() == null
                            || (colourName != null && ii.getColour().equalsIgnoreCase(colourName)))
                    .collect(Collectors.toList());
            v.setImages(vImages);
            variants.add(v);
        }

        // ── pricing ──
        BigDecimal perDay = product.getPerDayPrice();
        BigDecimal offer = product.getOfferPrice();
        boolean hasOffer = offer != null && offer.signum() > 0
                && perDay != null && perDay.signum() > 0
                && offer.compareTo(perDay) < 0;

        UserProductDto.PricingInfo pricing = new UserProductDto.PricingInfo();
        pricing.setPerDayPrice(perDay);
        pricing.setWeekendPrice(product.getWeekendPrice());
        pricing.setOfferPrice(offer);
        pricing.setSecurityDeposit(product.getSecurityDeposit());
        pricing.setEffectivePrice(hasOffer ? offer : perDay);
        pricing.setDiscountPercent(hasOffer
                ? perDay.subtract(offer)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(perDay, 0, java.math.RoundingMode.HALF_UP)
                        .intValue()
                : 0);

        // ── category ──
        UserProductDto.CategoryInfo cat = null;
        if (product.getCategory() != null) {
            cat = new UserProductDto.CategoryInfo();
            cat.setId(product.getCategory().getId());
            cat.setName(product.getCategory().getName());
        }

        // ── related products (same category, max 6, excluding current) ──
        List<UserProductDto.ProductSummaryResponse> related = new ArrayList<>();
        if (product.getCategory() != null) {
            Specification<ProductEntity> spec = buildCatalogueSpec(
                    product.getCategory().getId(), null, null, null, null, null);
            Page<ProductEntity> relatedPage = productRepository.findAll(
                    spec, PageRequest.of(0, 7, Sort.by(Sort.Direction.DESC, "createdAt")));
            related = relatedPage.getContent().stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(6)
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }

        // ── window ──
        UserProductDto.AvailabilityWindow win = new UserProductDto.AvailabilityWindow();
        win.setFromDate(fromDate);
        win.setToDate(toDate);

        // ── final response ──
        UserProductDto.ProductFullDetailsResponse r = new UserProductDto.ProductFullDetailsResponse();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setDescription(product.getDescription());
        r.setColour(product.getColour());
        r.setColours(colourList(product, inventory));
        r.setOccasion(product.getOccasion());
        r.setCategory(cat);
        r.setPricing(pricing);
        r.setImages(imageList);
        r.setSizes(sizeList);
        r.setColourVariants(variants);
        r.setTotalUnits(inventory.size());
        r.setTotalAvailableUnits(totalAvailable);
        r.setInStock(totalAvailable > 0);
        r.setAvailabilityWindow(win);
        r.setCreatedAt(product.getCreatedAt());
        r.setRelatedProducts(related);
        return r;
    }

    private UserProductDto.ProductDetailResponse toDetail(ProductEntity product) {

        List<InventoryEntity> inventory = inventoryRepository.findByProduct_Id(product.getId());
        List<ProductImageEntity> images  = imageRepository.findByProduct_Id(product.getId());

        List<UserProductDto.ImageResponse> imageResponses = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ProductImageEntity img = images.get(i);
            UserProductDto.ImageResponse ir = new UserProductDto.ImageResponse();
            ir.setId(img.getId());
            ir.setImageUrl(img.getImageUrl());
            ir.setColour(clean(img.getColour()));
            ir.setSortOrder(i);
            imageResponses.add(ir);
        }

        // sizes aggregated across colours
        Map<String, List<InventoryEntity>> bySize = inventory.stream()
                .filter(inv -> inv.getSize() != null)
                .collect(Collectors.groupingBy(
                        inv -> inv.getSize().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<UserProductDto.SizeSlotResponse> sizeSlots = bySize.entrySet().stream()
                .map(entry -> {
                    List<InventoryEntity> units = entry.getValue();
                    long avail = units.stream().filter(this::isGenerallyAvailable).count();
                    UserProductDto.SizeSlotResponse slot = new UserProductDto.SizeSlotResponse();
                    slot.setSize(entry.getKey());
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
        r.setColours(colourList(product, inventory));
        r.setOccasion(product.getOccasion());
        r.setPerDayPrice(product.getPerDayPrice());
        r.setWeekendPrice(product.getWeekendPrice());
        r.setOfferPrice(product.getOfferPrice());
        r.setSecurityDeposit(product.getSecurityDeposit());
        r.setImages(imageResponses);
        r.setSizes(sizeSlots);
        r.setStock(buildStock(product, inventory));
        r.setCreatedAt(product.getCreatedAt());
        return r;
    }

    // ═══════════════════════════════════════════
    //  EXCEPTIONS
    // ═══════════════════════════════════════════

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}
