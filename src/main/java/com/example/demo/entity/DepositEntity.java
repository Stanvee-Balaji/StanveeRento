package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One row per paid subscription purchase that included a deposit. Money columns are only changed by DepositService. */
@Entity
@Table(name = "subscription_deposits",
        uniqueConstraints = @UniqueConstraint(name = "uk_sub_dep_order", columnNames = "order_id"),
        indexes = @Index(name = "idx_sub_dep_username", columnList = "username"))
public class DepositEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "purchase_id", nullable = false) private UUID purchaseId;
    @Column(name = "order_id", nullable = false, length = 20) private String orderId;
    @Column(name = "username", nullable = false, length = 100) private String username;
    @Column(name = "plan_name", length = 200) private String planName;

    /** Payment breakdown of the single Airpay payment: plan_amount + deposit_paid = total_paid. */
    @Column(name = "plan_amount", nullable = false, precision = 12, scale = 2) private BigDecimal planAmount;
    @Column(name = "deposit_paid", nullable = false, precision = 12, scale = 2) private BigDecimal depositPaid;
    @Column(name = "total_paid", nullable = false, precision = 12, scale = 2) private BigDecimal totalPaid;

    @Column(name = "deducted_total", nullable = false, precision = 12, scale = 2) private BigDecimal deductedTotal = BigDecimal.ZERO;
    @Column(name = "refunded_total", nullable = false, precision = 12, scale = 2) private BigDecimal refundedTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) private DepositStatus status = DepositStatus.HELD;

    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @Version private Long version;

    public BigDecimal balance() { return depositPaid.subtract(deductedTotal).subtract(refundedTotal); }

    public UUID getId() { return id; }
    public UUID getPurchaseId() { return purchaseId; }
    public void setPurchaseId(UUID v) { purchaseId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { planName = v; }
    public BigDecimal getPlanAmount() { return planAmount; }
    public void setPlanAmount(BigDecimal v) { planAmount = v; }
    public BigDecimal getDepositPaid() { return depositPaid; }
    public void setDepositPaid(BigDecimal v) { depositPaid = v; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal v) { totalPaid = v; }
    public BigDecimal getDeductedTotal() { return deductedTotal; }
    public void setDeductedTotal(BigDecimal v) { deductedTotal = v; }
    public BigDecimal getRefundedTotal() { return refundedTotal; }
    public void setRefundedTotal(BigDecimal v) { refundedTotal = v; }
    public DepositStatus getStatus() { return status; }
    public void setStatus(DepositStatus v) { status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    public Long getVersion() { return version; }

    // ───────────── enums (kept in this file on purpose) ─────────────

    /** HELD = untouched, PARTIALLY_USED = some deducted/refunded but balance left, SETTLED = balance is 0. */
    public enum DepositStatus { HELD, PARTIALLY_USED, SETTLED }

    /** Type of a ledger row. */
    public enum DepositTxnType { DEPOSIT_RECEIVED, DEDUCTION, REFUND }

    /** Why money was deducted. */
    public enum DeductionReason { PRODUCT_DAMAGE, LOST_ITEM, LATE_RETURN, CLEANING, POLICY_VIOLATION, OTHER }
}
