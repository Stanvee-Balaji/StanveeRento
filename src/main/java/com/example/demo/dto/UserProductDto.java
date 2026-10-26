

package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the public-facing User Product API.
 * Multi-colour aware: inventory = colour + size (x quantity, one row per unit).
 */
public class UserProductDto {

    // ═══════════ CATALOGUE LIST ═══════════

    public static class ProductSummaryResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private String name;
        private String colour;
        private List<String> colours = new ArrayList<>();   // NEW
        private String occasion;
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal offerPrice;
        private BigDecimal securityDeposit;
        private String coverImageUrl;
        private int availableSizeCount;
        private int totalInventoryCount;

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
        public List<String> getColours() { return colours; }
        public void setColours(List<String> colours) { this.colours = colours; }
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

    // ═══════════ PRODUCT DETAIL (GET /{id}) ═══════════

    public static class ProductDetailResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private String name;
        private String description;
        private String colour;
        private List<String> colours = new ArrayList<>();      // NEW
        private String occasion;
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal offerPrice;
        private BigDecimal securityDeposit;
        private List<ImageResponse> images;
        private List<SizeSlotResponse> sizes;                  // aggregated by size (all colours)
        private List<StockGroup> stock = new ArrayList<>();    // NEW: colour + size lines
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
        public List<String> getColours() { return colours; }
        public void setColours(List<String> colours) { this.colours = colours; }
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
        public List<StockGroup> getStock() { return stock; }
        public void setStock(List<StockGroup> stock) { this.stock = stock; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ImageResponse {
        private UUID id;
        private String imageUrl;
        private String colour;     // NEW: null = shared by all colours
        private int sortOrder;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class SizeSlotResponse {
        private String size;
        private int totalCount;
        private int availableCount;
        private boolean inStock;

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getAvailableCount() { return availableCount; }
        public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
    }

    /** NEW: one stock line, e.g. "Blue / M" -> total 5, available 3. */
    public static class StockGroup {
        private String colour;
        private String size;
        private int totalCount;
        private int availableCount;
        private boolean inStock;

        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getAvailableCount() { return availableCount; }
        public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
    }

    // ═══════════ CATEGORIES ═══════════

    public static class CategoryResponse {
        private UUID id;
        private String name;
        private long productCount;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getProductCount() { return productCount; }
        public void setProductCount(long productCount) { this.productCount = productCount; }
    }

    // ═══════════ AVAILABILITY ═══════════

    public static class AvailabilityRequest {
        @NotNull(message = "fromDate is required")
        private LocalDateTime fromDate;
        @NotNull(message = "toDate is required")
        private LocalDateTime toDate;
        private String size;
        private String colour;   // NEW

        public LocalDateTime getFromDate() { return fromDate; }
        public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
    }

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

    public static class AvailableInventoryItem {
        private UUID inventoryId;
        private String colour;   // NEW
        private String size;
        private String sku;
        private String condition;

        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID inventoryId) { this.inventoryId = inventoryId; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
    }

    // ═══════════ FILTER OPTIONS ═══════════

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

    // ═══════════ FULL PRODUCT DETAILS (GET /{id}/details) ═══════════

    public static class CategoryInfo {
        private UUID id;
        private String name;
        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
    }

    public static class PricingInfo {
        private BigDecimal perDayPrice, weekendPrice, offerPrice, effectivePrice, securityDeposit;
        private int discountPercent;
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal v) { weekendPrice = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public BigDecimal getEffectivePrice() { return effectivePrice; }
        public void setEffectivePrice(BigDecimal v) { effectivePrice = v; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal v) { securityDeposit = v; }
        public int getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(int v) { discountPercent = v; }
    }

    public static class ImageInfo {
        private UUID id;
        private String imageUrl;
        private String colour;   // NEW: null = shared by all colours
        private int sortOrder;
        private boolean cover;
        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String v) { imageUrl = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int v) { sortOrder = v; }
        public boolean isCover() { return cover; }
        public void setCover(boolean v) { cover = v; }
    }

    public static class UnitInfo {
        private UUID inventoryId;
        private String colour;   // NEW
        private String sku, condition;
        private boolean available;
        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID v) { inventoryId = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getSku() { return sku; }
        public void setSku(String v) { sku = v; }
        public String getCondition() { return condition; }
        public void setCondition(String v) { condition = v; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean v) { available = v; }
    }

    public static class SizeInfo {
        private String size;
        private int totalCount, availableCount;
        private boolean inStock;
        private List<UnitInfo> units = new ArrayList<>();
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int v) { totalCount = v; }
        public int getAvailableCount() { return availableCount; }
        public void setAvailableCount(int v) { availableCount = v; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean v) { inStock = v; }
        public List<UnitInfo> getUnits() { return units; }
        public void setUnits(List<UnitInfo> v) { units = v; }
    }

