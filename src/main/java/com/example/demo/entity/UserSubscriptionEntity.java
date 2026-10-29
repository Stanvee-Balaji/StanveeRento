package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per subscription purchase attempt. Plan data (name / price / duration) is SNAPSHOTTED at
 * purchase time so later plan edits never change what a user paid for.
 * order_id links 1:1 to transactions.order_id (the Airpay payment record).
 */
@Entity
@Table(name = "user_subscriptions", indexes = {
        @Index(name = "idx_usub_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_usub_username", columnList = "username"),
        @Index(name = "idx_usub_status", columnList = "status"),
        @Index(name = "idx_usub_end_date", columnList = "end_date")
})
public class UserSubscriptionEntity {

    public enum Status {
        PENDING_PAYMENT,   // created, waiting for Airpay
        ACTIVE,            // paid + activated (check start/end dates for "currently running")
        EXPIRED,           // end date passed
        FAILED,            // payment failed / rejected by admin
        CANCELLED,         // buyer cancelled on Airpay
        NEEDS_REVIEW,      // suspicious callback (hash mismatch / amount mismatch) — admin must decide
        REVOKED,           // admin removed access (money NOT returned)
        REFUNDED           // admin recorded a refund (access removed)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, length = 150)
    private String username;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_name", nullable = false, length = 150)
    private String planName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "duration_value", nullable = false)
    private Integer durationValue;

    @Column(name = "duration_unit", nullable = false, length = 10)
    private String durationUnit;

    @Column(name = "order_id", nullable = false, unique = true, length = 30)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** AUTO (Airpay IPN) or ADMIN (manual confirm). */
    @Column(name = "activated_by", length = 20)
    private String activatedBy;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "refund_reference", length = 100)
    private String refundReference;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserSubscriptionEntity() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalINDTime.now(); updatedAt = createdAt; }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalINDTime.now(); }
    
    
    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal v) { depositAmount = v; }

    public UUID getId() { return id; }
    public void setId(UUID v) { id = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { planId = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { planName = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { price = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { currency = v; }
    public Integer getDurationValue() { return durationValue; }
    public void setDurationValue(Integer v) { durationValue = v; }
    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String v) { durationUnit = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { status = v; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime v) { startDate = v; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime v) { endDate = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { paidAt = v; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String v) { activatedBy = v; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String v) { statusReason = v; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String v) { refundReference = v; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime v) { refundedAt = v; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime v) { revokedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
