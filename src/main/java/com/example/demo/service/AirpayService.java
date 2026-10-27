package com.example.demo.service;

import com.example.demo.config.AirpayConfig;
import com.example.demo.entity.AuditEventType;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.demo.dto.PaymentDtos.*;

/**
 * Airpay gateway integration (subscription-purchase edition).
 *
 *   initiatePayment  : token fetch -> payload encryption -> transaction row   (called ONLY by UserSubscriptionService,
 *                      so the amount always comes from the plan price in the DB)
 *   decryptIpnPayload: decrypt + CRC32 secure-hash check of Airpay's server-to-server IPN
 *   resolveIpn       : update the Transaction row, then hand the result to SubscriptionPurchaseService,
 *                      which is the ONLY place a subscription gets activated.
 */
@Service
public class AirpayService {

    private static final Logger log = LoggerFactory.getLogger(AirpayService.class);

    private final AirpayConfig config;
    private final CryptoUtil crypto;
    private final HttpUtil http;
    private final TransactionService transactionService;
    private final SubscriptionPurchaseService purchaseService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AirpayService(AirpayConfig config, CryptoUtil crypto, HttpUtil http,
                         TransactionService transactionService,
                         SubscriptionPurchaseService purchaseService) {
        this.config = config;
        this.crypto = crypto;
        this.http = http;
        this.transactionService = transactionService;
        this.purchaseService = purchaseService;
    }