    /** NEW: everything the page needs once the user picks a colour. */
    public static class ColourVariant {
        private String colour;
        private List<ImageInfo> images = new ArrayList<>();  // this colour's images + shared (colour = null) images
        private List<SizeInfo> sizes = new ArrayList<>();
        private int totalCount, availableCount;
        private boolean inStock;
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public List<ImageInfo> getImages() { return images; }
        public void setImages(List<ImageInfo> v) { images = v; }
        public List<SizeInfo> getSizes() { return sizes; }
        public void setSizes(List<SizeInfo> v) { sizes = v; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int v) { totalCount = v; }
        public int getAvailableCount() { return availableCount; }
        public void setAvailableCount(int v) { availableCount = v; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean v) { inStock = v; }
    }

    public static class AvailabilityWindow {
        private LocalDateTime fromDate, toDate;
        public LocalDateTime getFromDate() { return fromDate; }
        public void setFromDate(LocalDateTime v) { fromDate = v; }
        public LocalDateTime getToDate() { return toDate; }
        public void setToDate(LocalDateTime v) { toDate = v; }
    }

    public static class ProductFullDetailsResponse {
        private UUID id;
        private String name, description, colour, occasion;
        private List<String> colours = new ArrayList<>();                 // NEW
        private CategoryInfo category;
        private PricingInfo pricing;
        private List<ImageInfo> images = new ArrayList<>();
        private List<SizeInfo> sizes = new ArrayList<>();                 // aggregated by size
        private List<ColourVariant> colourVariants = new ArrayList<>();   // NEW
        private int totalUnits, totalAvailableUnits;
        private boolean inStock;
        private AvailabilityWindow availabilityWindow;
        private LocalDateTime createdAt;
        private List<ProductSummaryResponse> relatedProducts = new ArrayList<>();

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public List<String> getColours() { return colours; }
        public void setColours(List<String> v) { colours = v; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String v) { occasion = v; }
        public CategoryInfo getCategory() { return category; }
        public void setCategory(CategoryInfo v) { category = v; }
        public PricingInfo getPricing() { return pricing; }
        public void setPricing(PricingInfo v) { pricing = v; }
        public List<ImageInfo> getImages() { return images; }
        public void setImages(List<ImageInfo> v) { images = v; }
        public List<SizeInfo> getSizes() { return sizes; }
        public void setSizes(List<SizeInfo> v) { sizes = v; }
        public List<ColourVariant> getColourVariants() { return colourVariants; }
        public void setColourVariants(List<ColourVariant> v) { colourVariants = v; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int v) { totalUnits = v; }
        public int getTotalAvailableUnits() { return totalAvailableUnits; }
        public void setTotalAvailableUnits(int v) { totalAvailableUnits = v; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean v) { inStock = v; }
        public AvailabilityWindow getAvailabilityWindow() { return availabilityWindow; }
        public void setAvailabilityWindow(AvailabilityWindow v) { availabilityWindow = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public List<ProductSummaryResponse> getRelatedProducts() { return relatedProducts; }
        public void setRelatedProducts(List<ProductSummaryResponse> v) { relatedProducts = v; }
    }
}