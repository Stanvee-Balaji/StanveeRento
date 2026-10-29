package com.example.demo.entity;

import com.example.demo.entity.DepositEntity.DeductionReason;
import com.example.demo.entity.DepositEntity.DepositTxnType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only history: every rupee that enters or leaves a deposit is one row here. Never updated, never deleted. */
@Entity
@Table(name = "subscription_deposit_ledger",
        uniqueConstraints = @UniqueConstraint(name = "uk_dep_ledger_idem", columnNames = {"deposit_id", "idempotency_key"}),
        indexes = {@Index(name = "idx_dep_ledger_deposit", columnList = "deposit_id"),
                   @Index(name = "idx_dep_ledger_order", columnList = "order_id")})
public class DepositLedgerEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "deposit_id", nullable = false) private UUID depositId;
    @Column(name = "order_id", nullable = false, length = 20) private String orderId;
    @Column(name = "username", nullable = false, length = 100) private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20) private DepositTxnType type;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2) private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 30) private DeductionReason reasonCode;
    @Column(name = "reason", length = 500) private String reason;
    /** Rental order id / product id / refund UTR etc. */
    @Column(name = "reference", length = 100) private String reference;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;

    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "actor_name", length = 200) private String actorName;
    @Column(name = "actor_role", length = 50) private String actorRole;
    @Column(name = "ip", length = 64) private String ip;

    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getDepositId() { return depositId; }
    public void setDepositId(UUID v) { depositId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public DepositTxnType getType() { return type; }
    public void setType(DepositTxnType v) { type = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal v) { balanceAfter = v; }
    public DeductionReason getReasonCode() { return reasonCode; }
    public void setReasonCode(DeductionReason v) { reasonCode = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { reference = v; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID v) { actorId = v; }
    public String getActorName() { return actorName; }
    public void setActorName(String v) { actorName = v; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String v) { actorRole = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { ip = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
