package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductPlanAccessDto {

    private ProductPlanAccessDto() {}

    /** Full replace. Empty list = product open to all plans. */
    public static class UpdateAccessRequest {
        @NotNull
        private List<UUID> planIds;
        public List<UUID> getPlanIds() { return planIds; }
        public void setPlanIds(List<UUID> v) { planIds = v; }
    }

    public static class PlanRef {
        private UUID id;
        private String planName;
        public PlanRef() {}
        public PlanRef(UUID id, String planName) { this.id = id; this.planName = planName; }
        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getPlanName() { return planName; }
        public void setPlanName(String v) { planName = v; }
    }

    public static class AccessResponse {
        private UUID productId;
        private boolean restricted;
        private List<PlanRef> plans = new ArrayList<>();
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public boolean isRestricted() { return restricted; }
        public void setRestricted(boolean v) { restricted = v; }
        public List<PlanRef> getPlans() { return plans; }
        public void setPlans(List<PlanRef> v) { plans = v; }
    }

    public static class UserAccessResponse {
        private UUID productId;
        private boolean restricted;
        private boolean canRent;
        private String reason;
        private List<PlanRef> requiredPlans = new ArrayList<>();
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { productId = v; }
        public boolean isRestricted() { return restricted; }
        public void setRestricted(boolean v) { restricted = v; }
        public boolean isCanRent() { return canRent; }
        public void setCanRent(boolean v) { canRent = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { reason = v; }
        public List<PlanRef> getRequiredPlans() { return requiredPlans; }
        public void setRequiredPlans(List<PlanRef> v) { requiredPlans = v; }
    }
}