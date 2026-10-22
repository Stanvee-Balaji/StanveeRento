package com.example.demo.controller;

import com.example.demo.entity.AirpayIpnLog;
import com.example.demo.repository.AirpayIpnLogRepository;
import com.example.demo.service.AirpayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Airpay endpoints that must stay public (no JWT):
 *   GET  /api/payment/health
 *   POST /api/payment/ipn      server-to-server notification from Airpay
 *
 * There is intentionally NO public "initiate" endpoint here: payments are started only through
 * POST /api/v1/user/subscriptions/purchase so the amount can never come from the client.
 *
 * Register in the Airpay merchant dashboard:  https://<your-api-domain>/api/payment/ipn
 */
@RestController
@RequestMapping("/api/payment")
public class AirpayController {

    private static final Logger log = LoggerFactory.getLogger(AirpayController.class);

    private final AirpayService airpayService;
    private final AirpayIpnLogRepository ipnLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AirpayController(AirpayService airpayService, AirpayIpnLogRepository ipnLogRepository) {
        this.airpayService = airpayService;
        this.ipnLogRepository = ipnLogRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Airpay subscription payments"));
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> handleIpn(@RequestBody(required = false) Map<String, String> body,
                                            HttpServletRequest request) {
        String sourceIp = resolveClientIp(request);

        if (body == null || body.isEmpty()) {
            log.warn("IPN with empty body | ip={}", sourceIp);
            return ResponseEntity.ok("OK");
        }
        String encrypted = body.get("response");
        if (encrypted == null || encrypted.isBlank()) {
            log.warn("IPN missing 'response' | ip={} | keys={}", sourceIp, body.keySet());
            return ResponseEntity.ok("OK");
        }

        // 1) decrypt + verify hash. A payload we cannot decrypt can never be valid -> 200 so Airpay stops retrying.
        Map<String, Object> ipnData;
        try {
            ipnData = airpayService.decryptIpnPayload(encrypted);
        } catch (Exception e) {
            log.error("IPN decrypt failed | ip={} | error={}", sourceIp, e.getMessage());
            return ResponseEntity.ok("OK");
        }

        // 2) ALWAYS keep the raw record first (reconciliation evidence), even if step 3 fails.
        try {
            ipnLogRepository.save(buildIpnLog(ipnData, sourceIp));
        } catch (Exception e) {
            log.error("IPN log save failed | orderId={} | {}", ipnData.get("orderid"), e.getMessage(), e);
        }

        // 3) resolve. resolveIpn is idempotent, so on an internal error we return 500 to let Airpay retry safely.
        try {
            airpayService.resolveIpn(ipnData);
        } catch (Exception e) {
            log.error("IPN resolve failed | orderId={} | {}", ipnData.get("orderid"), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("RETRY");
        }
        return ResponseEntity.ok("OK");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private AirpayIpnLog buildIpnLog(Map<String, Object> d, String sourceIp) throws Exception {
        AirpayIpnLog l = new AirpayIpnLog();
        l.setOrderId(str(d, "orderid"));
        l.setMerchantId(str(d, "merchant_id"));
        l.setApTransactionId(str(d, "ap_transactionid"));
        l.setAmount(str(d, "amount"));
        l.setTransactionStatus(str(d, "transaction_status"));
        l.setTransactionPaymentStatus(str(d, "transaction_payment_status"));
        l.setMessage(str(d, "message"));
        l.setApSecureHash(str(d, "ap_securehash"));
        l.setChmod(str(d, "chmod"));
        l.setCustomerName(str(d, "customer_name"));
        l.setCustomerEmail(str(d, "customer_email"));
        l.setCustomerPhone(str(d, "customer_phone"));
        l.setCustomerVpa(str(d, "customer_vpa"));
        l.setBankName(str(d, "bank_name"));
        l.setCardScheme(str(d, "card_scheme"));
        l.setCurrencyCode(str(d, "currency_code"));
        l.setCustomvar(str(d, "customvar"));
        l.setBilledAmount(str(d, "billed_amount"));
        l.setSurchargeAmount(str(d, "surcharge_amount"));
        l.setTransactionTime(str(d, "transaction_time"));
        l.setRawPayload(objectMapper.writeValueAsString(d));
        l.setSourceIp(sourceIp);
        return l;
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }
}
