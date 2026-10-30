package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
/**
 * Stores the raw IPN (Instant Payment Notification) JSON
 * that AirPay POSTs directly to /api/payment/ipn after every payment.
 *
 * AirPay sends plain JSON — no encryption, no encdata.
 * We just receive it and save as-is. That's Step 1.
 */
@Entity
@Table(name = "airpay_ipn_logs", indexes = {
        @Index(name = "idx_ipn_order_id", columnList = "order_id"),
        @Index(name = "idx_ipn_received_at", columnList = "received_at")
})
public class AirpayIpnLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Required fields AirPay always sends ─────────────────────────────────

    @Column(name = "order_id", length = 50)
    private String orderId;                   // your original orderId

    @Column(name = "merchant_id", length = 20)
    private String merchantId;                // your merchant id

    @Column(name = "ap_transaction_id", length = 50)
    private String apTransactionId;           // AirPay's own transaction reference

    @Column(name = "amount", length = 20)
    private String amount;                    // e.g. "999.00"

    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;         // 200=success, 402=cancelled, 400=failed

    @Column(name = "transaction_payment_status", length = 30)
    private String transactionPaymentStatus;  // "SUCCESS" / "FAILED" / "CANCEL" etc.

    @Column(name = "message", length = 255)
    private String message;                   // "Transaction success"

    @Column(name = "ap_secure_hash", length = 50)
    private String apSecureHash;              // CRC32 hash — verify in Step 2

    @Column(name = "chmod", length = 20)
    private String chmod;                     // pg / upi / nb / wallet etc.

    // ── Optional fields AirPay may send ─────────────────────────────────────

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_vpa", length = 100)
    private String customerVpa;              // only for UPI payments

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "card_scheme", length = 50)
    private String cardScheme;               // VISA / MASTERCARD etc.

    @Column(name = "currency_code", length = 10)
    private String currencyCode;             // 356 = INR

    @Column(name = "customvar", length = 255)
    private String customvar;               // your custom field from order creation

    @Column(name = "billed_amount", length = 20)
    private String billedAmount;

    @Column(name = "surcharge_amount", length = 20)
    private String surchargeAmount;

    @Column(name = "transaction_time", length = 30)
    private String transactionTime;          // "12-12-2023 10:10:12"

    // ── Full raw JSON — safety net so we never lose any field ───────────────

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;               // complete JSON string exactly as received

    // ── Our metadata ─────────────────────────────────────────────────────────

    @Column(name = "source_ip", length = 50)
    private String sourceIp;                 // AirPay server IP

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    public AirpayIpnLog() {}

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) receivedAt = LocalINDTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getApTransactionId() { return apTransactionId; }
    public void setApTransactionId(String apTransactionId) { this.apTransactionId = apTransactionId; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public String getTransactionPaymentStatus() { return transactionPaymentStatus; }
    public void setTransactionPaymentStatus(String s) { this.transactionPaymentStatus = s; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getApSecureHash() { return apSecureHash; }
    public void setApSecureHash(String apSecureHash) { this.apSecureHash = apSecureHash; }

    public String getChmod() { return chmod; }
    public void setChmod(String chmod) { this.chmod = chmod; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerVpa() { return customerVpa; }
    public void setCustomerVpa(String customerVpa) { this.customerVpa = customerVpa; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getCardScheme() { return cardScheme; }
    public void setCardScheme(String cardScheme) { this.cardScheme = cardScheme; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCustomvar() { return customvar; }
    public void setCustomvar(String customvar) { this.customvar = customvar; }

    public String getBilledAmount() { return billedAmount; }
    public void setBilledAmount(String billedAmount) { this.billedAmount = billedAmount; }

    public String getSurchargeAmount() { return surchargeAmount; }
    public void setSurchargeAmount(String surchargeAmount) { this.surchargeAmount = surchargeAmount; }

    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}