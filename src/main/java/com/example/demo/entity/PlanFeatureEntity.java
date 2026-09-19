package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_features")
public class PlanFeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlanEntity plan;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(name = "feature_description", columnDefinition = "text")
    private String featureDescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SubscriptionPlanEntity getPlan() { return plan; }
    public void setPlan(SubscriptionPlanEntity v) { this.plan = v; }
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String v) { this.featureName = v; }
    public String getFeatureDescription() { return featureDescription; }
    public void setFeatureDescription(String v) { this.featureDescription = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
