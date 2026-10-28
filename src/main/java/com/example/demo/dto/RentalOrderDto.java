package com.example.demo.dto;

import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
import com.example.demo.dto.PaymentDtos.PaymentRequest;
import com.example.demo.dto.PaymentDtos.TransactionDetailDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Requests / responses for rental order placing (user + admin + super-admin). */
public final class RentalOrderDto {

    private RentalOrderDto() {}

    // ═══════════════════════════════════════════
    //  REQUESTS
    // ═══════════════════════════════════════════

    /** NOTE: no price fields anywhere — every amount is calculated on the backend from the DB. */
    public record ItemRequest(@NotNull UUID productId,
                              @NotBlank @Size(max = 20) String size,
                              @Size(max = 50) String colour,
                              @Min(1) @Max(10) int quantity,
                              @NotNull LocalDate startDate,
                              @NotNull LocalDate endDate) {}

    /** Same body for POST /summary and POST /book (addressId is mandatory only for /book). */
    public record OrderRequest(@NotEmpty @Size(max = 20) List<@Valid ItemRequest> items,
                               UUID addressId,
                               @Email @Size(max = 100) String buyerEmail,
                               @Size(max = 300) String notes) {}

    public record ReasonRequest(@NotBlank @Size(max = 300) String reason) {}

    public record ConfirmRequest(@NotBlank @Size(max = 300) String reason, @Size(max = 100) String apTransactionId) {}

    public record RefundRequest(@NotBlank @Size(max = 300) String reason, @Size(max = 100) String refundReference) {}

    public record NoteRequest(@Size(max = 300) String note) {}

    public record DepositSettleRequest(@NotNull @DecimalMin("0.00") BigDecimal refundAmount,
                                       @NotNull @DecimalMin("0.00") BigDecimal deductionAmount,
                                       @Size(max = 300) String note) {}

    public record ConfigRequest(@NotNull Boolean deliveryEnabled,
                                @NotNull @DecimalMin("0.00") BigDecimal deliveryFee,
                                @DecimalMin("0.00") BigDecimal freeDeliveryAbove,
                                @NotNull Boolean waiveDeliveryForSubscribers,
                                @NotNull Boolean subscriptionCoversRental,
                                @Min(5) @Max(120) int pendingHoldMinutes,
                                @Min(0) @Max(30) int cancellationCutoffDays,
                                @Min(1) @Max(50) int maxItemsPerOrder) {}

    // ═══════════════════════════════════════════
    //  SHARED VIEWS
    // ═══════════════════════════════════════════

    public record AddressView(UUID id, String fullName, String phone, String line1, String line2, String landmark,
                              String city, String state, String pincode, String country) {}

    /** One rental line. In /summary: availableUnits + violations are filled, skus is empty. In orders: those two are null. */
    public record ItemView(UUID id, UUID productId, String productName, String coverImageUrl, String size, String colour,
                           int quantity, LocalDate startDate, LocalDate endDate, int rentalDays,
                           int weekdayCount, int weekendCount, BigDecimal dayRate, BigDecimal weekendRate,
                           boolean offerApplied, BigDecimal rentalValue, BigDecimal subscriptionDiscount,
                           BigDecimal rentalPayable, BigDecimal depositPerUnit, BigDecimal depositTotal,
                           Integer availableUnits, List<String> violations, List<String> skus) {}

    public record SubscriptionView(String orderId, UUID planId, String planName, LocalDateTime startDate,
                                   LocalDateTime endDate, LocalDateTime effectiveExpiry,
                                   boolean planDepositRequired, BigDecimal planDepositAmount, boolean depositPaid) {}

    public record PriceBreakdown(String currency, BigDecimal rentalValue, BigDecimal subscriptionDiscount,
                                 BigDecimal rentalPayable, BigDecimal depositAmount, BigDecimal deliveryFee,
                                 String deliveryNote, BigDecimal totalPayable, boolean paymentRequired) {}

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    public record SummaryResponse(boolean canBook, boolean paymentRequired, List<String> violations,
                                  SubscriptionView subscription, List<ItemView> items,
                                  PriceBreakdown breakdown, AddressView address) {}

    public record OrderResponse(UUID id, String orderId, String username,
                                String status, String label, String paymentStatus, String fulfilmentStatus,
                                String depositStatus, String statusReason, boolean paymentRequired, String currency,
                                BigDecimal rentalValue, BigDecimal subscriptionDiscount, BigDecimal rentalPayable,
                                BigDecimal depositAmount, BigDecimal deliveryFee, BigDecimal totalPayable,
                                String subscriptionOrderId, String planName, AddressView address, List<ItemView> items,
                                LocalDate startDate, LocalDate endDate, LocalDateTime expiresAt, LocalDateTime createdAt,
                                LocalDateTime paidAt, LocalDateTime confirmedAt, LocalDateTime cancelledAt,
                                LocalDateTime dispatchedAt, LocalDateTime deliveredAt, LocalDateTime returnedAt,
                                LocalDateTime completedAt, LocalDateTime refundedAt, String refundReference,
                                BigDecimal depositRefundedAmount, BigDecimal depositDeductionAmount,
                                LocalDateTime depositSettledAt) {}

    public record EventView(String eventType, String fromStatus, String toStatus, String actor, String actorRole,
                            String message, LocalDateTime createdAt) {}

    /** User detail: the order + customer-visible timeline. */
    public record OrderDetailResponse(OrderResponse order, List<EventView> timeline) {}

    /** Admin detail: order + Airpay transaction + IPN logs + full event history. */
    public record AdminOrderDetailResponse(OrderResponse order, TransactionDetailDto transaction,
                                           List<SubscriptionPurchaseDto.IpnLogRow> ipnLogs, List<EventView> events) {}

    /** Returned by POST /book. payment is null when nothing is payable (booking already CONFIRMED). */
    public record BookResponse(OrderResponse order, boolean paymentRequired, PaymentInitResponse payment) {}

    /** Internal hand-off from RentalOrderService to UserRentalService (never serialized to clients). */
    public record BookingResult(OrderResponse order, boolean paymentRequired, PaymentRequest paymentRequest) {}

    public record DayAvailability(LocalDate date, int totalUnits, int availableUnits) {}

    public record AvailabilityResponse(UUID productId, String productName, String size, String colour,
                                       LocalDate from, LocalDate to, List<DayAvailability> days) {}

    public record ConfigResponse(UUID id, boolean deliveryEnabled, BigDecimal deliveryFee, BigDecimal freeDeliveryAbove,
                                 boolean waiveDeliveryForSubscribers, boolean subscriptionCoversRental,
                                 int pendingHoldMinutes, int cancellationCutoffDays, int maxItemsPerOrder,
                                 LocalDateTime updatedAt) {}

    public record StatsResponse(long totalOrders, long todayOrders, long stuckCount,
                                Map<String, Long> countsByStatus, Map<String, Long> countsByFulfilment,
                                BigDecimal collected, BigDecimal depositHeld, BigDecimal refundPending) {}

    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
}
