package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.*;
import com.example.demo.entity.AuditEventType;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionAuditLog;
import com.example.demo.entity.TransactionStatus;
import com.example.demo.repository.TransactionAuditLogRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.util.LocalINDTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Single place where every Airpay transaction row and its audit trail is written.
 * Used by AirpayService (initiate / IPN) and SubscriptionPurchaseService (manual admin actions).
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionAuditLogRepository auditLogRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionAuditLogRepository auditLogRepository) {
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ───────────────────────── initiate ─────────────────────────

    @Transactional
    public Transaction createOrReuse(PaymentRequest req, String merchantId, String currencyCode,
                                     String isoCurrency, String kittype, String clientIp) {
        Optional<Transaction> existing = transactionRepository.findByOrderId(req.orderid());

        if (existing.isPresent()) {
            Transaction txn = existing.get();
            if (txn.getStatus() == TransactionStatus.SUCCESS) {
                throw new DuplicateOrderException(
                        "Order ID '" + req.orderid() + "' has already been paid successfully");
            }
            applyRequestFields(txn, req, merchantId, currencyCode, isoCurrency, kittype, clientIp);
            txn.setRetryCount(txn.getRetryCount() + 1);
            txn.setStatus(TransactionStatus.INITIATED);
            txn.setErrorMessage(null);
            log.info("Reusing transaction row for orderId={} (retry #{})", req.orderid(), txn.getRetryCount());
            return transactionRepository.save(txn);
        }

        Transaction txn = new Transaction();
        applyRequestFields(txn, req, merchantId, currencyCode, isoCurrency, kittype, clientIp);
        txn.setStatus(TransactionStatus.INITIATED);
        log.info("Creating transaction row for orderId={}", req.orderid());
        return transactionRepository.save(txn);
    }

    private void applyRequestFields(Transaction txn, PaymentRequest req, String merchantId,
                                    String currencyCode, String isoCurrency, String kittype, String clientIp) {
        txn.setOrderId(req.orderid());
        txn.setMerchantId(merchantId);
        txn.setBuyerEmail(req.buyerEmail());
        txn.setBuyerPhone(req.buyerPhone());
        txn.setBuyerFirstName(req.buyerFirstName());
        txn.setBuyerLastName(req.buyerLastName());
        txn.setBuyerAddress(req.buyerAddress());
        txn.setBuyerCity(req.buyerCity());
        txn.setBuyerState(req.buyerState());
        txn.setBuyerPincode(req.buyerPinCode());
        txn.setBuyerCountry(req.buyerCountry());
        txn.setAmount(new BigDecimal(req.amount()));
        txn.setCurrencyCode(currencyCode);
        txn.setIsoCurrency(isoCurrency);
        txn.setCustomvar(req.customvar());
        txn.setKittype(kittype);
        txn.setTxnSubType(req.txnsubtype());
        txn.setClientIp(clientIp);
    }

    @Transactional
    public void markTokenFailed(Transaction txn, String errorMessage) {
        txn.setStatus(TransactionStatus.TOKEN_FAILED);
        txn.setErrorMessage(errorMessage);
        transactionRepository.save(txn);
    }

    @Transactional
    public void markInitError(Transaction txn, String errorMessage) {
        txn.setStatus(TransactionStatus.ERROR);
        txn.setErrorMessage(errorMessage);
        transactionRepository.save(txn);
    }

    @Transactional
    public void markAwaitingPayment(Transaction txn, String paymentUrl, String checksum, String privateKey) {
        txn.setStatus(TransactionStatus.AWAITING_PAYMENT);
        txn.setPaymentUrl(paymentUrl);
        txn.setRequestChecksum(checksum);
        txn.setPrivateKey(privateKey);
        transactionRepository.save(txn);
    }

    // ───────────────────────── IPN result ─────────────────────────

    /** Never downgrades a SUCCESS transaction (defence against late / forged FAILED callbacks). */
    @Transactional
    public void applyCallbackResult(Transaction txn, String airpayStatusCode, String message,
                                    String apTransactionId, String secureHashStatus,
                                    TransactionStatus resolvedStatus) {
        if (txn.getStatus() == TransactionStatus.SUCCESS && resolvedStatus != TransactionStatus.SUCCESS) {
            log.warn("orderId={} already SUCCESS — ignoring callback result {}", txn.getOrderId(), resolvedStatus);
            return;
        }
        txn.setAirpayStatusCode(airpayStatusCode);
        txn.setMessage(message);
        txn.setApTransactionId(apTransactionId);
        txn.setSecureHashStatus(secureHashStatus);
        txn.setStatus(resolvedStatus);
        if (resolvedStatus == TransactionStatus.SUCCESS
                || resolvedStatus == TransactionStatus.FAILED
                || resolvedStatus == TransactionStatus.CANCELLED) {
            txn.setCompletedAt(LocalINDTime.now());
        }
        transactionRepository.save(txn);
        log.info("orderId={} resolved to status={}", txn.getOrderId(), resolvedStatus);
    }

    // ───────────────────────── admin manual overrides ─────────────────────────

    @Transactional
    public void markManualAdminConfirmed(Transaction txn, String apTransactionId, String adminName, String reason) {
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setSecureHashStatus("ADMIN_MANUAL_VERIFIED");
        if (apTransactionId != null && !apTransactionId.isBlank()) txn.setApTransactionId(apTransactionId.trim());
        txn.setMessage(truncate("Manually confirmed by admin (" + adminName + "): " + reason, 500));
        txn.setCompletedAt(LocalINDTime.now());
        transactionRepository.save(txn);
    }

    @Transactional
    public void markManualAdminRejected(Transaction txn, String adminName, String reason) {
        txn.setStatus(TransactionStatus.FAILED);
        txn.setSecureHashStatus("ADMIN_MANUAL_REJECTED");
        txn.setMessage(truncate("Manually rejected by admin (" + adminName + "): " + reason, 500));
        txn.setCompletedAt(LocalINDTime.now());
        transactionRepository.save(txn);
    }

    // ───────────────────────── audit ─────────────────────────

    @Transactional
    public void logAudit(Transaction txn, AuditEventType type, String payload, String remarks) {
        logAudit(txn != null ? txn.getOrderId() : null, txn, type, "SYSTEM", payload, remarks);
    }

    @Transactional
    public void logAudit(String orderId, Transaction txn, AuditEventType type, String actor,
                         String payload, String remarks) {
        TransactionAuditLog entry = new TransactionAuditLog();
        entry.setOrderId(orderId);
        entry.setTransaction(txn);
        entry.setEventType(type);
        entry.setActor(truncate(actor, 200));
        entry.setPayload(payload);
//        entry.setRemarks(truncate(remarks, 1000));
        entry.setRemarks(truncate(remarks, 1000));
        entry.setCreatedAt(LocalINDTime.now());
        auditLogRepository.save(entry);
        auditLogRepository.save(entry);
    }

    // ───────────────────────── lookups / mappers ─────────────────────────

    public Optional<Transaction> findByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    public List<TransactionAuditLog> getAuditLogs(String orderId) {
        return auditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    public TransactionDetailDto toDetailDto(Transaction t) {
        return new TransactionDetailDto(t.getId(), t.getOrderId(), t.getMerchantId(), t.getBuyerEmail(),
                t.getBuyerPhone(), t.getBuyerFirstName(), t.getBuyerLastName(), t.getAmount(),
                t.getCurrencyCode(), t.getStatus() != null ? t.getStatus().name() : null,
                t.getApTransactionId(), t.getAirpayStatusCode(), t.getMessage(), t.getSecureHashStatus(),
                t.getErrorMessage(), t.getClientIp(), t.getRetryCount(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt());
    }

    public AuditLogDto toAuditDto(TransactionAuditLog l) {
        return new AuditLogDto(l.getId(), l.getOrderId(),
                l.getEventType() != null ? l.getEventType().name() : null,
                l.getActor(), l.getPayload(), l.getRemarks(), l.getCreatedAt());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
