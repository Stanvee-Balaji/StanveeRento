package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Airpay-internal DTOs. PaymentRequest is built by the server only (never bound from a client body). */
public final class PaymentDtos {

    private PaymentDtos() {}

    public record PaymentRequest(String orderid, String buyerEmail, String buyerPhone,
                                 String buyerFirstName, String buyerLastName, String buyerAddress,
                                 String amount, String buyerCity, String buyerState,
                                 String buyerPinCode, String buyerCountry,
                                 String customvar, String txnsubtype) {}

    public record PaymentInitResponse(String orderId, String paymentUrl, String privateKey,
                                      String merchantId, String encdata, String checksum,
                                      String kittype, String chmod) {}

    public record TransactionDetailDto(Long id, String orderId, String merchantId, String buyerEmail,
                                       String buyerPhone, String buyerFirstName, String buyerLastName,
                                       BigDecimal amount, String currencyCode, String status,
                                       String apTransactionId, String airpayStatusCode, String message,
                                       String secureHashStatus, String errorMessage, String clientIp,
                                       int retryCount, LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime completedAt) {}

    public record AuditLogDto(Long id, String orderId, String eventType, String actor,
                              String payload, String remarks, LocalDateTime createdAt) {}

    public static class DuplicateOrderException extends RuntimeException {
        public DuplicateOrderException(String message) { super(message); }
    }
}
