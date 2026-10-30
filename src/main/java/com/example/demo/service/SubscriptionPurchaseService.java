package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.PaymentDtos.TransactionDetailDto;
import com.example.demo.dto.SubscriptionPurchaseDto;
import com.example.demo.dto.SubscriptionPurchaseDto.*;
import com.example.demo.entity.AuditEventType;
import com.example.demo.entity.SubscriptionPlanEntity;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionStatus;
import com.example.demo.entity.UserSubscriptionEntity;
import com.example.demo.entity.UserSubscriptionEntity.Status;
import com.example.demo.repository.AirpayIpnLogRepository;
import com.example.demo.repository.UserSubscriptionRepository;
import com.example.demo.util.LocalINDTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Subscription purchase lifecycle + admin management.
 *
 * Actor-agnostic (like SubscriptionPlanService): the Super Admin and Sub-Admin controllers both call the
 * admin methods here and pass actorId / actorName / actorRole / ip for the audit trail.
 *
 * Payment-driven methods (createOrReusePending, onPaymentResolved, markInitFailed, expireOverdue) are called by
 * UserSubscriptionService / AirpayService / the scheduler.
 *
 * SAFETY RULES (real money):
 *   - A subscription becomes ACTIVE only via (a) a VALID-hash Airpay IPN whose amount equals the snapshotted
 *     plan price, or (b) an explicit admin confirm. Anything else -> NEEDS_REVIEW, never active.
 *   - Every transition runs under a row lock and checks the current status first -> duplicate IPNs are no-ops.
 *   - ACTIVE / EXPIRED / REVOKED / REFUNDED are never downgraded by late FAILED / CANCELLED callbacks.
 */
