package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_limits", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "limit_key"}))
public class PlanLimitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlanEntity plan;

    @Column(name = "limit_key", nullable = false)
    private String limitKey;

    @Column(name = "limit_label")
    private String limitLabel;

    @Column(name = "limit_value", nullable = false)
    private Integer limitValue; // -1 = unlimited

    @Column(name = "reset_period")
    private String resetPeriod; // DAILY / MONTHLY / NONE

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SubscriptionPlanEntity getPlan() { return plan; }
    public void setPlan(SubscriptionPlanEntity v) { this.plan = v; }
    public String getLimitKey() { return limitKey; }
    public void setLimitKey(String v) { this.limitKey = v; }
    public String getLimitLabel() { return limitLabel; }
    public void setLimitLabel(String v) { this.limitLabel = v; }
    public Integer getLimitValue() { return limitValue; }
    public void setLimitValue(Integer v) { this.limitValue = v; }
    public String getResetPeriod() { return resetPeriod; }
    public void setResetPeriod(String v) { this.resetPeriod = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
