package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    /** NEW / GOOD / FAIR / POOR */
    @Column(name = "condition", nullable = false)
    private String condition = "NEW";

    /** AVAILABLE / RENTED / CLEANING / DAMAGED */
    @Column(name = "status", nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "blocked_from")
    private LocalDateTime blockedFrom;

    @Column(name = "blocked_to")
    private LocalDateTime blockedTo;

    // ── getters / setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getBlockedFrom() { return blockedFrom; }
    public void setBlockedFrom(LocalDateTime blockedFrom) { this.blockedFrom = blockedFrom; }

    public LocalDateTime getBlockedTo() { return blockedTo; }
    public void setBlockedTo(LocalDateTime blockedTo) { this.blockedTo = blockedTo; }
}
