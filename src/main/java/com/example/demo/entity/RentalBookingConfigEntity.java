package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rental_booking_config")
public class RentalBookingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "delivery_enabled", nullable = false)
    private boolean deliveryEnabled;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "free_delivery_above", precision = 12, scale = 2)
    private BigDecimal freeDeliveryAbove;

    @Column(name = "waive_delivery_for_subscribers", nullable = false)
    private boolean waiveDeliveryForSubscribers;

    @Column(name = "subscription_covers_rental", nullable = false)
    private boolean subscriptionCoversRental;

    @Column(name = "pending_hold_minutes", nullable = false)
    private int pendingHoldMinutes;

    @Column(name = "cancellation_cutoff_days", nullable = false)
    private int cancellationCutoffDays;

    @Column(name = "max_items_per_order", nullable = false)
    private int maxItemsPerOrder;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalINDTime.now();
        if (createdAt == null) createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalINDTime.now(); }

    public UUID getId() { return id; }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public boolean isDeliveryEnabled() { return deliveryEnabled; }
    public void setDeliveryEnabled(boolean v) { this.deliveryEnabled = v; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal v) { this.deliveryFee = v; }
    public BigDecimal getFreeDeliveryAbove() { return freeDeliveryAbove; }
    public void setFreeDeliveryAbove(BigDecimal v) { this.freeDeliveryAbove = v; }
    public boolean isWaiveDeliveryForSubscribers() { return waiveDeliveryForSubscribers; }
    public void setWaiveDeliveryForSubscribers(boolean v) { this.waiveDeliveryForSubscribers = v; }
    public boolean isSubscriptionCoversRental() { return subscriptionCoversRental; }
    public void setSubscriptionCoversRental(boolean v) { this.subscriptionCoversRental = v; }
    public int getPendingHoldMinutes() { return pendingHoldMinutes; }
    public void setPendingHoldMinutes(int v) { this.pendingHoldMinutes = v; }
    public int getCancellationCutoffDays() { return cancellationCutoffDays; }
    public void setCancellationCutoffDays(int v) { this.cancellationCutoffDays = v; }
    public int getMaxItemsPerOrder() { return maxItemsPerOrder; }
    public void setMaxItemsPerOrder(int v) { this.maxItemsPerOrder = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
