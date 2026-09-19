package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "colour")
    private String colour;

    @Column(name = "occasion")
    private String occasion;

    @Column(name = "per_day_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal perDayPrice;

    @Column(name = "weekend_price", precision = 12, scale = 2)
    private BigDecimal weekendPrice;

    @Column(name = "security_deposit", precision = 12, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "offer_price", precision = 12, scale = 2)
    private BigDecimal offerPrice;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Convenience collections — the service manages these explicitly via
    // ProductImageRepository / InventoryRepository, but mapping them here
    // lets product.getImages() / product.getInventory() work if you ever
    // need them (e.g. cascade-delete on the parent row).
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryEntity> inventory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── getters / setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CategoryEntity getCategory() { return category; }
    public void setCategory(CategoryEntity category) { this.category = category; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

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

    public BigDecimal getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }

    public BigDecimal getOfferPrice() { return offerPrice; }
    public void setOfferPrice(BigDecimal offerPrice) { this.offerPrice = offerPrice; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<ProductImageEntity> getImages() { return images; }
    public void setImages(List<ProductImageEntity> images) { this.images = images; }

    public List<InventoryEntity> getInventory() { return inventory; }
    public void setInventory(List<InventoryEntity> inventory) { this.inventory = inventory; }
}