@Service
public class SubscriptionPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPurchaseService.class);

    public static final String ORDER_PREFIX = "SUB";
    private static final int PENDING_REUSE_MINUTES = 30;
    private static final String MODULE = "SUBSCRIPTION_PURCHASE_MANAGEMENT";

    private static final List<Status> PAID_STATUSES = List.of(Status.ACTIVE, Status.EXPIRED, Status.REVOKED);
    private static final Set<Status> CONFIRMABLE = EnumSet.of(
            Status.PENDING_PAYMENT, Status.FAILED, Status.CANCELLED, Status.NEEDS_REVIEW);
    private static final Set<Status> ACTIVATED_OR_LATER = EnumSet.of(
            Status.ACTIVE, Status.EXPIRED, Status.REVOKED, Status.REFUNDED);

    private final UserSubscriptionRepository repo;
    private final TransactionService transactionService;
    private final AirpayIpnLogRepository ipnLogRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)
    private final ObjectMapper mapper = new ObjectMapper();
    private final DepositService depositService;
 // add as the last constructor parameter and assign: this.depositService = depositService;

    public SubscriptionPurchaseService(UserSubscriptionRepository repo,
                                       TransactionService transactionService,
                                       AirpayIpnLogRepository ipnLogRepository,
                                       SuperAdminService superAdminService,
                                       DepositService depositService) {
        this.repo = repo;
        this.transactionService = transactionService;
        this.ipnLogRepository = ipnLogRepository;
        this.superAdminService = superAdminService;
        this.depositService=depositService;
    }

    // ═══════════════════════════════════════════
    //  PAYMENT-DRIVEN LIFECYCLE
    // ═══════════════════════════════════════════

    /** Creates a PENDING_PAYMENT purchase (price snapshotted from the DB plan) or reuses a recent one. */
    @Transactional
    public UserSubscriptionEntity createOrReusePending(String username, SubscriptionPlanEntity plan) {
//        LocalDateTime now = LocalINDTime.now();
//        Optional<UserSubscriptionEntity> existing = repo
//                .findFirstByUsernameAndPlanIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
//                        username, plan.getId(), Status.PENDING_PAYMENT, now.minusMinutes(PENDING_REUSE_MINUTES));
//
//        UserSubscriptionEntity p = existing.orElseGet(UserSubscriptionEntity::new);
    	
        LocalDateTime now = LocalINDTime.now();

        // Always start a fresh purchase row with a NEW order id (never reuse a pending one).
        // Airpay can reject an order id it has already seen, so each attempt needs its own id.
        // Old abandoned PENDING rows are cleaned up by expireStalePending() below.
        UserSubscriptionEntity p = new UserSubscriptionEntity();
    	
        boolean isNew = p.getId() == null;
        if (isNew) {
            p.setUsername(username);
            p.setOrderId(newOrderId(now));
            p.setStatus(Status.PENDING_PAYMENT);
        }
        // the snapshot is always re-read from the DB plan, never from the request
//        p.setPlanId(plan.getId());
//        p.setPlanName(plan.getPlanName());
        p.setCreatedAt(now);
        p.setPlanId(plan.getId());
        p.setPlanName(plan.getPlanName());
        p.setPrice(plan.getPrice());
        p.setCurrency(plan.getCurrency());
        p.setDurationValue(plan.getDurationValue());
        p.setDurationUnit(plan.getDurationUnit());
        p = repo.save(p);
        
      
        p.setDepositAmount(plan.isDepositRequired() && plan.getDepositAmount() != null
                ? plan.getDepositAmount() : BigDecimal.ZERO);          // <-- NEW

        if (isNew) {
            sysAudit(p.getOrderId(), AuditEventType.SUBSCRIPTION_EVENT, "USER:" + username,
                    json(Map.of("after", state(p))), "[SUBSCRIPTION] Purchase created for plan " + p.getPlanName());
        }
        return p;
    }

    /** Called when Airpay could not even be reached (token / init failure). Only touches PENDING purchases. */
    @Transactional
    public void markInitFailed(String orderId, String reason) {
        repo.findByOrderIdForUpdate(orderId).ifPresent(p -> {
            if (p.getStatus() != Status.PENDING_PAYMENT) return;
            Map<String, Object> before = state(p);
            p.setStatus(Status.FAILED);
            p.setStatusReason(truncate("Payment could not be started: " + reason, 500));
            repo.save(p);
            sysAudit(orderId, AuditEventType.ERROR, "SYSTEM",
                    json(Map.of("before", before, "after", state(p))), "[SUBSCRIPTION] Payment init failed");
        });
    }

    /**
     * Single entry point for Airpay results (called from AirpayService.resolveIpn). Idempotent.
     *
     * @param hashStatus VALID | MISMATCH | NOT_PRESENT
     * @param ipnAmount  amount reported by Airpay (covered by the secure hash)
     */
    @Transactional
    public void onPaymentResolved(String orderId, TransactionStatus resolved, String hashStatus,
                                  String ipnAmount, String message) {
        Optional<UserSubscriptionEntity> opt = repo.findByOrderIdForUpdate(orderId);
        if (opt.isEmpty()) {
            log.error("orderId={} payment result but no subscription purchase exists", orderId);
            sysAudit(orderId, AuditEventType.ERROR, "SYSTEM", null,
                    "[SUBSCRIPTION] Payment result received but no subscription purchase found");
            return;
        }
        UserSubscriptionEntity p = opt.get();
        Map<String, Object> before = state(p);

        if (resolved == TransactionStatus.SUCCESS) {
            if (ACTIVATED_OR_LATER.contains(p.getStatus())) {
                sysAudit(orderId, AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM", null,
                        "[SUBSCRIPTION] Duplicate success IPN ignored (status already " + p.getStatus() + ")");
                return;
            }
            if (!"VALID".equals(hashStatus)) {
                moveToReview(p, before, "Airpay reported success but secure hash status was " + hashStatus);
                return;
            }
            if (!amountMatches(p, ipnAmount)) {
//                moveToReview(p, before, "Paid amount " + ipnAmount + " does not match plan price " + p.getPrice());
                moveToReview(p, before, "Paid amount " + ipnAmount + " does not match the expected amount sent to Airpay (plan price " + p.getPrice() + " + deposit)");
                return;
            }
            activate(p, "AUTO", "Payment confirmed by Airpay IPN");
            p = repo.save(p);
            depositService.recordReceived(p, new BigDecimal(ipnAmount.trim()));  
            sysAudit(orderId, AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM",
                    json(Map.of("before", before, "after", state(p))), "[SUBSCRIPTION] ACTIVATED automatically");
            return;
        }

        // non-success results may only affect a purchase that is still waiting
        if (p.getStatus() != Status.PENDING_PAYMENT) {
            sysAudit(orderId, AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM", null,
                    "[SUBSCRIPTION] " + resolved + " IPN ignored (purchase already " + p.getStatus() + ")");
            return;
        }
        if (resolved == TransactionStatus.HASH_MISMATCH) {
            moveToReview(p, before, "IPN secure hash mismatch");
            return;
        }
        p.setStatus(resolved == TransactionStatus.CANCELLED ? Status.CANCELLED : Status.FAILED);
        p.setStatusReason(truncate(message, 500));
        p = repo.save(p);
        sysAudit(orderId, AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM",
                json(Map.of("before", before, "after", state(p))), "[SUBSCRIPTION] Marked " + p.getStatus());
    }

    /** Hourly-ish housekeeping: ACTIVE rows whose end date has passed become EXPIRED. Needs @EnableScheduling. */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Kolkata")
    @Transactional
    public void expireOverdue() {
        List<UserSubscriptionEntity> overdue = repo.findByStatusAndEndDateBefore(Status.ACTIVE, LocalINDTime.now());
        for (UserSubscriptionEntity p : overdue) {
            Map<String, Object> before = state(p);
            p.setStatus(Status.EXPIRED);
            repo.save(p);
            sysAudit(p.getOrderId(), AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM",
                    json(Map.of("before", before, "after", state(p))), "[SUBSCRIPTION] EXPIRED (end date reached)");
        }
        if (!overdue.isEmpty()) log.info("Expired {} subscription(s)", overdue.size());
    }

    // ═══════════════════════════════════════════
    //  ADMIN: READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResult<PurchaseResponse> list(String status, String username, String orderId, UUID planId,
                                             LocalDate from, LocalDate to, int page, int size) {
        Status st = parseStatus(status);
        Specification<UserSubscriptionEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (st != null) p.add(cb.equal(root.get("status"), st));
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            if (orderId != null && !orderId.isBlank()) p.add(cb.equal(root.get("orderId"), orderId.trim()));
            if (planId != null) p.add(cb.equal(root.get("planId"), planId));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Page<UserSubscriptionEntity> result = repo.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PurchaseDetailResponse getDetail(String orderId) {
        UserSubscriptionEntity p = find(orderId);
        TransactionDetailDto txn = transactionService.findByOrderId(orderId)
                .map(transactionService::toDetailDto).orElse(null);
        List<IpnLogRow> ipn = ipnLogRepository.findByOrderIdOrderByReceivedAtAsc(orderId).stream()
                .map(l -> new IpnLogRow(l.getId(), l.getApTransactionId(), l.getAmount(), l.getTransactionStatus(),
                        l.getTransactionPaymentStatus(), l.getMessage(), l.getChmod(), l.getSourceIp(),
                        l.getReceivedAt()))
                .toList();
        return new PurchaseDetailResponse(toResponse(p), txn, ipn, getAuditHistory(orderId));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditHistory(String orderId) {
        return transactionService.getAuditLogs(orderId).stream().map(transactionService::toAuditDto).toList();
    }

    /** Purchases waiting (PENDING_PAYMENT) or flagged (NEEDS_REVIEW) for longer than N minutes. */
    @Transactional(readOnly = true)
    public List<PurchaseResponse> findStuck(int olderThanMinutes) {
        LocalDateTime cutoff = LocalINDTime.now().minusMinutes(Math.max(olderThanMinutes, 1));
        return repo.findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                        List.of(Status.PENDING_PAYMENT, Status.NEEDS_REVIEW), cutoff)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        LocalDateTime now = LocalINDTime.now();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Status s : Status.values()) counts.put(s.name(), repo.countByStatus(s));

        List<PlanRevenueRow> byPlan = repo.revenueByPlan(PAID_STATUSES).stream()
                .map(r -> new PlanRevenueRow((String) r[0], ((Number) r[1]).longValue(),
                        r[2] == null ? BigDecimal.ZERO : (BigDecimal) r[2]))
                .toList();

        return new StatsResponse(
                repo.count(),
                repo.countByStatusAndStartDateLessThanEqualAndEndDateAfter(Status.ACTIVE, now, now),
                repo.findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                        List.of(Status.PENDING_PAYMENT, Status.NEEDS_REVIEW), now.minusMinutes(30)).size(),
                counts,
                nz(repo.sumPrice(PAID_STATUSES)),
                nz(repo.sumPriceSince(PAID_STATUSES, now.toLocalDate().atStartOfDay())),
                nz(repo.sumPrice(List.of(Status.REFUNDED))),
                byPlan);
    }

    // ═══════════════════════════════════════════
    //  ADMIN: MANUAL ACTIONS (each one audited with before/after)
    // ═══════════════════════════════════════════

    /** Admin verified the payment on the Airpay dashboard -> activate. */
    @Transactional
    public PurchaseResponse confirm(String orderId, ConfirmRequest req,
                                    UUID actorId, String actorName, String actorRole, String ip) {
        UserSubscriptionEntity p = lock(orderId);
        if (!CONFIRMABLE.contains(p.getStatus())) {
            throw new SuperAdminService.BadRequestException(
                    "Cannot confirm a purchase in status " + p.getStatus() + ". Allowed: " + CONFIRMABLE);
        }
        Transaction txn = transactionService.findByOrderId(orderId).orElseThrow(() ->
                new SuperAdminService.BadRequestException(
                        "No Airpay payment attempt exists for this order — nothing to confirm."));

        Map<String, Object> before = state(p);
        transactionService.markManualAdminConfirmed(txn, req.getApTransactionId(), actorName, req.getReason());
        activate(p, "ADMIN", "Manually confirmed by " + actorName + ": " + req.getReason());
        p = repo.save(p);
        depositService.recordReceived(p, txn.getAmount());    

        adminAudit("MANUAL_CONFIRM", p, txn, before, state(p), req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(p);
    }

    /** Admin verified that the payment really failed. Refuses if Airpay says money was received. */
    @Transactional
    public PurchaseResponse reject(String orderId, ReasonRequest req,
                                   UUID actorId, String actorName, String actorRole, String ip) {
        UserSubscriptionEntity p = lock(orderId);
        if (p.getStatus() != Status.PENDING_PAYMENT && p.getStatus() != Status.NEEDS_REVIEW) {
            throw new SuperAdminService.BadRequestException(
                    "Only PENDING_PAYMENT or NEEDS_REVIEW purchases can be rejected (current: " + p.getStatus() + ")");
        }
        Transaction txn = transactionService.findByOrderId(orderId).orElse(null);
        if (txn != null && txn.getStatus() == TransactionStatus.SUCCESS) {
            throw new SuperAdminService.BadRequestException(
                    "Airpay recorded this payment as SUCCESS — use confirm, or mark-refunded if you are returning the money.");
        }

        Map<String, Object> before = state(p);
        if (txn != null) transactionService.markManualAdminRejected(txn, actorName, req.getReason());
        p.setStatus(Status.FAILED);
        p.setStatusReason(truncate("Rejected by " + actorName + ": " + req.getReason(), 500));
        p = repo.save(p);

        adminAudit("MANUAL_REJECT", p, txn, before, state(p), req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(p);
    }

    /** Removes access from an ACTIVE subscription (money is not returned — use mark-refunded for that). */
    @Transactional
    public PurchaseResponse revoke(String orderId, ReasonRequest req,
                                   UUID actorId, String actorName, String actorRole, String ip) {
        UserSubscriptionEntity p = lock(orderId);
        if (p.getStatus() != Status.ACTIVE) {
            throw new SuperAdminService.BadRequestException("Only ACTIVE subscriptions can be revoked (current: " + p.getStatus() + ")");
        }
        Map<String, Object> before = state(p);
        p.setStatus(Status.REVOKED);
        p.setRevokedAt(LocalINDTime.now());
        p.setStatusReason(truncate("Revoked by " + actorName + ": " + req.getReason(), 500));
        p = repo.save(p);

        adminAudit("REVOKE", p, null, before, state(p), req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(p);
    }

    /**
     * Records that the buyer was refunded. This does NOT move money — do the refund on the Airpay dashboard first,
     * then record it here. Access is removed and the amount moves from "revenue" to "refunded".
     */
    @Transactional
    public PurchaseResponse markRefunded(String orderId, RefundRequest req,
                                         UUID actorId, String actorName, String actorRole, String ip) {
        UserSubscriptionEntity p = lock(orderId);
        Set<Status> allowed = EnumSet.of(Status.ACTIVE, Status.EXPIRED, Status.REVOKED, Status.NEEDS_REVIEW);
        if (!allowed.contains(p.getStatus())) {
            throw new SuperAdminService.BadRequestException(
                    "Only a paid purchase can be marked refunded (current: " + p.getStatus() + "). Allowed: " + allowed);
        }
        Map<String, Object> before = state(p);
        p.setStatus(Status.REFUNDED);
        p.setRefundedAt(LocalINDTime.now());
        p.setRefundReference(req.getRefundReference());
        p.setStatusReason(truncate("Refunded (recorded by " + actorName + "): " + req.getReason(), 500));
        p = repo.save(p);

        adminAudit("MARK_REFUNDED", p, null, before, state(p), req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(p);
    }

    // ═══════════════════════════════════════════
    //  MAPPERS (public — reused by UserSubscriptionService)
    // ═══════════════════════════════════════════

    public UserPurchaseResponse toUserResponse(UserSubscriptionEntity p) {
        return new UserPurchaseResponse(p.getId(), p.getPlanId(), p.getPlanName(), p.getPrice(), p.getCurrency(),
                p.getDurationValue(), p.getDurationUnit(), p.getOrderId(), p.getStatus().name(),
                p.getStartDate(), p.getEndDate(), p.getPaidAt(), isRunning(p));
    }

    public PurchaseResponse toResponse(UserSubscriptionEntity p) {
        return new PurchaseResponse(p.getId(), p.getUsername(), p.getPlanId(), p.getPlanName(), p.getPrice(),
                p.getCurrency(), p.getDurationValue(), p.getDurationUnit(), p.getOrderId(), p.getStatus().name(),
                p.getStartDate(), p.getEndDate(), p.getPaidAt(), p.getActivatedBy(), p.getStatusReason(),
                p.getRefundReference(), p.getRefundedAt(), p.getRevokedAt(), p.getCreatedAt(), p.getUpdatedAt(),
                isRunning(p));
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    /** Start = now, or the end of the user's current ACTIVE subscription if they are renewing early. */
    private void activate(UserSubscriptionEntity p, String by, String reason) {
        LocalDateTime now = LocalINDTime.now();
        LocalDateTime latestEnd = repo.findLatestEnd(p.getUsername(), Status.ACTIVE, now);
        LocalDateTime start = (latestEnd != null && latestEnd.isAfter(now)) ? latestEnd : now;

        p.setStartDate(start);
        p.setEndDate(addDuration(start, p.getDurationValue(), p.getDurationUnit()));
        p.setStatus(Status.ACTIVE);
        if (p.getPaidAt() == null) p.setPaidAt(now);
        p.setActivatedBy(by);
        p.setStatusReason(truncate(reason, 500));
    }

    private LocalDateTime addDuration(LocalDateTime start, int value, String unit) {
        return switch (unit == null ? "" : unit.toUpperCase()) {
            case "DAY" -> start.plusDays(value);
            case "MONTH" -> start.plusMonths(value);
            case "YEAR" -> start.plusYears(value);
            default -> throw new IllegalStateException("Unknown duration unit: " + unit);
        };
    }

    private void moveToReview(UserSubscriptionEntity p, Map<String, Object> before, String reason) {
        p.setStatus(Status.NEEDS_REVIEW);
        p.setStatusReason(truncate(reason, 500));
        repo.save(p);
        log.warn("orderId={} -> NEEDS_REVIEW: {}", p.getOrderId(), reason);
        sysAudit(p.getOrderId(), AuditEventType.ERROR, "SYSTEM",
                json(Map.of("before", before, "after", state(p))), "[SUBSCRIPTION] NEEDS_REVIEW: " + reason);
    }

    private boolean amountMatches(UserSubscriptionEntity p, String ipnAmount) {
        try {
            if (ipnAmount == null) return false;
            BigDecimal paid = new BigDecimal(ipnAmount.trim());
            // expected = amount actually sent to Airpay (price + deposit), stored on the transaction row
            BigDecimal expected = transactionService.findByOrderId(p.getOrderId())
                    .map(Transaction::getAmount)
                    .orElse(p.getPrice());
            return paid.compareTo(expected) == 0;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isRunning(UserSubscriptionEntity p) {
        LocalDateTime now = LocalINDTime.now();
        return p.getStatus() == Status.ACTIVE && p.getStartDate() != null && p.getEndDate() != null
                && !p.getStartDate().isAfter(now) && p.getEndDate().isAfter(now);
    }

    private UserSubscriptionEntity find(String orderId) {
        return repo.findByOrderId(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Subscription purchase not found: " + orderId));
    }

    private UserSubscriptionEntity lock(String orderId) {
        return repo.findByOrderIdForUpdate(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Subscription purchase not found: " + orderId));
    }

    private Status parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Status.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SuperAdminService.BadRequestException("Invalid status. Must be one of: " + Arrays.toString(Status.values()));
        }
    }

    private String newOrderId(LocalDateTime now) {
        // SUB (3) + yyMMddHHmmss (12) + 5 random = 20 chars (Airpay max is 20, alphanumeric only)
        return ORDER_PREFIX + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    }

    /** Global audit (audit_logs) + order-level audit (transaction_audit_logs) for every admin action. */
    private void adminAudit(String action, UserSubscriptionEntity p, Transaction txn,
                            Map<String, Object> before, Map<String, Object> after, String reason,
                            UUID actorId, String actorName, String actorRole, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action, MODULE, p.getOrderId(), before, after, ip);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("reason", reason);
        payload.put("actor", actorName);
        payload.put("role", actorRole);
        payload.put("ip", ip);
        payload.put("before", before);
        payload.put("after", after);
        transactionService.logAudit(p.getOrderId(), txn, AuditEventType.ADMIN_ACTION,
                actorName + " (" + actorRole + ")", json(payload), "[SUBSCRIPTION] " + action + ": " + reason);
    }

    private void sysAudit(String orderId, AuditEventType type, String actor, String payload, String remarks) {
        Transaction txn = transactionService.findByOrderId(orderId).orElse(null);
        transactionService.logAudit(orderId, txn, type, actor, payload, remarks);
    }

    private Map<String, Object> state(UserSubscriptionEntity p) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", p.getId() == null ? null : p.getId().toString());
        s.put("orderId", p.getOrderId());
        s.put("username", p.getUsername());
        s.put("planName", p.getPlanName());
        s.put("price", p.getPrice());
        s.put("status", p.getStatus() == null ? null : p.getStatus().name());
        s.put("startDate", p.getStartDate() == null ? null : p.getStartDate().toString());
        s.put("endDate", p.getEndDate() == null ? null : p.getEndDate().toString());
        s.put("paidAt", p.getPaidAt() == null ? null : p.getPaidAt().toString());
        s.put("activatedBy", p.getActivatedBy());
        s.put("statusReason", p.getStatusReason());
        return s;
    }

    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"could not serialize audit payload\"}";
        }
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String truncate(String s, int max) { return s == null || s.length() <= max ? s : s.substring(0, max); }
}
