package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Append-only audit trail for every payment / purchase event. Never updated, never deleted. */
@Entity
@Table(name = "transaction_audit_logs", indexes = {
        @Index(name = "idx_tal_order_id", columnList = "order_id"),
        @Index(name = "idx_tal_created_at", columnList = "created_at")
})
public class TransactionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", length = 30)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private AuditEventType eventType;

    /** Who did it: "SYSTEM", "USER:<username>" or "<admin name> (<role>)". */
    @Column(name = "actor", length = 200)
    private String actor;

    /** JSON: before / after state, raw callback data, etc. */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TransactionAuditLog() {}

    @PrePersist
    protected void onCreate() { this.createdAt = LocalINDTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
