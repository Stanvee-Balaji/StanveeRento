package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductDto {

    private ProductDto() {}

    // ═══════════════════════════════════════════
    //  NESTED: IMAGE / INVENTORY ITEM
    // ═══════════════════════════════════════════

    public static class ImageItem {
        @NotBlank
        private String imageUrl;

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String v) { imageUrl = v; }
    }

    public static class InventoryItem {
        @NotBlank
        private String size;
        @NotBlank
        private String sku;
        private String condition;   // NEW / GOOD / FAIR / POOR
        private String status;      // AVAILABLE / RENTED / CLEANING / DAMAGED
        private boolean isActive = true;
        private boolean isBlocked = false;
        private LocalDateTime blockedFrom;
        private LocalDateTime blockedTo;

        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public String getSku() { return sku; }
        public void setSku(String v) { sku = v; }
        public String getCondition() { return condition; }
        public void setCondition(String v) { condition = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public boolean isBlocked() { return isBlocked; }
        public void setBlocked(boolean v) { isBlocked = v; }
        public LocalDateTime getBlockedFrom() { return blockedFrom; }
        public void setBlockedFrom(LocalDateTime v) { blockedFrom = v; }
        public LocalDateTime getBlockedTo() { return blockedTo; }
        public void setBlockedTo(LocalDateTime v) { blockedTo = v; }
    }

    // ═══════════════════════════════════════════
    //  REQUESTS
    // ═══════════════════════════════════════════

    public static class CreateProductRequest {
        @NotNull
        private UUID categoryId;
        @NotBlank
        private String name;
        private String description;
        private String colour;
        private String occasion;
        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal securityDeposit;
        private BigDecimal offerPrice;
        private boolean isVisible = true;
        @Valid
        private List<ImageItem> images;
        @Valid
        private List<InventoryItem> inventory;

        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String v) { occasion = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal v) { weekendPrice = v; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal v) { securityDeposit = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public boolean isVisible() { return isVisible; }
        public void setVisible(boolean v) { isVisible = v; }
        public List<ImageItem> getImages() { return images; }
        public void setImages(List<ImageItem> v) { images = v; }
        public List<InventoryItem> getInventory() { return inventory; }
        public void setInventory(List<InventoryItem> v) { inventory = v; }
    }

    public static class UpdateProductRequest {
        @NotNull
        private UUID categoryId;
        @NotBlank
        private String name;
        private String description;
        private String colour;
        private String occasion;
        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal perDayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal securityDeposit;
        private BigDecimal offerPrice;
        private boolean isVisible;

        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String v) { occasion = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal v) { weekendPrice = v; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal v) { securityDeposit = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public boolean isVisible() { return isVisible; }
        public void setVisible(boolean v) { isVisible = v; }
    }

    public static class UpdateImagesRequest {
        @NotNull @Valid
        private List<ImageItem> images;

        public List<ImageItem> getImages() { return images; }
        public void setImages(List<ImageItem> v) { images = v; }
    }

    public static class UpdateInventoryRequest {
        @NotNull @Valid
        private List<InventoryItem> inventory;

        public List<InventoryItem> getInventory() { return inventory; }
        public void setInventory(List<InventoryItem> v) { inventory = v; }
    }

    public static class ActiveFlagRequest {
        @NotNull
        private Boolean active;

        public Boolean getActive() { return active; }
        public void setActive(Boolean v) { active = v; }
    }

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    public static class ImageResponse {
        private UUID id;
        private String imageUrl;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String v) { imageUrl = v; }
    }

    public static class InventoryResponse {
        private UUID id;
        private String size, sku, condition, status;
        private boolean isActive, isBlocked;
        private LocalDateTime blockedFrom, blockedTo;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public String getSku() { return sku; }
        public void setSku(String v) { sku = v; }
        public String getCondition() { return condition; }
        public void setCondition(String v) { condition = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public boolean isBlocked() { return isBlocked; }
        public void setBlocked(boolean v) { isBlocked = v; }
        public LocalDateTime getBlockedFrom() { return blockedFrom; }
        public void setBlockedFrom(LocalDateTime v) { blockedFrom = v; }
        public LocalDateTime getBlockedTo() { return blockedTo; }
        public void setBlockedTo(LocalDateTime v) { blockedTo = v; }
    }

    public static class ProductResponse {
        private UUID id, categoryId, createdBy;
        private String name, description, colour, occasion;
        private BigDecimal perDayPrice, weekendPrice, securityDeposit, offerPrice;
        private boolean isActive, isVisible;
        private List<ImageResponse> images;
        private List<InventoryResponse> inventory;
        private LocalDateTime createdAt, updatedAt, deletedAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public UUID getCreatedBy() { return createdBy; }
        public void setCreatedBy(UUID v) { createdBy = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String v) { occasion = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getWeekendPrice() { return weekendPrice; }
        public void setWeekendPrice(BigDecimal v) { weekendPrice = v; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal v) { securityDeposit = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public boolean isVisible() { return isVisible; }
        public void setVisible(boolean v) { isVisible = v; }
        public List<ImageResponse> getImages() { return images; }
        public void setImages(List<ImageResponse> v) { images = v; }
        public List<InventoryResponse> getInventory() { return inventory; }
        public void setInventory(List<InventoryResponse> v) { inventory = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime v) { deletedAt = v; }
    }

    public static class ProductListItemResponse {
        private UUID id, categoryId;
        private String name, colour, occasion;
        private BigDecimal perDayPrice, offerPrice;
        private boolean isActive, isVisible;
        private String coverImageUrl;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID v) { categoryId = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getColour() { return colour; }
        public void setColour(String v) { colour = v; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String v) { occasion = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal v) { offerPrice = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public boolean isVisible() { return isVisible; }
        public void setVisible(boolean v) { isVisible = v; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String v) { coverImageUrl = v; }
    }
    
    
    public static class BulkRowError {
        private int rowNumber;
        private String field;
        private String message;

        public BulkRowError() {}
        public BulkRowError(int rowNumber, String field, String message) {
            this.rowNumber = rowNumber; this.field = field; this.message = message;
        }
        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int v) { rowNumber = v; }
        public String getField() { return field; }
        public void setField(String v) { field = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { message = v; }
    }

    public static class BulkProductPreview {
        private String categoryName;
        private String name;
        private BigDecimal perDayPrice;
        private int inventoryCount;
        private List<String> skus;
        private boolean valid;
        private List<BulkRowError> errors = new ArrayList<>();
        private List<Integer> sourceRowNumbers = new ArrayList<>();

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String v) { categoryName = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal v) { perDayPrice = v; }
        public int getInventoryCount() { return inventoryCount; }
        public void setInventoryCount(int v) { inventoryCount = v; }
        public List<String> getSkus() { return skus; }
        public void setSkus(List<String> v) { skus = v; }
        public boolean isValid() { return valid; }
        public void setValid(boolean v) { valid = v; }
        public List<BulkRowError> getErrors() { return errors; }
        public void setErrors(List<BulkRowError> v) { errors = v; }
        public List<Integer> getSourceRowNumbers() { return sourceRowNumbers; }
        public void setSourceRowNumbers(List<Integer> v) { sourceRowNumbers = v; }
    }

    public static class BulkUploadPreviewResponse {
        private int totalRowsRead;
        private int totalProductsParsed;
        private int validProductCount;
        private int invalidProductCount;
        private List<BulkProductPreview> products;

        public int getTotalRowsRead() { return totalRowsRead; }
        public void setTotalRowsRead(int v) { totalRowsRead = v; }
        public int getTotalProductsParsed() { return totalProductsParsed; }
        public void setTotalProductsParsed(int v) { totalProductsParsed = v; }
        public int getValidProductCount() { return validProductCount; }
        public void setValidProductCount(int v) { validProductCount = v; }
        public int getInvalidProductCount() { return invalidProductCount; }
        public void setInvalidProductCount(int v) { invalidProductCount = v; }
        public List<BulkProductPreview> getProducts() { return products; }
        public void setProducts(List<BulkProductPreview> v) { products = v; }
    }

    public static class BulkImportRowOutcome {
        private String categoryName;
        private String name;
        private boolean created;
        private UUID productId;
        private List<BulkRowError> errors = new ArrayList<>();

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String v) { categoryName = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public boolean isCreated() { return created; }
        public void setCreated(boolean v) { created = v; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public List<BulkRowError> getErrors() { return errors; }
        public void setErrors(List<BulkRowError> v) { errors = v; }
    }

    public static class BulkImportResponse {
        private int totalProductsParsed;
        private int createdCount;
        private int failedCount;
        private List<BulkImportRowOutcome> results;

        public int getTotalProductsParsed() { return totalProductsParsed; }
        public void setTotalProductsParsed(int v) { totalProductsParsed = v; }
        public int getCreatedCount() { return createdCount; }
        public void setCreatedCount(int v) { createdCount = v; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int v) { failedCount = v; }
        public List<BulkImportRowOutcome> getResults() { return results; }
        public void setResults(List<BulkImportRowOutcome> v) { results = v; }
    }
}
