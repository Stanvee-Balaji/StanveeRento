package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_config")
public class CalendarConfigEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "scope", nullable = false)
    private String scope; // GLOBAL | CATEGORY | PRODUCT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Column(name = "cleaning_buffer_days", nullable = false)
    private int cleaningBufferDays;

    @Column(name = "min_rental_days", nullable = false)
    private int minRentalDays;

    @Column(name = "advance_booking_days", nullable = false)
    private int advanceBookingDays;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public CategoryEntity getCategory() { return category; }
    public void setCategory(CategoryEntity category) { this.category = category; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public int getCleaningBufferDays() { return cleaningBufferDays; }
    public void setCleaningBufferDays(int cleaningBufferDays) { this.cleaningBufferDays = cleaningBufferDays; }

    public int getMinRentalDays() { return minRentalDays; }
    public void setMinRentalDays(int minRentalDays) { this.minRentalDays = minRentalDays; }

    public int getAdvanceBookingDays() { return advanceBookingDays; }
    public void setAdvanceBookingDays(int advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
