package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rental_orders")
public class RentalOrderEntity {

    public enum OrderStatus { PAYMENT_PENDING, CONFIRMED, PAYMENT_FAILED, CANCELLED, NEEDS_REVIEW }
    public enum PaymentStatus { NOT_REQUIRED, PENDING, PAID, FAILED, REFUND_PENDING, REFUNDED }
    public enum FulfilmentStatus { NOT_STARTED, DISPATCHED, DELIVERED, RETURNED, COMPLETED }
    public enum DepositStatus { NOT_APPLICABLE, PENDING, HELD, REFUNDED, PARTIALLY_REFUNDED, FORFEITED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true, length = 20)
    private String orderId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "subscription_order_id")
    private String subscriptionOrderId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfilment_status", nullable = false, length = 30)
    private FulfilmentStatus fulfilmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 30)
    private DepositStatus depositStatus;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "rental_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalValue;

    @Column(name = "subscription_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subscriptionDiscount;

    @Column(name = "rental_payable", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalPayable;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "total_payable", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "subscription_covers_rental", nullable = false)
    private boolean subscriptionCoversRental;

    @Column(name = "pricing_snapshot", columnDefinition = "text")
    private String pricingSnapshot;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "ship_full_name")
    private String shipFullName;

    @Column(name = "ship_phone")
    private String shipPhone;

    @Column(name = "ship_line1")
    private String shipLine1;

    @Column(name = "ship_line2")
    private String shipLine2;

    @Column(name = "ship_landmark")
    private String shipLandmark;

    @Column(name = "ship_city")
    private String shipCity;

    @Column(name = "ship_state")
    private String shipState;

    @Column(name = "ship_pincode")
    private String shipPincode;

    @Column(name = "ship_country")
    private String shipCountry;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "buyer_phone")
    private String buyerPhone;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "deposit_settled_at")
    private LocalDateTime depositSettledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "activated_by", length = 20)
    private String activatedBy;

    @Column(name = "refund_reference", length = 100)
    private String refundReference;

    @Column(name = "deposit_settle_note", length = 300)
    private String depositSettleNote;

    @Column(name = "deposit_refunded_amount", precision = 12, scale = 2)
    private BigDecimal depositRefundedAmount;

    @Column(name = "deposit_deduction_amount", precision = 12, scale = 2)
    private BigDecimal depositDeductionAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<RentalOrderItemEntity> items = new ArrayList<>();

    public List<RentalOrderItemEntity> getItems() { return items; }

    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalINDTime.now();
        if (createdAt == null) createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalINDTime.now(); }

    public UUID getId() { return id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID v) { this.subscriptionId = v; }
    public String getSubscriptionOrderId() { return subscriptionOrderId; }
    public void setSubscriptionOrderId(String v) { this.subscriptionOrderId = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { this.planId = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }
    public LocalDateTime getSubscriptionExpiry() { return subscriptionExpiry; }
    public void setSubscriptionExpiry(LocalDateTime v) { this.subscriptionExpiry = v; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus v) { this.status = v; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus v) { this.paymentStatus = v; }
    public FulfilmentStatus getFulfilmentStatus() { return fulfilmentStatus; }
    public void setFulfilmentStatus(FulfilmentStatus v) { this.fulfilmentStatus = v; }
    public DepositStatus getDepositStatus() { return depositStatus; }
    public void setDepositStatus(DepositStatus v) { this.depositStatus = v; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String v) { this.statusReason = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public BigDecimal getRentalValue() { return rentalValue; }
    public void setRentalValue(BigDecimal v) { this.rentalValue = v; }
    public BigDecimal getSubscriptionDiscount() { return subscriptionDiscount; }
    public void setSubscriptionDiscount(BigDecimal v) { this.subscriptionDiscount = v; }
    public BigDecimal getRentalPayable() { return rentalPayable; }
    public void setRentalPayable(BigDecimal v) { this.rentalPayable = v; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal v) { this.deliveryFee = v; }
    public BigDecimal getTotalPayable() { return totalPayable; }
    public void setTotalPayable(BigDecimal v) { this.totalPayable = v; }
    public boolean isSubscriptionCoversRental() { return subscriptionCoversRental; }
    public void setSubscriptionCoversRental(boolean v) { this.subscriptionCoversRental = v; }
    public String getPricingSnapshot() { return pricingSnapshot; }
    public void setPricingSnapshot(String v) { this.pricingSnapshot = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public UUID getAddressId() { return addressId; }
    public void setAddressId(UUID v) { this.addressId = v; }
    public String getShipFullName() { return shipFullName; }
    public void setShipFullName(String v) { this.shipFullName = v; }
    public String getShipPhone() { return shipPhone; }
    public void setShipPhone(String v) { this.shipPhone = v; }
    public String getShipLine1() { return shipLine1; }
    public void setShipLine1(String v) { this.shipLine1 = v; }
    public String getShipLine2() { return shipLine2; }
    public void setShipLine2(String v) { this.shipLine2 = v; }
    public String getShipLandmark() { return shipLandmark; }
    public void setShipLandmark(String v) { this.shipLandmark = v; }
    public String getShipCity() { return shipCity; }
    public void setShipCity(String v) { this.shipCity = v; }
    public String getShipState() { return shipState; }
    public void setShipState(String v) { this.shipState = v; }
    public String getShipPincode() { return shipPincode; }
    public void setShipPincode(String v) { this.shipPincode = v; }
    public String getShipCountry() { return shipCountry; }
    public void setShipCountry(String v) { this.shipCountry = v; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String v) { this.buyerEmail = v; }
    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String v) { this.buyerPhone = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime v) { this.confirmedAt = v; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime v) { this.cancelledAt = v; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime v) { this.dispatchedAt = v; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime v) { this.deliveredAt = v; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime v) { this.returnedAt = v; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime v) { this.refundedAt = v; }
    public LocalDateTime getDepositSettledAt() { return depositSettledAt; }
    public void setDepositSettledAt(LocalDateTime v) { this.depositSettledAt = v; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String v) { this.cancelledBy = v; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String v) { this.activatedBy = v; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String v) { this.refundReference = v; }
    public String getDepositSettleNote() { return depositSettleNote; }
    public void setDepositSettleNote(String v) { this.depositSettleNote = v; }
    public BigDecimal getDepositRefundedAmount() { return depositRefundedAmount; }
    public void setDepositRefundedAmount(BigDecimal v) { this.depositRefundedAmount = v; }
    public BigDecimal getDepositDeductionAmount() { return depositDeductionAmount; }
    public void setDepositDeductionAmount(BigDecimal v) { this.depositDeductionAmount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
