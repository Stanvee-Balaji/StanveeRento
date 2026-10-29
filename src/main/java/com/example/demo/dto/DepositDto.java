package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Requests / responses for the subscription-deposit module (user + admin + super-admin). */
public final class DepositDto {

    private DepositDto() {}

    // ───────────── REQUESTS ─────────────

    /** Partial deduction, e.g. 100 for a damaged product. */
    public static class DeductRequest {
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") @Digits(integer = 10, fraction = 2)
        private BigDecimal amount;
        @NotBlank(message = "reasonCode is required (PRODUCT_DAMAGE, LOST_ITEM, LATE_RETURN, CLEANING, POLICY_VIOLATION, OTHER)")
        private String reasonCode;
        @NotBlank @Size(max = 300)
        private String reason;
        /** rental order id / product id the deduction relates to (optional but recommended) */
        @Size(max = 100)
        private String reference;
        /** send a unique value per click/action; a repeat with the same key is a safe no-op */
        @Size(max = 100)
        private String idempotencyKey;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { amount = v; }
        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String v) { reasonCode = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
        public String getReference() { return reference; }
        public void setReference(String v) { reference = v; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String v) { idempotencyKey = v; }
    }

    /** Records a refund of deposit money that was already returned on the Airpay dashboard. */
    public static class DepositRefundRequest {
        /** null = refund the whole remaining balance */
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2)
        private BigDecimal amount;
        @NotBlank @Size(max = 300)
        private String reason;
        @Size(max = 100)
        private String refundReference;   // Airpay refund id / bank UTR
        @Size(max = 100)
        private String idempotencyKey;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { amount = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
        public String getRefundReference() { return refundReference; }
        public void setRefundReference(String v) { refundReference = v; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String v) { idempotencyKey = v; }
    }

    // ───────────── RESPONSES ─────────────

    /** What the buyer sees: no actor names / IPs. */
    public record UserLedgerRow(String type, BigDecimal amount, BigDecimal balanceAfter,
                                String reasonCode, String reason, String reference, LocalDateTime createdAt) {}

    public record UserDepositResponse(String orderId, String planName, BigDecimal planAmount, BigDecimal depositPaid,
                                      BigDecimal totalPaid, BigDecimal deducted, BigDecimal refunded,
                                      BigDecimal balance, String status, LocalDateTime paidAt,
                                      List<UserLedgerRow> history) {}

    /** Admin / super-admin: includes who did it and from where. */
    public record LedgerRow(UUID id, String type, BigDecimal amount, BigDecimal balanceAfter, String reasonCode,
                            String reason, String reference, String idempotencyKey, UUID actorId, String actorName,
                            String actorRole, String ip, LocalDateTime createdAt) {}

    public record DepositResponse(UUID id, UUID purchaseId, String orderId, String username, String planName,
                                  BigDecimal planAmount, BigDecimal depositPaid, BigDecimal totalPaid,
                                  BigDecimal deducted, BigDecimal refunded, BigDecimal balance, String status,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, List<LedgerRow> ledger) {}

    public record DepositStatsResponse(long totalDeposits, BigDecimal totalReceived, BigDecimal totalDeducted,
                                       BigDecimal totalRefunded, BigDecimal totalHeld) {}

    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
}
