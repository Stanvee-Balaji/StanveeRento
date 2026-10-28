package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rental_order_items")
public class RentalOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "colour")
    private String colour;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "rental_days", nullable = false)
    private int rentalDays;

    @Column(name = "weekday_count", nullable = false)
    private int weekdayCount;

    @Column(name = "weekend_count", nullable = false)
    private int weekendCount;

    @Column(name = "day_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dayRate;

    @Column(name = "weekend_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal weekendRate;

    @Column(name = "offer_applied", nullable = false)
    private boolean offerApplied;

    @Column(name = "rental_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalValue;

    @Column(name = "subscription_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subscriptionDiscount;

    @Column(name = "rental_payable", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalPayable;

    @Column(name = "deposit_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositPerUnit;

    @Column(name = "deposit_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_pk", nullable = false)
    private RentalOrderEntity order;

    public RentalOrderEntity getOrder() { return order; }
    public void setOrder(RentalOrderEntity v) { this.order = v; }

    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalINDTime.now();
        if (createdAt == null) createdAt = n;
    }

    public UUID getId() { return id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String v) { this.coverImageUrl = v; }
    public String getSize() { return size; }
    public void setSize(String v) { this.size = v; }
    public String getColour() { return colour; }
    public void setColour(String v) { this.colour = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int v) { this.rentalDays = v; }
    public int getWeekdayCount() { return weekdayCount; }
    public void setWeekdayCount(int v) { this.weekdayCount = v; }
    public int getWeekendCount() { return weekendCount; }
    public void setWeekendCount(int v) { this.weekendCount = v; }
    public BigDecimal getDayRate() { return dayRate; }
    public void setDayRate(BigDecimal v) { this.dayRate = v; }
    public BigDecimal getWeekendRate() { return weekendRate; }
    public void setWeekendRate(BigDecimal v) { this.weekendRate = v; }
    public boolean isOfferApplied() { return offerApplied; }
    public void setOfferApplied(boolean v) { this.offerApplied = v; }
    public BigDecimal getRentalValue() { return rentalValue; }
    public void setRentalValue(BigDecimal v) { this.rentalValue = v; }
    public BigDecimal getSubscriptionDiscount() { return subscriptionDiscount; }
    public void setSubscriptionDiscount(BigDecimal v) { this.subscriptionDiscount = v; }
    public BigDecimal getRentalPayable() { return rentalPayable; }
    public void setRentalPayable(BigDecimal v) { this.rentalPayable = v; }
    public BigDecimal getDepositPerUnit() { return depositPerUnit; }
    public void setDepositPerUnit(BigDecimal v) { this.depositPerUnit = v; }
    public BigDecimal getDepositTotal() { return depositTotal; }
    public void setDepositTotal(BigDecimal v) { this.depositTotal = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
