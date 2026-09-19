package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RentalCalendarDto {

    private RentalCalendarDto() {}

    // ═══════════════════════════════════════════
    //  BLOCK / UNBLOCK
    // ═══════════════════════════════════════════

    /** Block by explicit inventory IDs (you already know which SKUs). */
    public static class BlockInventoryRequest {
        @NotEmpty(message = "inventoryIds must not be empty")
        private List<UUID> inventoryIds;

        @NotBlank
        private String blockType; // MAINTENANCE | CLEANING | WASHING | DAMAGE | MANUAL_OVERRIDE | OTHER

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate; // inclusive

        private String notes;

        public List<UUID> getInventoryIds() { return inventoryIds; }
        public void setInventoryIds(List<UUID> inventoryIds) { this.inventoryIds = inventoryIds; }
        public String getBlockType() { return blockType; }
        public void setBlockType(String blockType) { this.blockType = blockType; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    /**
     * Block N units of a product without naming specific SKUs — e.g.
     * "5 of the 10 suits in Product X for washing, Jun 10-12". The service
     * auto-picks the first N currently-available units for that range.
     */
    public static class BlockByQuantityRequest {
        @NotNull
        private UUID productId;

        @Min(1)
        private int quantity;

        @NotBlank
        private String blockType;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;

        private String notes;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getBlockType() { return blockType; }
        public void setBlockType(String blockType) { this.blockType = blockType; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class UnblockRequest {
        private String releaseNotes;
        public String getReleaseNotes() { return releaseNotes; }
        public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    }

    public static class BlockResponse {
        private UUID id;
        private UUID inventoryId;
        private String sku;
        private UUID productId;
        private String productName;
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
        public void setId(UUID id) { this.id = id; }
        public UUID getInventoryId() { return inventoryId; }
        public void setInventoryId(UUID inventoryId) { this.inventoryId = inventoryId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBlockType() { return blockType; }
        public void setBlockType(String blockType) { this.blockType = blockType; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getCreatedByType() { return createdByType; }
        public void setCreatedByType(String createdByType) { this.createdByType = createdByType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getReleasedAt() { return releasedAt; }
        public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
        public String getReleaseNotes() { return releaseNotes; }
        public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    }

    // ═══════════════════════════════════════════
    //  AVAILABILITY / OCCUPANCY
    // ═══════════════════════════════════════════

    /** One day's availability snapshot for a product. */
    public static class DayAvailability {
        private LocalDate date;
        private int totalUnits;
        private int blockedUnits;
        private int bookedUnits;   // 0 until a bookings table is wired in — see service NOTE
        private int availableUnits;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public int getTotalUnits() { return totalUnits; }
        public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
        public int getBlockedUnits() { return blockedUnits; }
        public void setBlockedUnits(int blockedUnits) { this.blockedUnits = blockedUnits; }
        public int getBookedUnits() { return bookedUnits; }
        public void setBookedUnits(int bookedUnits) { this.bookedUnits = bookedUnits; }
        public int getAvailableUnits() { return availableUnits; }
        public void setAvailableUnits(int availableUnits) { this.availableUnits = availableUnits; }
    }

    public static class AvailabilityResponse {
        private UUID productId;
        private String productName;
        private LocalDate from;
        private LocalDate to;
        private List<DayAvailability> days;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public LocalDate getFrom() { return from; }
        public void setFrom(LocalDate from) { this.from = from; }
        public LocalDate getTo() { return to; }
        public void setTo(LocalDate to) { this.to = to; }
        public List<DayAvailability> getDays() { return days; }
        public void setDays(List<DayAvailability> days) { this.days = days; }
    }

    // ═══════════════════════════════════════════
    //  CONFIG
    // ═══════════════════════════════════════════

    public static class UpsertConfigRequest {
        @NotBlank
        private String scope; // GLOBAL | CATEGORY | PRODUCT

        private UUID categoryId; // required if scope=CATEGORY
        private UUID productId;  // required if scope=PRODUCT

        @Min(0)
        private int cleaningBufferDays;

        @Min(1)
        private int minRentalDays;

        @Min(0)
        private int advanceBookingDays;

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public int getCleaningBufferDays() { return cleaningBufferDays; }
        public void setCleaningBufferDays(int cleaningBufferDays) { this.cleaningBufferDays = cleaningBufferDays; }
        public int getMinRentalDays() { return minRentalDays; }
        public void setMinRentalDays(int minRentalDays) { this.minRentalDays = minRentalDays; }
        public int getAdvanceBookingDays() { return advanceBookingDays; }
        public void setAdvanceBookingDays(int advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }
    }

    public static class ConfigResponse {
        private UUID id;
        private String scope;
        private UUID categoryId;
        private UUID productId;
        private int cleaningBufferDays;
        private int minRentalDays;
        private int advanceBookingDays;
        private LocalDateTime updatedAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public int getCleaningBufferDays() { return cleaningBufferDays; }
        public void setCleaningBufferDays(int cleaningBufferDays) { this.cleaningBufferDays = cleaningBufferDays; }
        public int getMinRentalDays() { return minRentalDays; }
        public void setMinRentalDays(int minRentalDays) { this.minRentalDays = minRentalDays; }
        public int getAdvanceBookingDays() { return advanceBookingDays; }
        public void setAdvanceBookingDays(int advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /** Validates a proposed booking window against the resolved config + live availability. */
    public static class ValidateBookingRequest {
        @NotNull
        private UUID productId;
        @Min(1)
        private int quantity;
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class ValidateBookingResponse {
        private boolean valid;
        private List<String> violations;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }
    }
}