    // =========================================================================
    // STEP 1 — Initiate payment
    // =========================================================================
    public PaymentInitResponse initiatePayment(PaymentRequest req, String clientIp) throws Exception {

        String username = config.getUsername();
        String password = config.getPassword();
        String secret = config.getSecret();
        String mercId = config.getMercId();

        String currency = "356";
        String isoCurrency = "INR";
        String kittype = "iframe";

        Transaction txn = transactionService.createOrReuse(req, mercId, currency, isoCurrency, kittype, clientIp);

        String secretKey = crypto.buildSecretKey(username, password);

        // ── token request (client_secret is NEVER written to the audit log) ──
        Map<String, String> tokenRequestMap = new LinkedHashMap<>();
        tokenRequestMap.put("client_id", config.getClientId());
        tokenRequestMap.put("client_secret", config.getClientSecret());
        tokenRequestMap.put("grant_type", "client_credentials");
        tokenRequestMap.put("merchant_id", mercId);

        String encTokenJson = crypto.encrypt(mapper.writeValueAsString(tokenRequestMap), secretKey);
        transactionService.logAudit(txn, AuditEventType.TOKEN_REQUEST, null, "Token requested");

        Map<String, String> tokenPostParams = new LinkedHashMap<>();
        tokenPostParams.put("merchant_id", mercId);
        tokenPostParams.put("encdata", encTokenJson);
        tokenPostParams.put("checksum", crypto.calculateChecksum(tokenRequestMap));

//        String tokenRawResponse;
//        try {
//            tokenRawResponse = http.sendPostData(config.getTokenUrl(), tokenPostParams);
//        } catch (Exception e) {
//            log.error("orderId={} — token API call failed: {}", req.orderid(), e.getMessage());
//            transactionService.markInitError(txn, "Token API call failed: " + e.getMessage());
//            throw e;
//        }
        
        String tokenRawResponse = null;
        Exception lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                tokenRawResponse = http.sendPostData(config.getTokenUrl(), tokenPostParams);
                lastError = null;
                break;
            } catch (java.net.SocketTimeoutException e) {
                lastError = e;
                log.warn("orderId={} token call timed out (attempt {}/3)", req.orderid(), attempt);
            } catch (Exception e) {
                lastError = e;
                break;   // non-timeout errors: do not retry
            }
        }
        if (lastError != null) {
            log.error("orderId={} — token API call failed: {}", req.orderid(), lastError.getMessage());
            transactionService.markInitError(txn, "Token API call failed: " + lastError.getMessage());
            throw lastError;
        }
        transactionService.logAudit(txn, AuditEventType.TOKEN_RESPONSE, tokenRawResponse, null);

        JsonNode tokenResponseNode = mapper.readTree(tokenRawResponse);
        if (!tokenResponseNode.has("response")) {
            transactionService.markInitError(txn, "Token API response missing 'response' field");
            throw new RuntimeException("Token API response missing 'response' field");
        }

        String decryptedToken;
        try {
            decryptedToken = crypto.decrypt(tokenResponseNode.get("response").asText(), secretKey);
        } catch (Exception e) {
            transactionService.markTokenFailed(txn, "Token decryption failed: " + e.getMessage());
            throw new RuntimeException("Token decryption failed: " + e.getMessage(), e);
        }

        JsonNode tokenData = mapper.readTree(decryptedToken);
        String responseCode = tokenData.path("response_code").asText("");
        if (!"00".equals(responseCode)) {
            String errMsg = tokenData.path("message").asText("Unknown error from Airpay");
            transactionService.markTokenFailed(txn, "[" + responseCode + "] " + errMsg);
            throw new RuntimeException("Airpay token error [" + responseCode + "]: " + errMsg);
        }

        String accessToken = tokenData.path("data").path("access_token").asText("");
        if (accessToken.isEmpty()) {
            transactionService.markTokenFailed(txn, "Empty access token returned by Airpay");
            throw new RuntimeException("Failed to obtain access token from Airpay");
        }

        String fullPaymentUrl = config.getPaymentUrl() + "?token=" + accessToken;
        String privateKey = crypto.buildPrivateKey(username, password, secret);

        // ── the 16-field payment map Airpay requires ──
        Map<String, String> dataMap = new LinkedHashMap<>();
        dataMap.put("buyer_email", req.buyerEmail());
        dataMap.put("buyer_phone", req.buyerPhone());
        dataMap.put("buyer_firstname", req.buyerFirstName());
        dataMap.put("buyer_lastname", req.buyerLastName());
        dataMap.put("buyer_address", nvl(req.buyerAddress()));
        dataMap.put("amount", req.amount());
        dataMap.put("buyer_city", nvl(req.buyerCity()));
        dataMap.put("buyer_state", nvl(req.buyerState()));
        dataMap.put("buyer_pincode", nvl(req.buyerPinCode()));
        dataMap.put("buyer_country", nvl(req.buyerCountry()));
        dataMap.put("orderid", req.orderid());
        dataMap.put("currency_code", currency);
        dataMap.put("iso_currency", isoCurrency);
        dataMap.put("merchant_id", mercId);
        dataMap.put("kittype", kittype);
        dataMap.put("customvar", nvl(req.customvar()));

        String checksum = crypto.calculateChecksum(dataMap);
        String paymentJson = mapper.writeValueAsString(dataMap);
        String encData = crypto.encrypt(paymentJson, secretKey);

        transactionService.markAwaitingPayment(txn, fullPaymentUrl, checksum, privateKey);
        transactionService.logAudit(txn, AuditEventType.PAYMENT_INIT_RESPONSE, paymentJson, "Ready for Airpay redirect");
        log.info("orderId={} ready for Airpay redirect", req.orderid());

        return new PaymentInitResponse(req.orderid(), fullPaymentUrl, privateKey, mercId,
                encData, checksum, kittype, "");
    }

    // =========================================================================
    // STEP 2 — Decrypt + verify IPN
    // =========================================================================
    public Map<String, Object> decryptIpnPayload(String encryptedResponseData) throws Exception {
        if (encryptedResponseData == null || encryptedResponseData.isBlank()) {
            throw new IllegalArgumentException("Empty IPN response payload");
        }
        String secretKey = crypto.buildSecretKey(config.getUsername(), config.getPassword());
        String decrypted = crypto.decrypt(encryptedResponseData, secretKey);
        if (decrypted == null || decrypted.isBlank()) {
            throw new RuntimeException("IPN payload was empty after decryption");
        }
        log.info("IPN decrypted payload: {}", decrypted);

        JsonNode root = mapper.readTree(decrypted);
        JsonNode dataObject = root.has("data") ? root.path("data") : root;

        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.convertValue(dataObject, Map.class);
        result.put("_secureHashStatus", validateIpnHash(dataObject));
        return result;
    }

    // =========================================================================
    // STEP 3 — Resolve IPN into Transaction + subscription state (idempotent)
    // =========================================================================
    public void resolveIpn(Map<String, Object> ipnData) {

        String orderId = str(ipnData, "orderid");
        String apTransactionId = str(ipnData, "ap_transactionid");
        String transtatus = str(ipnData, "transaction_status");   // 200 success, 402 cancelled, other failed
        String message = str(ipnData, "message");
        String amount = str(ipnData, "amount");
        String hashStatus = str(ipnData, "_secureHashStatus");

        if (orderId == null || orderId.isBlank()) {
            log.warn("IPN payload missing orderid — ignored");
            return;
        }

        Transaction txn = transactionService.findByOrderId(orderId).orElse(null);
        if (txn == null) {
            log.error("orderId={} IPN for an order we never initiated — ignored", orderId);
            transactionService.logAudit(orderId, null, AuditEventType.ERROR, "SYSTEM", null,
                    "IPN received for unknown order");
            return;
        }

        // A paid transaction is never downgraded by a late / duplicate / forged non-success IPN.
        if (txn.getStatus() == TransactionStatus.SUCCESS && !"200".equals(transtatus)) {
            transactionService.logAudit(orderId, txn, AuditEventType.CALLBACK_PROCESSED, "SYSTEM", null,
                    "Non-success IPN ignored because transaction is already SUCCESS (status=" + transtatus + ")");
            return;
        }

        if ("MISMATCH".equals(hashStatus)) {
            log.warn("orderId={} IPN secure hash MISMATCH — untrusted, subscription will NOT be activated", orderId);
            transactionService.applyCallbackResult(txn, transtatus, message, apTransactionId,
                    hashStatus, TransactionStatus.HASH_MISMATCH);
            transactionService.logAudit(orderId, txn, AuditEventType.ERROR, "SYSTEM", null,
                    "IPN secure hash mismatch");
            purchaseService.onPaymentResolved(orderId, TransactionStatus.HASH_MISMATCH, hashStatus, amount, message);
            return;
        }

        TransactionStatus resolved = "200".equals(transtatus) ? TransactionStatus.SUCCESS
                : "402".equals(transtatus) ? TransactionStatus.CANCELLED : TransactionStatus.FAILED;

        transactionService.applyCallbackResult(txn, transtatus, message, apTransactionId, hashStatus, resolved);
        transactionService.logAudit(orderId, txn, AuditEventType.CALLBACK_PROCESSED, "SYSTEM", null,
                "IPN resolvedStatus=" + resolved + " secureHashStatus=" + hashStatus);

        // THE GATE: the subscription is activated here, and only here (or by an admin override).
        purchaseService.onPaymentResolved(orderId, resolved, hashStatus, amount, message);
    }

    /** ap_securehash = CRC32(orderid:ap_transactionid:amount:transaction_status:message:mercid:username[:customer_vpa]) */
    private String validateIpnHash(JsonNode d) {
        if (!d.has("ap_securehash")) return "NOT_PRESENT";

        String chmod = d.path("chmod").asText("");
        String vpa = d.path("customer_vpa").asText("");
        long received = d.path("ap_securehash").asLong(0);

        String base = d.path("orderid").asText("") + ":" + d.path("ap_transactionid").asText("") + ":"
                + d.path("amount").asText("") + ":" + d.path("transaction_status").asText("") + ":"
                + d.path("message").asText("") + ":" + config.getMercId() + ":" + config.getUsername();

        if (received == crypto.computeCRC32(base)) return "VALID";
        if ("upi".equalsIgnoreCase(chmod) && !vpa.isBlank() && received == crypto.computeCRC32(base + ":" + vpa)) {
            return "VALID";
        }
        log.warn("IPN hash MISMATCH | orderId={} | chmod={}", d.path("orderid").asText(""), chmod);
        return "MISMATCH";
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }

    private String nvl(String value) { return value != null ? value : ""; }
}
