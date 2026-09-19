package com.example.demo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the public-facing User Product API.
 * These are deliberately leaner than the admin-side ProductDto —
 * no audit fields, no deletedAt, no createdBy, and inventory is
 * presented as grouped size slots rather than raw SKU rows.
 */
public class UserProductDto {

    // ═══════════════════════════════════════════
    //  CATALOGUE LIST  (GET /api/v1/products)
    // ═══════════════════════════════════════════

    /** Lightweight card shown in catalogue grids / search results. */
    public static class ProductSummaryResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private String name;
        private String colour;
        private String occasion;
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal offerPrice;
        private BigDecimal securityDeposit;
        private String coverImageUrl;          // first image only
        private int availableSizeCount;        // # distinct sizes still AVAILABLE
        private int totalInventoryCount;       // total SKUs (all statuses)

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal perDayPrice) { this.perDayPrice = perDayPrice; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal weekendPrice) { this.weekendPrice = weekendPrice; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal offerPrice) { this.offerPrice = offerPrice; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
        public int getAvailableSizeCount() { return availableSizeCount; }
        public void setAvailableSizeCount(int availableSizeCount) { this.availableSizeCount = availableSizeCount; }
        public int getTotalInventoryCount() { return totalInventoryCount; }
        public void setTotalInventoryCount(int totalInventoryCount) { this.totalInventoryCount = totalInventoryCount; }
    }

    /** Paginated wrapper for catalogue list. */
    public static class ProductPageResponse {
        private List<ProductSummaryResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        public List<ProductSummaryResponse> getContent() { return content; }
        public void setContent(List<ProductSummaryResponse> content) { this.content = content; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    // ═══════════════════════════════════════════
    //  PRODUCT DETAIL  (GET /api/v1/products/{id})
    // ═══════════════════════════════════════════

    /** Full product detail page response. */
    public static class ProductDetailResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private String name;
        private String description;
        private String colour;
        private String occasion;
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal offerPrice;
        private BigDecimal securityDeposit;
        private List<ImageResponse> images;
        private List<SizeSlotResponse> sizes;   // inventory grouped by size
        private LocalDateTime createdAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal perDayPrice) { this.perDayPrice = perDayPrice; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal weekendPrice) { this.weekendPrice = weekendPrice; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal offerPrice) { this.offerPrice = offerPrice; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }
        public List<ImageResponse> getImages() { return images; }
        public void setImages(List<ImageResponse> images) { this.images = images; }
        public List<SizeSlotResponse> getSizes() { return sizes; }
        public void setSizes(List<SizeSlotResponse> sizes) { this.sizes = sizes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /** Single image entry inside a product detail. */
    public static class ImageResponse {
        private UUID id;
        private String imageUrl;
        private int sortOrder;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }

    /**
     * Inventory aggregated by size — tells the user which sizes exist and
     * how many units are currently rentable (AVAILABLE + active + not blocked).
     */
    public static class SizeSlotResponse {
        private String size;
        private int totalCount;
        private int availableCount;   // AVAILABLE + active + not blocked right now
        private boolean inStock;      // availableCount > 0

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getAvailableCount() { return availableCount; }
        public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
    }

    // ═══════════════════════════════════════════
    //  CATEGORIES  (GET /api/v1/products/categories)
    // ═══════════════════════════════════════════

    /** Slim category entry used for filter dropdowns on the front-end. */
    public static class CategoryResponse {
        private UUID id;
        private String name;
        private long productCount;   // # of visible+active products in this category

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getProductCount() { return productCount; }
        public void setProductCount(long productCount) { this.productCount = productCount; }
    }

    // ═══════════════════════════════════════════
    //  AVAILABILITY CHECK
    //  (GET /api/v1/products/{id}/availability)
    // ═══════════════════════════════════════════

    /** Query params for availability check — passed as @RequestParam. */
    public static class AvailabilityRequest {

        @NotNull(message = "fromDate is required")
        private LocalDateTime fromDate;

        @NotNull(message = "toDate is required")
        private LocalDateTime toDate;

        /** Optional: only check a specific size. */
        private String size;

        public LocalDateTime getFromDate() { return fromDate; }
        public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }

    /** Full availability result for a product over a requested date window. */
    public static class AvailabilityResponse {
        private UUID productId;
        private String productName;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
        private int totalAvailable;
        private List<AvailableInventoryItem> availableItems;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public LocalDateTime getFromDate() { return fromDate; }
        public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
        public int getTotalAvailable() { return totalAvailable; }
        public void setTotalAvailable(int totalAvailable) { this.totalAvailable = totalAvailable; }
        public List<AvailableInventoryItem> getAvailableItems() { return availableItems; }
        public void setAvailableItems(List<AvailableInventoryItem> availableItems) { this.availableItems = availableItems; }
    }

    /** One rentable SKU inside an AvailabilityResponse. */
    public static class AvailableInventoryItem {
        private UUID inventoryId;
        private String size;
        private String sku;
        private String condition;  // NEW | GOOD | FAIR

        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID inventoryId) { this.inventoryId = inventoryId; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
    }

    // ═══════════════════════════════════════════
    //  FILTER OPTIONS  (GET /api/v1/products/filters)
    // ═══════════════════════════════════════════

    /** All distinct filter values computed from the live catalogue. */
    public static class FilterOptionsResponse {
        private List<CategoryResponse> categories;
        private List<String> colours;
        private List<String> occasions;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;

        public List<CategoryResponse> getCategories() { return categories; }
        public void setCategories(List<CategoryResponse> categories) { this.categories = categories; }
        public List<String> getColours() { return colours; }
        public void setColours(List<String> colours) { this.colours = colours; }
        public List<String> getOccasions() { return occasions; }
        public void setOccasions(List<String> occasions) { this.occasions = occasions; }
        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    }
}
