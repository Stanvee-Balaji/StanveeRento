package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(name = "duration_value", nullable = false)
    private Integer durationValue;

    @Column(name = "duration_unit", nullable = false)
    private String durationUnit; // DAY / MONTH / YEAR

    @Column(name = "is_recommended", nullable = false)
    private boolean isRecommended = false;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT / ACTIVE / INACTIVE / ARCHIVED

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanFeatureEntity> features = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanLimitEntity> limits = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public Integer getDurationValue() { return durationValue; }
    public void setDurationValue(Integer v) { this.durationValue = v; }
    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String v) { this.durationUnit = v; }
    public boolean isRecommended() { return isRecommended; }
    public void setRecommended(boolean v) { this.isRecommended = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime v) { this.deletedAt = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public List<PlanFeatureEntity> getFeatures() { return features; }
    public void setFeatures(List<PlanFeatureEntity> v) { this.features = v; }
    public List<PlanLimitEntity> getLimits() { return limits; }
    public void setLimits(List<PlanLimitEntity> v) { this.limits = v; }
}
