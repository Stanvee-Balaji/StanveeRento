package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rental_order_units")
public class RentalOrderUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "order_pk", nullable = false)
    private UUID orderPk;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "size")
    private String size;

    @Column(name = "colour")
    private String colour;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "blocked_until", nullable = false)
    private LocalDate blockedUntil;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason", length = 200)
    private String releaseReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;



    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalINDTime.now();
        if (createdAt == null) createdAt = n;
    }

    public UUID getId() { return id; }

    public UUID getOrderPk() { return orderPk; }
    public void setOrderPk(UUID v) { this.orderPk = v; }
    public UUID getItemId() { return itemId; }
    public void setItemId(UUID v) { this.itemId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public UUID getInventoryId() { return inventoryId; }
    public void setInventoryId(UUID v) { this.inventoryId = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }
    public String getSize() { return size; }
    public void setSize(String v) { this.size = v; }
    public String getColour() { return colour; }
    public void setColour(String v) { this.colour = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public LocalDate getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDate v) { this.blockedUntil = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime v) { this.releasedAt = v; }
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String v) { this.releaseReason = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
