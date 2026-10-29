package com.example.demo.service;

import com.example.demo.dto.DepositDto.*;
import com.example.demo.entity.*;
import com.example.demo.entity.DepositEntity.DeductionReason;
import com.example.demo.entity.DepositEntity.DepositStatus;
import com.example.demo.entity.DepositEntity.DepositTxnType;
import com.example.demo.repository.DepositLedgerRepository;
import com.example.demo.repository.DepositRepository;
import com.example.demo.util.LocalINDTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Deposit lifecycle. The deposit is paid together with the plan price in ONE Airpay payment; this service
 * splits that payment into plan_amount + deposit_paid and keeps a ledger of every deduction / refund.
 *
 * RULES (real money):
 *   - A deposit row is created only when a subscription is activated (auto IPN or admin confirm).
 *   - balance = deposit_paid - deducted - refunded, never below 0. Every change runs under a row lock.
 *   - Ledger rows are append-only. Corrections are made with a new row, never by editing an old one.
 *   - A refund here does NOT move money. Do it on the Airpay dashboard first, then record it.
 */
@Service
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);
    private static final String MODULE = "SUBSCRIPTION_DEPOSIT_MANAGEMENT";

    private final DepositRepository repo;
    private final DepositLedgerRepository ledgerRepo;
    private final TransactionService transactionService;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)
    private final ObjectMapper mapper = new ObjectMapper();

    public DepositService(DepositRepository repo, DepositLedgerRepository ledgerRepo,
                          TransactionService transactionService, SuperAdminService superAdminService) {
        this.repo = repo;
        this.ledgerRepo = ledgerRepo;
        this.transactionService = transactionService;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  PAYMENT-DRIVEN (called by SubscriptionPurchaseService when a plan becomes ACTIVE)
    // ═══════════════════════════════════════════

    /** Idempotent. Runs inside the caller's transaction so "plan active" and "deposit recorded" succeed or fail together. */
    @Transactional
    public void recordReceived(UserSubscriptionEntity p, BigDecimal totalPaid) {
        BigDecimal dep = p.getDepositAmount();
        if (dep == null || dep.signum() <= 0) return;                       // plan had no deposit
        if (repo.findByOrderId(p.getOrderId()).isPresent()) return;         // duplicate IPN / confirm

        LocalINDTimeHolder t = new LocalINDTimeHolder();
        DepositEntity d = new DepositEntity();
        d.setPurchaseId(p.getId());
        d.setOrderId(p.getOrderId());
        d.setUsername(p.getUsername());
        d.setPlanName(p.getPlanName());
        d.setPlanAmount(money(p.getPrice()));
        d.setDepositPaid(money(dep));
        d.setTotalPaid(money(totalPaid != null ? totalPaid : p.getPrice().add(dep)));
        d.setStatus(DepositStatus.HELD);
        d.setCreatedAt(t.now);
        d.setUpdatedAt(t.now);
        d = repo.save(d);

        addLedger(d, DepositTxnType.DEPOSIT_RECEIVED, d.getDepositPaid(), null,
                "Deposit received with subscription payment", null, null, "SYSTEM", null, "SYSTEM", null);
        sysAudit(d.getOrderId(), "[DEPOSIT] Recorded " + d.getDepositPaid() + " (plan " + d.getPlanAmount()
                + ", total paid " + d.getTotalPaid() + ")", json(Map.of("after", state(d))));
        log.info("orderId={} deposit {} recorded", d.getOrderId(), d.getDepositPaid());
    }

    // ═══════════════════════════════════════════
    //  ADMIN ACTIONS
    // ═══════════════════════════════════════════

    @Transactional
    public DepositResponse deduct(String orderId, DeductRequest req,
                                  UUID actorId, String actorName, String actorRole, String ip) {
        DepositEntity d = lock(orderId);

        String key = blankToNull(req.getIdempotencyKey());
        if (key != null && ledgerRepo.findByDepositIdAndIdempotencyKey(d.getId(), key).isPresent()) {
            return toResponse(d);   // same request sent twice -> no second deduction
        }

        DeductionReason code = parseReason(req.getReasonCode());
        BigDecimal amt = money(req.getAmount());
        if (amt.signum() <= 0) throw new SuperAdminService.BadRequestException("Amount must be greater than 0.");
        if (amt.compareTo(d.balance()) > 0) {
            throw new SuperAdminService.BadRequestException(
                    "Deduction " + amt + " is more than the remaining deposit balance " + d.balance());
        }

        Map<String, Object> before = state(d);
        d.setDeductedTotal(d.getDeductedTotal().add(amt));
        refresh(d);
        d = repo.save(d);

        addLedger(d, DepositTxnType.DEDUCTION, amt, code, req.getReason(), blankToNull(req.getReference()),
                key, actorName, actorId, actorRole, ip);
        adminAudit("DEPOSIT_DEDUCT", d, before, state(d), code + ": " + req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(d);
    }

    @Transactional
    public DepositResponse refund(String orderId, DepositRefundRequest req,
                                  UUID actorId, String actorName, String actorRole, String ip) {
        DepositEntity d = lock(orderId);

        String key = blankToNull(req.getIdempotencyKey());
        if (key != null && ledgerRepo.findByDepositIdAndIdempotencyKey(d.getId(), key).isPresent()) {
            return toResponse(d);
        }

        BigDecimal amt = req.getAmount() == null ? d.balance() : money(req.getAmount());
        if (amt.signum() <= 0) throw new SuperAdminService.BadRequestException("Nothing left to refund (balance " + d.balance() + ").");
        if (amt.compareTo(d.balance()) > 0) {
            throw new SuperAdminService.BadRequestException(
                    "Refund " + amt + " is more than the remaining deposit balance " + d.balance());
        }

        Map<String, Object> before = state(d);
        d.setRefundedTotal(d.getRefundedTotal().add(amt));
        refresh(d);
        d = repo.save(d);

        addLedger(d, DepositTxnType.REFUND, amt, null, req.getReason(), blankToNull(req.getRefundReference()),
                key, actorName, actorId, actorRole, ip);
        adminAudit("DEPOSIT_REFUND", d, before, state(d), req.getReason(), actorId, actorName, actorRole, ip);
        return toResponse(d);
    }

    // ═══════════════════════════════════════════
    //  ADMIN READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResult<DepositResponse> list(String status, String username, String orderId,
                                            boolean onlyWithBalance, LocalDate from, LocalDate to, int page, int size) {
        DepositStatus st = parseStatus(status);
        Specification<DepositEntity> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (st != null) p.add(cb.equal(root.get("status"), st));
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            if (orderId != null && !orderId.isBlank()) p.add(cb.equal(root.get("orderId"), orderId.trim()));
            if (onlyWithBalance) p.add(cb.greaterThan(
                    cb.diff(cb.diff(root.<BigDecimal>get("depositPaid"), root.<BigDecimal>get("deductedTotal")),
                            root.<BigDecimal>get("refundedTotal")), BigDecimal.ZERO));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Page<DepositEntity> r = repo.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        // list rows come without ledger (keeps the payload small); use detail for the full history
        return new PageResult<>(r.getContent().stream().map(this::toSummary).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DepositResponse getDetail(String orderId) {
        return toResponse(find(orderId));
    }

    @Transactional(readOnly = true)
    public DepositStatsResponse stats() {
        Object[] r = repo.totals().get(0);
        BigDecimal received = (BigDecimal) r[1], ded = (BigDecimal) r[2], ref = (BigDecimal) r[3];
        return new DepositStatsResponse(((Number) r[0]).longValue(), received, ded, ref,
                received.subtract(ded).subtract(ref));
    }

    // ═══════════════════════════════════════════
    //  USER READ (always scoped to the caller)
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<UserDepositResponse> myDeposits(String username) {
        return repo.findByUsernameOrderByCreatedAtDesc(username).stream().map(this::toUserResponse).toList();
    }

    /** 404 for other people's orders (no info leak). */
    @Transactional(readOnly = true)
    public UserDepositResponse myDeposit(String username, String orderId) {
        return repo.findByOrderId(orderId)
                .filter(d -> d.getUsername().equals(username))
                .map(this::toUserResponse)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Deposit not found: " + orderId));
    }

    // ═══════════════════════════════════════════
    //  MAPPERS / HELPERS
    // ═══════════════════════════════════════════

    private DepositResponse toResponse(DepositEntity d) {
        List<LedgerRow> rows = ledgerRepo.findByDepositIdOrderByCreatedAtAsc(d.getId()).stream()
                .map(l -> new LedgerRow(l.getId(), l.getType().name(), l.getAmount(), l.getBalanceAfter(),
                        l.getReasonCode() == null ? null : l.getReasonCode().name(), l.getReason(), l.getReference(),
                        l.getIdempotencyKey(), l.getActorId(), l.getActorName(), l.getActorRole(), l.getIp(),
                        l.getCreatedAt()))
                .toList();
        return build(d, rows);
    }

    private DepositResponse toSummary(DepositEntity d) { return build(d, List.of()); }

    private DepositResponse build(DepositEntity d, List<LedgerRow> rows) {
        return new DepositResponse(d.getId(), d.getPurchaseId(), d.getOrderId(), d.getUsername(), d.getPlanName(),
                d.getPlanAmount(), d.getDepositPaid(), d.getTotalPaid(), d.getDeductedTotal(), d.getRefundedTotal(),
                d.balance(), d.getStatus().name(), d.getCreatedAt(), d.getUpdatedAt(), rows);
    }

    private UserDepositResponse toUserResponse(DepositEntity d) {
        List<UserLedgerRow> rows = ledgerRepo.findByDepositIdOrderByCreatedAtAsc(d.getId()).stream()
                .map(l -> new UserLedgerRow(l.getType().name(), l.getAmount(), l.getBalanceAfter(),
                        l.getReasonCode() == null ? null : l.getReasonCode().name(), l.getReason(),
                        l.getReference(), l.getCreatedAt()))
                .toList();
        return new UserDepositResponse(d.getOrderId(), d.getPlanName(), d.getPlanAmount(), d.getDepositPaid(),
                d.getTotalPaid(), d.getDeductedTotal(), d.getRefundedTotal(), d.balance(), d.getStatus().name(),
                d.getCreatedAt(), rows);
    }

    private void refresh(DepositEntity d) {
        d.setUpdatedAt(LocalINDTime.now());
        if (d.balance().signum() == 0) d.setStatus(DepositStatus.SETTLED);
        else if (d.getDeductedTotal().signum() > 0 || d.getRefundedTotal().signum() > 0) d.setStatus(DepositStatus.PARTIALLY_USED);
        else d.setStatus(DepositStatus.HELD);
    }

    private void addLedger(DepositEntity d, DepositTxnType type, BigDecimal amount, DeductionReason code,
                           String reason, String reference, String key,
                           String actorName, UUID actorId, String actorRole, String ip) {
        DepositLedgerEntity l = new DepositLedgerEntity();
        l.setDepositId(d.getId());
        l.setOrderId(d.getOrderId());
        l.setUsername(d.getUsername());
        l.setType(type);
        l.setAmount(amount);
        l.setBalanceAfter(d.balance());
        l.setReasonCode(code);
        l.setReason(truncate(reason, 500));
        l.setReference(truncate(reference, 100));
        l.setIdempotencyKey(key);
        l.setActorName(truncate(actorName, 200));
        l.setActorId(actorId);
        l.setActorRole(actorRole);
        l.setIp(ip);
        l.setCreatedAt(LocalINDTime.now());
        ledgerRepo.save(l);
    }

    private void adminAudit(String action, DepositEntity d, Map<String, Object> before, Map<String, Object> after,
                            String reason, UUID actorId, String actorName, String actorRole, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action, MODULE, d.getOrderId(), before, after, ip);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("reason", reason);
        payload.put("actor", actorName);
        payload.put("role", actorRole);
        payload.put("ip", ip);
        payload.put("before", before);
        payload.put("after", after);
        Transaction txn = transactionService.findByOrderId(d.getOrderId()).orElse(null);
        transactionService.logAudit(d.getOrderId(), txn, AuditEventType.ADMIN_ACTION,
                actorName + " (" + actorRole + ")", json(payload), "[DEPOSIT] " + action + ": " + reason);
    }

    private void sysAudit(String orderId, String remarks, String payload) {
        Transaction txn = transactionService.findByOrderId(orderId).orElse(null);
        transactionService.logAudit(orderId, txn, AuditEventType.SUBSCRIPTION_EVENT, "SYSTEM", payload, remarks);
    }

    private Map<String, Object> state(DepositEntity d) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("orderId", d.getOrderId());
        s.put("depositPaid", d.getDepositPaid());
        s.put("deducted", d.getDeductedTotal());
        s.put("refunded", d.getRefundedTotal());
        s.put("balance", d.balance());
        s.put("status", d.getStatus() == null ? null : d.getStatus().name());
        return s;
    }

    private DepositEntity find(String orderId) {
        return repo.findByOrderId(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Deposit not found: " + orderId));
    }

    private DepositEntity lock(String orderId) {
        return repo.findByOrderIdForUpdate(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Deposit not found: " + orderId));
    }

    private DeductionReason parseReason(String s) {
        try { return DeductionReason.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) {
            throw new SuperAdminService.BadRequestException("Invalid reasonCode. Must be one of: " + Arrays.toString(DeductionReason.values()));
        }
    }

    private DepositStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return DepositStatus.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new SuperAdminService.BadRequestException("Invalid status. Must be one of: " + Arrays.toString(DepositStatus.values()));
        }
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); } catch (Exception e) { return "{\"error\":\"could not serialize audit payload\"}"; }
    }

    private static BigDecimal money(BigDecimal v) { return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String truncate(String s, int max) { return s == null || s.length() <= max ? s : s.substring(0, max); }

    /** tiny helper so recordReceived uses one consistent timestamp */
    private static final class LocalINDTimeHolder { final java.time.LocalDateTime now = LocalINDTime.now(); }
}
