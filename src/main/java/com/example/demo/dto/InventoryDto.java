package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for the Super Admin / Admin Inventory Management section
 * (read-only dashboard, stock lists, unit details, breakdowns, export).
 */
public final class InventoryDto {

    private InventoryDto() {}

    // ═══════════════════════════════════════════
    //  GENERIC PAGE
    // ═══════════════════════════════════════════

    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        public List<T> getContent() { return content; }
        public void setContent(List<T> v) { content = v; }
        public int getPage() { return page; }
        public void setPage(int v) { page = v; }
        public int getSize() { return size; }
        public void setSize(int v) { size = v; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long v) { totalElements = v; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int v) { totalPages = v; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean v) { hasNext = v; }
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean v) { hasPrevious = v; }
    }

    // ═══════════════════════════════════════════
    //  SUMMARY CARDS
    // ═══════════════════════════════════════════

    public static class SummaryResponse {
        private LocalDateTime generatedAt;

        // products
        private int totalProducts;
        private int activeProducts;
        private int visibleProducts;
        private int featuredProducts;
        private int newArrivalProducts;

        // units (one inventory row = one physical unit)
        private int totalUnits;
        private int activeUnits;
        private int availableUnits;
        private int rentedUnits;
        private int blockedUnits;
        private int cleaningUnits;
        private int damagedUnits;
        private int inactiveUnits;

        // alerts
        private int outOfStockProducts;
        private int lowStockProducts;
        private int lowStockThreshold;
        private int upcomingBlocks7d;
        private int blocksEndingToday;

        /** (rented + blocked) / active units * 100 */
        private double utilizationPercent;

        /** NEW / GOOD / FAIR / POOR -> unit count */
        private Map<String, Integer> unitsByCondition;

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime v) { generatedAt = v; }
        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int v) { totalProducts = v; }
        public int getActiveProducts() { return activeProducts; }
        public void setActiveProducts(int v) { activeProducts = v; }
        public int getVisibleProducts() { return visibleProducts; }
        public void setVisibleProducts(int v) { visibleProducts = v; }
        public int getFeaturedProducts() { return featuredProducts; }
        public void setFeaturedProducts(int v) { featuredProducts = v; }
        public int getNewArrivalProducts() { return newArrivalProducts; }
        public void setNewArrivalProducts(int v) { newArrivalProducts = v; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int v) { totalUnits = v; }
        public int getActiveUnits() { return activeUnits; }
        public void setActiveUnits(int v) { activeUnits = v; }
        public int getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(int v) { availableUnits = v; }
        public int getRentedUnits() { return rentedUnits; }
        public void setRentedUnits(int v) { rentedUnits = v; }
        public int getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(int v) { blockedUnits = v; }
        public int getCleaningUnits() { return cleaningUnits; }
        public void setCleaningUnits(int v) { cleaningUnits = v; }
        public int getDamagedUnits() { return damagedUnits; }
        public void setDamagedUnits(int v) { damagedUnits = v; }
        public int getInactiveUnits() { return inactiveUnits; }
        public void setInactiveUnits(int v) { inactiveUnits = v; }
        public int getOutOfStockProducts() { return outOfStockProducts; }
        public void setOutOfStockProducts(int v) { outOfStockProducts = v; }
        public int getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(int v) { lowStockProducts = v; }
        public int getLowStockThreshold() { return lowStockThreshold; }
        public void setLowStockThreshold(int v) { lowStockThreshold = v; }
        public int getUpcomingBlocks7d() { return upcomingBlocks7d; }
        public void setUpcomingBlocks7d(int v) { upcomingBlocks7d = v; }
        public int getBlocksEndingToday() { return blocksEndingToday; }
        public void setBlocksEndingToday(int v) { blocksEndingToday = v; }
        public double getUtilizationPercent() { return utilizationPercent; }
        public void setUtilizationPercent(double v) { utilizationPercent = v; }
        public Map<String, Integer> getUnitsByCondition() { return unitsByCondition; }
        public void setUnitsByCondition(Map<String, Integer> v) { unitsByCondition = v; }
    }

    // ═══════════════════════════════════════════
    //  BREAKDOWN (charts)
    // ═══════════════════════════════════════════

    /** One bar / slice: e.g. key = "Blue" (by colour) or "Sherwani" (by category). */
    public static class BreakdownItem {
        private String key;
        private int totalUnits;
        private int activeUnits;
        private int availableUnits;
        private int rentedUnits;
        private int blockedUnits;
        /** cleaning + damaged + inactive */
        private int otherUnits;
        private double utilizationPercent;

        public String getKey() { return key; }
        public void setKey(String v) { key = v; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int v) { totalUnits = v; }
        public int getActiveUnits() { return activeUnits; }
        public void setActiveUnits(int v) { activeUnits = v; }
        public int getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(int v) { availableUnits = v; }
        public int getRentedUnits() { return rentedUnits; }
        public void setRentedUnits(int v) { rentedUnits = v; }
        public int getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(int v) { blockedUnits = v; }
        public int getOtherUnits() { return otherUnits; }
        public void setOtherUnits(int v) { otherUnits = v; }
        public double getUtilizationPercent() { return utilizationPercent; }
        public void setUtilizationPercent(double v) { utilizationPercent = v; }
    }

    // ═══════════════════════════════════════════
    //  BLOCKS
    // ═══════════════════════════════════════════

    public static class BlockRow {
        private UUID id;
        private UUID inventoryId;
        private String sku;
        private UUID productId;
        private String productName;
        private String colour;
        private String size;
        private String blockType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String notes;
        private boolean active;
        private String createdByType;
        private LocalDateTime createdAt;
        private LocalDateTime releasedAt;
        private String releaseNotes;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID v) { inventoryId = v; }
        public String getSku() { return sku; }
        public void setSku(String v) { sku = v; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public String getProductName() { return productName; }
        public void setProductName(String v) { productName = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public String getBlockType() { return blockType; }
        public void setBlockType(String v) { blockType = v; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate v) { startDate = v; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate v) { endDate = v; }
        public String getNotes() { return notes; }
        public void setNotes(String v) { notes = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
        public String getCreatedByType() { return createdByType; }
        public void setCreatedByType(String v) { createdByType = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getReleasedAt() { return releasedAt; }
        public void setReleasedAt(LocalDateTime v) { releasedAt = v; }
        public String getReleaseNotes() { return releaseNotes; }
        public void setReleaseNotes(String v) { releaseNotes = v; }
    }

    // ═══════════════════════════════════════════
    //  UNIT (one physical SKU)
    // ═══════════════════════════════════════════

    public static class UnitRow {
        private UUID inventoryId;
        private String sku;
        private UUID productId;
        private String productName;
        private UUID categoryId;
        private String categoryName;
        private String colour;
        private String size;
        private String condition;
        /** Raw inventory.status column: AVAILABLE / RENTED / CLEANING / DAMAGED */
        private String status;
        /**
         * Computed live state (use this one for UI badges):
         * AVAILABLE / RENTED / BLOCKED / CLEANING / DAMAGED / INACTIVE
         */
        private String state;
        private boolean active;
        private UUID currentBlockId;
        private String currentBlockType;
        private LocalDate currentBlockStart;
        private LocalDate currentBlockEnd;
        private LocalDate nextBlockStart;
        private LocalDate nextBlockEnd;
        private BigDecimal perDayPrice;

        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID v) { inventoryId = v; }
        public String getSku() { return sku; }
        public void setSku(String v) { sku = v; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public String getProductName() { return productName; }
        public void setProductName(String v) { productName = v; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String v) { categoryName = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public String getCondition() { return condition; }
        public void setCondition(String v) { condition = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public String getState() { return state; }
        public void setState(String v) { state = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
        public UUID getCurrentBlockId() { return currentBlockId; }
        public void setCurrentBlockId(UUID v) { currentBlockId = v; }
        public String getCurrentBlockType() { return currentBlockType; }
        public void setCurrentBlockType(String v) { currentBlockType = v; }
        public LocalDate getCurrentBlockStart() { return currentBlockStart; }
        public void setCurrentBlockStart(LocalDate v) { currentBlockStart = v; }
        public LocalDate getCurrentBlockEnd() { return currentBlockEnd; }
        public void setCurrentBlockEnd(LocalDate v) { currentBlockEnd = v; }
        public LocalDate getNextBlockStart() { return nextBlockStart; }
        public void setNextBlockStart(LocalDate v) { nextBlockStart = v; }
        public LocalDate getNextBlockEnd() { return nextBlockEnd; }
        public void setNextBlockEnd(LocalDate v) { nextBlockEnd = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
    }

    /** Unit + its full block history (active and released). */
    public static class UnitDetailResponse {
        private UnitRow unit;
        private List<BlockRow> blocks;

        public UnitRow getUnit() { return unit; }
        public void setUnit(UnitRow v) { unit = v; }
        public List<BlockRow> getBlocks() { return blocks; }
        public void setBlocks(List<BlockRow> v) { blocks = v; }
    }

    // ═══════════════════════════════════════════
    //  PRODUCT STOCK
    // ═══════════════════════════════════════════

    public static class ProductStockRow {
        private UUID productId;
        private String name;
        private UUID categoryId;
        private String categoryName;
        private List<String> colours;
        private int totalUnits;
        private int activeUnits;
        private int availableUnits;
        private int rentedUnits;
        private int blockedUnits;
        private int cleaningUnits;
        private int damagedUnits;
        private int inactiveUnits;
        private double utilizationPercent;
        /** IN_STOCK / LOW_STOCK / OUT_OF_STOCK */
        private String stockStatus;
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal offerPrice;
        private BigDecimal securityDeposit;
        private boolean active;
        private boolean visible;
        private boolean featured;
        private boolean newArrival;
        private LocalDateTime createdAt;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String v) { categoryName = v; }
        public List<String> getColours() { return colours; }
        public void setColours(List<String> v) { colours = v; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int v) { totalUnits = v; }
        public int getActiveUnits() { return activeUnits; }
        public void setActiveUnits(int v) { activeUnits = v; }
        public int getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(int v) { availableUnits = v; }
        public int getRentedUnits() { return rentedUnits; }
        public void setRentedUnits(int v) { rentedUnits = v; }
        public int getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(int v) { blockedUnits = v; }
        public int getCleaningUnits() { return cleaningUnits; }
        public void setCleaningUnits(int v) { cleaningUnits = v; }
        public int getDamagedUnits() { return damagedUnits; }
        public void setDamagedUnits(int v) { damagedUnits = v; }
        public int getInactiveUnits() { return inactiveUnits; }
        public void setInactiveUnits(int v) { inactiveUnits = v; }
        public double getUtilizationPercent() { return utilizationPercent; }
        public void setUtilizationPercent(double v) { utilizationPercent = v; }
        public String getStockStatus() { return stockStatus; }
        public void setStockStatus(String v) { stockStatus = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal v) { weekendPrice = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal v) { securityDeposit = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean v) { visible = v; }
        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean v) { featured = v; }
        public boolean isNewArrival() { return newArrival; }
        public void setNewArrival(boolean v) { newArrival = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }

    /** One line of the colour x size stock matrix. */
    public static class ColourSizeStock {
        private String colour;
        private String size;
        private int totalUnits;
        private int availableUnits;
        private int rentedUnits;
        private int blockedUnits;
        private int otherUnits;

        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int v) { totalUnits = v; }
        public int getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(int v) { availableUnits = v; }
        public int getRentedUnits() { return rentedUnits; }
        public void setRentedUnits(int v) { rentedUnits = v; }
        public int getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(int v) { blockedUnits = v; }
        public int getOtherUnits() { return otherUnits; }
        public void setOtherUnits(int v) { otherUnits = v; }
    }

    public static class ProductInventoryDetailResponse {
        private ProductStockRow product;
        private int imageCount;
        private List<ColourSizeStock> stockMatrix;
        private List<UnitRow> units;
        private List<BlockRow> activeBlocks;

        public ProductStockRow getProduct() { return product; }
        public void setProduct(ProductStockRow v) { product = v; }
        public int getImageCount() { return imageCount; }
        public void setImageCount(int v) { imageCount = v; }
        public List<ColourSizeStock> getStockMatrix() { return stockMatrix; }
        public void setStockMatrix(List<ColourSizeStock> v) { stockMatrix = v; }
        public List<UnitRow> getUnits() { return units; }
        public void setUnits(List<UnitRow> v) { units = v; }
        public List<BlockRow> getActiveBlocks() { return activeBlocks; }
        public void setActiveBlocks(List<BlockRow> v) { activeBlocks = v; }
    }

    // ═══════════════════════════════════════════
    //  DASHBOARD (everything in one call)
    // ═══════════════════════════════════════════

    public static class DashboardResponse {
        private SummaryResponse summary;
        private List<BreakdownItem> byCategory;
        private List<BreakdownItem> byColour;
        private List<BreakdownItem> bySize;
        private List<BreakdownItem> byCondition;
        private List<BreakdownItem> byStatus;
        private List<ProductStockRow> outOfStockProducts;
        private List<ProductStockRow> lowStockProducts;
        private List<BlockRow> upcomingBlocks;

        public SummaryResponse getSummary() { return summary; }
        public void setSummary(SummaryResponse v) { summary = v; }
        public List<BreakdownItem> getByCategory() { return byCategory; }
        public void setByCategory(List<BreakdownItem> v) { byCategory = v; }
        public List<BreakdownItem> getByColour() { return byColour; }
        public void setByColour(List<BreakdownItem> v) { byColour = v; }
        public List<BreakdownItem> getBySize() { return bySize; }
        public void setBySize(List<BreakdownItem> v) { bySize = v; }
        public List<BreakdownItem> getByCondition() { return byCondition; }
        public void setByCondition(List<BreakdownItem> v) { byCondition = v; }
        public List<BreakdownItem> getByStatus() { return byStatus; }
        public void setByStatus(List<BreakdownItem> v) { byStatus = v; }
        public List<ProductStockRow> getOutOfStockProducts() { return outOfStockProducts; }
        public void setOutOfStockProducts(List<ProductStockRow> v) { outOfStockProducts = v; }
        public List<ProductStockRow> getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(List<ProductStockRow> v) { lowStockProducts = v; }
        public List<BlockRow> getUpcomingBlocks() { return upcomingBlocks; }
        public void setUpcomingBlocks(List<BlockRow> v) { upcomingBlocks = v; }
    }
}
