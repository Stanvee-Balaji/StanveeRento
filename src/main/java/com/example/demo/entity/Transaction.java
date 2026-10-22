package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persists every Airpay transaction attempt.
 *
 * One row per orderId: created (status = INITIATED) when /api/payment/initiate
 * is called, updated through AWAITING_PAYMENT, then finalized to
 * SUCCESS / FAILED / CANCELLED / HASH_MISMATCH / ERROR when Airpay's
 * callback hits /api/payment/response.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_created_at", columnList = "created_at"),
        @Index(name = "idx_txn_buyer_email", columnList = "buyer_email")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 30)
    private String orderId;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "buyer_email", nullable = false, length = 100)
    private String buyerEmail;

    @Column(name = "buyer_phone", length = 20)
    private String buyerPhone;

    @Column(name = "buyer_first_name", length = 100)
    private String buyerFirstName;

    @Column(name = "buyer_last_name", length = 100)
    private String buyerLastName;

    @Column(name = "buyer_address", length = 255)
    private String buyerAddress;

    @Column(name = "buyer_city", length = 100)
    private String buyerCity;

    @Column(name = "buyer_state", length = 100)
    private String buyerState;

    @Column(name = "buyer_pincode", length = 20)
    private String buyerPincode;

    @Column(name = "buyer_country", length = 100)
    private String buyerCountry;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "iso_currency", length = 10)
    private String isoCurrency;

    @Column(name = "customvar", length = 255)
    private String customvar;

    @Column(name = "kittype", length = 30)
    private String kittype;

    @Column(name = "txn_sub_type", length = 10)
    private String txnSubType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Column(name = "ap_transaction_id", length = 100)
    private String apTransactionId;

    @Column(name = "airpay_status_code", length = 10)
    private String airpayStatusCode;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "secure_hash_status", length = 30)
    private String secureHashStatus;

    @Column(name = "request_checksum", length = 100)
    private String requestChecksum;

    @Column(name = "private_key", length = 100)
    private String privateKey;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Transaction() {
    }

    // ---- IST-based lifecycle timestamps (replaces @CreationTimestamp/@UpdateTimestamp,
    //      which use the JVM's system clock instead of Asia/Kolkata) ----

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalINDTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalINDTime.now();
    }

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }

    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

    public String getBuyerFirstName() { return buyerFirstName; }
    public void setBuyerFirstName(String buyerFirstName) { this.buyerFirstName = buyerFirstName; }

    public String getBuyerLastName() { return buyerLastName; }
    public void setBuyerLastName(String buyerLastName) { this.buyerLastName = buyerLastName; }

    public String getBuyerAddress() { return buyerAddress; }
    public void setBuyerAddress(String buyerAddress) { this.buyerAddress = buyerAddress; }

    public String getBuyerCity() { return buyerCity; }
    public void setBuyerCity(String buyerCity) { this.buyerCity = buyerCity; }

    public String getBuyerState() { return buyerState; }
    public void setBuyerState(String buyerState) { this.buyerState = buyerState; }

    public String getBuyerPincode() { return buyerPincode; }
    public void setBuyerPincode(String buyerPincode) { this.buyerPincode = buyerPincode; }

    public String getBuyerCountry() { return buyerCountry; }
    public void setBuyerCountry(String buyerCountry) { this.buyerCountry = buyerCountry; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getIsoCurrency() { return isoCurrency; }
    public void setIsoCurrency(String isoCurrency) { this.isoCurrency = isoCurrency; }

    public String getCustomvar() { return customvar; }
    public void setCustomvar(String customvar) { this.customvar = customvar; }

    public String getKittype() { return kittype; }
    public void setKittype(String kittype) { this.kittype = kittype; }

    public String getTxnSubType() { return txnSubType; }
    public void setTxnSubType(String txnSubType) { this.txnSubType = txnSubType; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getApTransactionId() { return apTransactionId; }
    public void setApTransactionId(String apTransactionId) { this.apTransactionId = apTransactionId; }

    public String getAirpayStatusCode() { return airpayStatusCode; }
    public void setAirpayStatusCode(String airpayStatusCode) { this.airpayStatusCode = airpayStatusCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSecureHashStatus() { return secureHashStatus; }
    public void setSecureHashStatus(String secureHashStatus) { this.secureHashStatus = secureHashStatus; }

    public String getRequestChecksum() { return requestChecksum; }
    public void setRequestChecksum(String requestChecksum) { this.requestChecksum = requestChecksum; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}