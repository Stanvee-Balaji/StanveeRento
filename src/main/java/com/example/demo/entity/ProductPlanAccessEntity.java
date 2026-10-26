package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_plan_access",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "plan_id"}))
public class ProductPlanAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { productId = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { planId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}