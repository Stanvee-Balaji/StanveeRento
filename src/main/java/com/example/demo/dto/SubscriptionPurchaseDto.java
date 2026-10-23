package com.example.demo.dto;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
import com.example.demo.dto.PaymentDtos.TransactionDetailDto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All request / response classes for subscription purchases (user + admin + super-admin). */
public final class SubscriptionPurchaseDto {

    private SubscriptionPurchaseDto() {}

    // ═══════════════════════════════════════════
    //  REQUESTS
    // ═══════════════════════════════════════════

    /** NOTE: there is deliberately no amount field — the price is always read from the plan in the DB. */
    public static class PurchaseRequest {
        @NotNull
        private UUID planId;
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z\\d\\s]+$", message = "Invalid first name")
        private String buyerFirstName;
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z\\d\\s]+$", message = "Invalid last name")
        private String buyerLastName;
        @NotBlank @Email @Size(min = 6, max = 50)
        private String buyerEmail;
        @NotBlank @Size(min = 8, max = 15) @Pattern(regexp = "^[\\d\\s.\\-]+$", message = "Invalid phone number")
        private String buyerPhone;
        @Size(max = 255)
        private String buyerAddress;
        @Size(max = 50) @Pattern(regexp = "^[A-Za-z\\d\\s]*$", message = "Invalid city")
        private String buyerCity;
        @Size(max = 50) @Pattern(regexp = "^[A-Za-z\\d\\s]*$", message = "Invalid state")
        private String buyerState;
        @Size(max = 8) @Pattern(regexp = "^[A-Za-z\\d]*$", message = "Invalid pincode")
        private String buyerPinCode;
        @Size(max = 50) @Pattern(regexp = "^[A-Za-z\\d\\s]*$", message = "Invalid country")
        private String buyerCountry;
 

        public UUID getPlanId() { return planId; }
        public void setPlanId(UUID v) { planId = v; }
        public String getBuyerFirstName() { return buyerFirstName; }
        public void setBuyerFirstName(String v) { buyerFirstName = v; }
        public String getBuyerLastName() { return buyerLastName; }
        public void setBuyerLastName(String v) { buyerLastName = v; }
        public String getBuyerEmail() { return buyerEmail; }
        public void setBuyerEmail(String v) { buyerEmail = v; }
        public String getBuyerPhone() { return buyerPhone; }
        public void setBuyerPhone(String v) { buyerPhone = v; }
        public String getBuyerAddress() { return buyerAddress; }
        public void setBuyerAddress(String v) { buyerAddress = v; }
        public String getBuyerCity() { return buyerCity; }
        public void setBuyerCity(String v) { buyerCity = v; }
        public String getBuyerState() { return buyerState; }
        public void setBuyerState(String v) { buyerState = v; }
        public String getBuyerPinCode() { return buyerPinCode; }
        public void setBuyerPinCode(String v) { buyerPinCode = v; }
        public String getBuyerCountry() { return buyerCountry; }
        public void setBuyerCountry(String v) { buyerCountry = v; }
    }

    public static class ConfirmRequest {
        @NotBlank @Size(max = 300)
        private String reason;
        private String apTransactionId; // Airpay transaction id seen on the merchant dashboard (recommended)

        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
        public String getApTransactionId() { return apTransactionId; }
        public void setApTransactionId(String v) { apTransactionId = v; }
    }

    /** Used by reject and revoke. */
    public static class ReasonRequest {
        @NotBlank @Size(max = 300)
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
    }

    public static class RefundRequest {
        @NotBlank @Size(max = 300)
        private String reason;
        @Size(max = 100)
        private String refundReference; // Airpay refund id / bank UTR

        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
        public String getRefundReference() { return refundReference; }
        public void setRefundReference(String v) { refundReference = v; }
    }

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    /** What a buyer is allowed to see about their own purchase. */
    public record UserPurchaseResponse(UUID id, UUID planId, String planName, BigDecimal price, String currency,
                                       Integer durationValue, String durationUnit, String orderId, String status,
                                       LocalDateTime startDate, LocalDateTime endDate, LocalDateTime paidAt,
                                       boolean active) {}

    /** Returned by POST /purchase: the purchase + everything the frontend needs to open the Airpay iframe. */
    public record InitiateResponse(UserPurchaseResponse purchase, PaymentInitResponse payment) {}

    /** Admin / super-admin view. */
    public record PurchaseResponse(UUID id, String username, UUID planId, String planName, BigDecimal price,
                                   String currency, Integer durationValue, String durationUnit, String orderId,
                                   String status, LocalDateTime startDate, LocalDateTime endDate,
                                   LocalDateTime paidAt, String activatedBy, String statusReason,
                                   String refundReference, LocalDateTime refundedAt, LocalDateTime revokedAt,
                                   LocalDateTime createdAt, LocalDateTime updatedAt, boolean currentlyActive) {}

    public record IpnLogRow(Long id, String apTransactionId, String amount, String transactionStatus,
                            String transactionPaymentStatus, String message, String chmod, String sourceIp,
                            LocalDateTime receivedAt) {}

    public record PurchaseDetailResponse(PurchaseResponse purchase, TransactionDetailDto transaction,
                                         List<IpnLogRow> ipnLogs, List<AuditLogDto> auditLogs) {}

    public record PlanRevenueRow(String planName, long purchases, BigDecimal revenue) {}

    public record StatsResponse(long totalPurchases, long activeNow, long stuckCount,
                                Map<String, Long> countsByStatus,
                                BigDecimal totalRevenue, BigDecimal todayRevenue, BigDecimal refundedAmount,
                                List<PlanRevenueRow> revenueByPlan) {}

    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
}
