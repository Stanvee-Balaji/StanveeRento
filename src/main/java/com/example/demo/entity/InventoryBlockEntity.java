package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_blocks")
public class InventoryBlockEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "block_type", nullable = false)
    private String blockType; // MAINTENANCE | CLEANING | WASHING | DAMAGE | MANUAL_OVERRIDE | OTHER

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // inclusive

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_by_type", nullable = false)
    private String createdByType; // SUPER_ADMIN | ADMIN

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "released_by_type")
    private String releasedByType;

    @Column(name = "release_notes")
    private String releaseNotes;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public InventoryEntity getInventory() { return inventory; }
    public void setInventory(InventoryEntity inventory) { this.inventory = inventory; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

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

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getCreatedByType() { return createdByType; }
    public void setCreatedByType(String createdByType) { this.createdByType = createdByType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    public UUID getReleasedBy() { return releasedBy; }
    public void setReleasedBy(UUID releasedBy) { this.releasedBy = releasedBy; }

    public String getReleasedByType() { return releasedByType; }
    public void setReleasedByType(String releasedByType) { this.releasedByType = releasedByType; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
}
