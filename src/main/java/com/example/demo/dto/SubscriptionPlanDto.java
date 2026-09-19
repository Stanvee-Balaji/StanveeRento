package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SubscriptionPlanDto {

    private SubscriptionPlanDto() {}

    // ═══════════════════════════════════════════
    //  NESTED: FEATURE / LIMIT
    // ═══════════════════════════════════════════

    public static class FeatureItem {
        @NotBlank
        private String featureName;
        private String featureDescription;

        public String getFeatureName() { return featureName; }
        public void setFeatureName(String v) { featureName = v; }
        public String getFeatureDescription() { return featureDescription; }
        public void setFeatureDescription(String v) { featureDescription = v; }
        
        
    }

    public static class LimitItem {
        @NotBlank
        private String limitKey;
        private String limitLabel;
        @NotNull
        private Integer limitValue; // -1 = unlimited
        private String resetPeriod; // DAILY / MONTHLY / NONE

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
    //  REQUESTS
    // ═══════════════════════════════════════════

    public static class CreatePlanRequest {
        @NotBlank
        private String planName;
        private String description;
        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal price;
        private String currency = "INR";
        @NotNull @Min(1)
        private Integer durationValue;
        @NotBlank
        private String durationUnit; // DAY / MONTH / YEAR
        private boolean isRecommended = false;
        @Valid
        private List<FeatureItem> features;
        @Valid
        private List<LimitItem> limits;

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
        public List<FeatureItem> getFeatures() { return features; }
        public void setFeatures(List<FeatureItem> v) { features = v; }
        public List<LimitItem> getLimits() { return limits; }
        public void setLimits(List<LimitItem> v) { limits = v; }
    }

    public static class UpdatePlanRequest {
        @NotBlank
        private String planName;
        private String description;
        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal price;
        @NotBlank
        private String currency;
        @NotNull @Min(1)
        private Integer durationValue;
        @NotBlank
        private String durationUnit;
        private boolean isRecommended;

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
    }

    public static class UpdateFeaturesRequest {
        @NotNull @Valid
        private List<FeatureItem> features;

        public List<FeatureItem> getFeatures() { return features; }
        public void setFeatures(List<FeatureItem> v) { features = v; }
    }

    public static class UpdateLimitsRequest {
        @NotNull @Valid
        private List<LimitItem> limits;

        public List<LimitItem> getLimits() { return limits; }
        public void setLimits(List<LimitItem> v) { limits = v; }
    }

    public static class PlanStatusRequest {
        @NotBlank
        private String status; // DRAFT / ACTIVE / INACTIVE / ARCHIVED

        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
    }

    public static class ActiveFlagRequest {
        @NotNull
        private Boolean active;

        public Boolean getActive() { return active; }
        public void setActive(Boolean v) { active = v; }
    }

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    public static class FeatureResponse {
        private UUID id;
        private String featureName, featureDescription;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getFeatureName() { return featureName; }
        public void setFeatureName(String v) { featureName = v; }
        public String getFeatureDescription() { return featureDescription; }
        public void setFeatureDescription(String v) { featureDescription = v; }
    }

    public static class LimitResponse {
        private UUID id;
        private String limitKey, limitLabel, resetPeriod;
        private Integer limitValue;

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

    public static class PlanResponse {
        private UUID id, createdBy, updatedBy;
        private String planName, description, currency, durationUnit, status;
        private BigDecimal price;
        private Integer durationValue;
        private boolean isRecommended, active;
        private List<FeatureResponse> features;
        private List<LimitResponse> limits;
        private LocalDateTime createdAt, updatedAt, deletedAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public UUID getCreatedBy() { return createdBy; }
        public void setCreatedBy(UUID v) { createdBy = v; }
        public UUID getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(UUID v) { updatedBy = v; }
        public String getPlanName() { return planName; }
        public void setPlanName(String v) { planName = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { currency = v; }
        public String getDurationUnit() { return durationUnit; }
        public void setDurationUnit(String v) { durationUnit = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal v) { price = v; }
        public Integer getDurationValue() { return durationValue; }
        public void setDurationValue(Integer v) { durationValue = v; }
        public boolean isRecommended() { return isRecommended; }
        public void setRecommended(boolean v) { isRecommended = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
        public List<FeatureResponse> getFeatures() { return features; }
        public void setFeatures(List<FeatureResponse> v) { features = v; }
        public List<LimitResponse> getLimits() { return limits; }
        public void setLimits(List<LimitResponse> v) { limits = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime v) { deletedAt = v; }
    }

    public static class PlanListItemResponse {
        private UUID id;
        private String planName, currency, durationUnit, status;
        private BigDecimal price;
        private Integer durationValue;
        private boolean isRecommended, active;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getPlanName() { return planName; }
        public void setPlanName(String v) { planName = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { currency = v; }
        public String getDurationUnit() { return durationUnit; }
        public void setDurationUnit(String v) { durationUnit = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal v) { price = v; }
        public Integer getDurationValue() { return durationValue; }
        public void setDurationValue(Integer v) { durationValue = v; }
        public boolean isRecommended() { return isRecommended; }
        public void setRecommended(boolean v) { isRecommended = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
    }
}
