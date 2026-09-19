package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DTOs for the public / user-facing Subscription Plan APIs.
 *
 * Intentionally stripped of internal fields (createdBy, updatedBy, deletedAt,
 * audit metadata) – only data a subscriber needs to make a purchase decision is
 * exposed.
 */
public final class UserSubscriptionPlanDto {

    private UserSubscriptionPlanDto() {}

    // ═══════════════════════════════════════════
    //  NESTED: FEATURE / LIMIT (read-only)
    // ═══════════════════════════════════════════

    /** Single feature line shown on a plan card (e.g. "Unlimited Listings"). */
    public static class FeatureView {
        private UUID id;
        private String featureName;
        private String featureDescription;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getFeatureName() { return featureName; }
        public void setFeatureName(String v) { featureName = v; }
        public String getFeatureDescription() { return featureDescription; }
        public void setFeatureDescription(String v) { featureDescription = v; }
    }

    /** Single usage/quota limit row (e.g. MAX_ITEMS_PER_MONTH = 50, MONTHLY). */
    public static class LimitView {
        private UUID id;
        private String limitKey;
        private String limitLabel;
        private Integer limitValue;   // -1 = unlimited
        private String resetPeriod;   // DAILY / MONTHLY / NONE

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getLimitKey() { return limitKey; }
        public void setLimitKey(String v) { limitKey = v; }
        public String getLimitLabel() { return limitLabel; }
        public void setLimitLabel(String v) { limitLabel = v; }
        public Integer getLimitValue() { return limitValue; }
        public void setLimitValue(Integer v) { limitValue = v; }
        public String getResetPeriod() { return resetPeriod; }
        public void setResetPeriod(String v) { resetPeriod = v; }
    }

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    /**
     * Lightweight card used in listing and comparison grids.
     * Does NOT include the full feature/limit lists to keep the payload small;
     * use {@link PlanDetailResponse} for the full breakdown.
     */
    public static class PlanSummaryResponse {
        private UUID id;
        private String planName;
        private String description;
        private BigDecimal price;
        private String currency;
        private Integer durationValue;
        private String durationUnit;
        private boolean isRecommended;
        private int featureCount;
        private LocalDateTime createdAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getPlanName() { return planName; }
        public void setPlanName(String v) { planName = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal v) { price = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { currency = v; }
        public Integer getDurationValue() { return durationValue; }
        public void setDurationValue(Integer v) { durationValue = v; }
        public String getDurationUnit() { return durationUnit; }
        public void setDurationUnit(String v) { durationUnit = v; }
        public boolean isRecommended() { return isRecommended; }
        public void setRecommended(boolean v) { isRecommended = v; }
        public int getFeatureCount() { return featureCount; }
        public void setFeatureCount(int v) { featureCount = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }

    /**
     * Full plan detail including all features and limits.
     * Returned by the single-plan endpoint GET /api/v1/plans/{id}.
     */
    public static class PlanDetailResponse {
        private UUID id;
        private String planName;
        private String description;
        private BigDecimal price;
        private String currency;
        private Integer durationValue;
        private String durationUnit;
        private boolean isRecommended;
        private List<FeatureView> features;
        private List<LimitView> limits;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getPlanName() { return planName; }
        public void setPlanName(String v) { planName = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal v) { price = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { currency = v; }
        public Integer getDurationValue() { return durationValue; }
        public void setDurationValue(Integer v) { durationValue = v; }
        public String getDurationUnit() { return durationUnit; }
        public void setDurationUnit(String v) { durationUnit = v; }
        public boolean isRecommended() { return isRecommended; }
        public void setRecommended(boolean v) { isRecommended = v; }
        public List<FeatureView> getFeatures() { return features; }
        public void setFeatures(List<FeatureView> v) { features = v; }
        public List<LimitView> getLimits() { return limits; }
        public void setLimits(List<LimitView> v) { limits = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    }

    // ═══════════════════════════════════════════
    //  COMPARISON RESPONSE
    // ═══════════════════════════════════════════

    /**
     * Structured payload for side-by-side plan comparison.
     *
     * Frontend can render a table directly:
     *  - Column headers  →  plans[].planName  (+ price, duration)
     *  - Feature rows    →  featureMatrix[].featureName, .availability{planId → boolean}
     *  - Limit rows      →  limitMatrix[].limitLabel,  .values{planId → value}, .resetPeriod
     */
    public static class PlanComparisonResponse {

        /** Ordered list of the plans being compared (same order as requested ids). */
        private List<PlanSummaryResponse> plans;

        /**
         * One row per unique feature name across ALL compared plans.
         * availability maps planId → true/false (false = plan doesn't have this feature).
         */
        private List<FeatureMatrixRow> featureMatrix;

        /**
         * One row per unique limit key across ALL compared plans.
         * values maps planId → limitValue  (-1 = unlimited, null = not applicable).
         */
        private List<LimitMatrixRow> limitMatrix;

        public List<PlanSummaryResponse> getPlans() { return plans; }
        public void setPlans(List<PlanSummaryResponse> v) { plans = v; }
        public List<FeatureMatrixRow> getFeatureMatrix() { return featureMatrix; }
        public void setFeatureMatrix(List<FeatureMatrixRow> v) { featureMatrix = v; }
        public List<LimitMatrixRow> getLimitMatrix() { return limitMatrix; }
        public void setLimitMatrix(List<LimitMatrixRow> v) { limitMatrix = v; }
    }

    /** A single feature row in the comparison matrix. */
    public static class FeatureMatrixRow {
        private String featureName;
        private String featureDescription;
        /** planId (as string) → true if the plan includes this feature */
        private Map<String, Boolean> availability;

        public String getFeatureName() { return featureName; }
        public void setFeatureName(String v) { featureName = v; }
        public String getFeatureDescription() { return featureDescription; }
        public void setFeatureDescription(String v) { featureDescription = v; }
        public Map<String, Boolean> getAvailability() { return availability; }
        public void setAvailability(Map<String, Boolean> v) { availability = v; }
    }

    /** A single limit/quota row in the comparison matrix. */
    public static class LimitMatrixRow {
        private String limitKey;
        private String limitLabel;
        private String resetPeriod;   // DAILY / MONTHLY / NONE (taken from first plan that has it)
        /** planId (as string) → limitValue  (null = plan has no such limit defined) */
        private Map<String, Integer> values;

        public String getLimitKey() { return limitKey; }
        public void setLimitKey(String v) { limitKey = v; }
        public String getLimitLabel() { return limitLabel; }
        public void setLimitLabel(String v) { limitLabel = v; }
        public String getResetPeriod() { return resetPeriod; }
        public void setResetPeriod(String v) { resetPeriod = v; }
        public Map<String, Integer> getValues() { return values; }
        public void setValues(Map<String, Integer> v) { values = v; }
    }
}
