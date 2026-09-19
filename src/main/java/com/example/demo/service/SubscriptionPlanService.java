package com.example.demo.service;

import com.example.demo.dto.SubscriptionPlanDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-side Subscription Plan management (Basic / Standard / Prime style
 * rental tiers). Callable by:
 *   - Super Admin: unrestricted (see SuperAdminSubscriptionPlanController)
 *   - Sub-Admin: gated by PermissionCodes.PLAN_* via AuthorizationService,
 *     re-checked live against role_permissions on every call
 *     (see AdminSubscriptionPlanController)
 *
 * Both controllers delegate here; this service is actor-agnostic and just
 * needs actorId/actorName/actorRole/ip for the audit trail.
 */
@Service
public class SubscriptionPlanService {

    // Recognized status / duration-unit / reset-period values.
    private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE", "ARCHIVED");
    private static final Set<String> DURATION_UNITS = Set.of("DAY", "MONTH", "YEAR");
    private static final Set<String> RESET_PERIODS = Set.of("DAILY", "MONTHLY", "NONE");

    private final SubscriptionPlanRepository planRepository;
    private final PlanFeatureRepository featureRepository;
    private final PlanLimitRepository limitRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)

    public SubscriptionPlanService(
            SubscriptionPlanRepository planRepository,
            PlanFeatureRepository featureRepository,
            PlanLimitRepository limitRepository,
            SuperAdminService superAdminService) {
        this.planRepository = planRepository;
        this.featureRepository = featureRepository;
        this.limitRepository = limitRepository;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════

    @Transactional
    public SubscriptionPlanDto.PlanResponse create(
            SubscriptionPlanDto.CreatePlanRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        if (planRepository.findByPlanNameIgnoreCaseAndDeletedAtIsNull(request.getPlanName()).isPresent()) {
            throw new SuperAdminService.BadRequestException("Plan already exists: " + request.getPlanName());
        }
        validateDurationUnit(request.getDurationUnit());

        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setPlanName(request.getPlanName().trim());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setCurrency(request.getCurrency() == null || request.getCurrency().isBlank()
                ? "INR" : request.getCurrency().trim().toUpperCase());
        plan.setDurationValue(request.getDurationValue());
        plan.setDurationUnit(request.getDurationUnit().trim().toUpperCase());
        plan.setRecommended(request.isRecommended());
        plan.setStatus("DRAFT");
        plan.setActive(true);
        plan.setCreatedBy(actorId);

        plan = planRepository.save(plan);
        replaceFeatures(plan, request.getFeatures());
        replaceLimits(plan, request.getLimits());

        writeAudit(actorId, actorName, actorRole, "CREATE", plan.getPlanName(), null, planState(plan), ip);
        return toResponse(plan);
    }

    // ═══════════════════════════════════════════
    //  READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SubscriptionPlanDto.PlanListItemResponse> list(String status, Boolean active) {
        Specification<SubscriptionPlanEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (status != null && !status.isBlank()) p.add(cb.equal(root.get("status"), status.toUpperCase()));
            if (active != null) p.add(cb.equal(root.get("active"), active));
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return planRepository.findAll(spec).stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanDto.PlanResponse get(UUID id) {
        return toResponse(findPlan(id));
    }

    // ═══════════════════════════════════════════
    //  UPDATE (core fields)
    // ═══════════════════════════════════════════

    @Transactional
    public SubscriptionPlanDto.PlanResponse update(
            UUID id, SubscriptionPlanDto.UpdatePlanRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        SubscriptionPlanEntity plan = findPlan(id);
        validateDurationUnit(request.getDurationUnit());

        planRepository.findByPlanNameIgnoreCaseAndDeletedAtIsNull(request.getPlanName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new SuperAdminService.BadRequestException("Plan already exists: " + request.getPlanName());
                });

        Map<String, Object> before = planState(plan);

        plan.setPlanName(request.getPlanName().trim());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setCurrency(request.getCurrency().trim().toUpperCase());
        plan.setDurationValue(request.getDurationValue());
        plan.setDurationUnit(request.getDurationUnit().trim().toUpperCase());
        plan.setRecommended(request.isRecommended());
        plan.setUpdatedBy(actorId);

        plan = planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "UPDATE", plan.getPlanName(), before, planState(plan), ip);
        return toResponse(plan);
    }

    /** Full replace of a plan's feature list. */
    @Transactional
    public SubscriptionPlanDto.PlanResponse updateFeatures(
            UUID id, SubscriptionPlanDto.UpdateFeaturesRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        SubscriptionPlanEntity plan = findPlan(id);
        Map<String, Object> before = planState(plan);

        replaceFeatures(plan, request.getFeatures());
        plan.setUpdatedBy(actorId);
        plan = planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "UPDATE_FEATURES", plan.getPlanName(), before, planState(plan), ip);
        return toResponse(plan);
    }

    /** Full replace of a plan's limit list (MAX_ITEMS_PER_MONTH, MAX_SWAPS_PER_MONTH, etc). */
    @Transactional
    public SubscriptionPlanDto.PlanResponse updateLimits(
            UUID id, SubscriptionPlanDto.UpdateLimitsRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        SubscriptionPlanEntity plan = findPlan(id);
        Map<String, Object> before = planState(plan);

        replaceLimits(plan, request.getLimits());
        plan.setUpdatedBy(actorId);
        plan = planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "UPDATE_LIMITS", plan.getPlanName(), before, planState(plan), ip);
        return toResponse(plan);
    }

    /** Lifecycle status: DRAFT -> ACTIVE -> INACTIVE / ARCHIVED. */
    @Transactional
    public SubscriptionPlanDto.PlanResponse changeStatus(
            UUID id, String status,
            UUID actorId, String actorName, String actorRole, String ip) {

        if (status == null || !STATUSES.contains(status.toUpperCase())) {
            throw new SuperAdminService.BadRequestException(
                    "Invalid status. Must be one of: " + STATUSES);
        }

        SubscriptionPlanEntity plan = findPlan(id);
        String before = plan.getStatus();

        plan.setStatus(status.toUpperCase());
        plan.setUpdatedBy(actorId);
        plan = planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "STATUS_CHANGE", plan.getPlanName(),
                Map.of("status", before), Map.of("status", plan.getStatus()), ip);
        return toResponse(plan);
    }

    /** Soft active/inactive toggle (separate from lifecycle status — controls visibility to end users). */
    @Transactional
    public SubscriptionPlanDto.PlanResponse setActive(
            UUID id, boolean active,
            UUID actorId, String actorName, String actorRole, String ip) {

        SubscriptionPlanEntity plan = findPlan(id);
        boolean before = plan.isActive();

        plan.setActive(active);
        plan.setUpdatedBy(actorId);
        plan = planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "ACTIVE_TOGGLE", plan.getPlanName(),
                Map.of("active", before), Map.of("active", active), ip);
        return toResponse(plan);
    }

    /** Soft delete. Recommend enforcing at the API layer that no live subscriber sits on this plan before allowing this. */
    @Transactional
    public void delete(UUID id, UUID actorId, String actorName, String actorRole, String ip) {

        SubscriptionPlanEntity plan = findPlan(id);
        Map<String, Object> before = planState(plan);

        plan.setActive(false);
        plan.setStatus("ARCHIVED");
        plan.setDeletedAt(LocalDateTime.now());
        plan.setUpdatedBy(actorId);
        planRepository.save(plan);

        writeAudit(actorId, actorName, actorRole, "DELETE", plan.getPlanName(), before,
                Map.of("active", false, "status", "ARCHIVED", "deleted", true), ip);
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    private void replaceFeatures(SubscriptionPlanEntity plan, List<SubscriptionPlanDto.FeatureItem> items) {
        featureRepository.deleteByPlan_Id(plan.getId());
        plan.getFeatures().clear();
        if (items == null) return;
        for (SubscriptionPlanDto.FeatureItem item : items) {
            PlanFeatureEntity feature = new PlanFeatureEntity();
            feature.setPlan(plan);
            feature.setFeatureName(item.getFeatureName().trim());
            feature.setFeatureDescription(item.getFeatureDescription());
            featureRepository.save(feature);
        }
    }

    private void replaceLimits(SubscriptionPlanEntity plan, List<SubscriptionPlanDto.LimitItem> items) {
        limitRepository.deleteByPlan_Id(plan.getId());
        plan.getLimits().clear();
        if (items == null) return;
        Set<String> seenKeys = new HashSet<>();
        for (SubscriptionPlanDto.LimitItem item : items) {
            String key = item.getLimitKey().trim().toUpperCase();
            if (!seenKeys.add(key)) {
                throw new SuperAdminService.BadRequestException("Duplicate limit_key in request: " + key);
            }
            if (item.getResetPeriod() != null && !item.getResetPeriod().isBlank()
                    && !RESET_PERIODS.contains(item.getResetPeriod().toUpperCase())) {
                throw new SuperAdminService.BadRequestException(
                        "Invalid reset_period '" + item.getResetPeriod() + "'. Must be one of: " + RESET_PERIODS);
            }
            PlanLimitEntity limit = new PlanLimitEntity();
            limit.setPlan(plan);
            limit.setLimitKey(key);
            limit.setLimitLabel(item.getLimitLabel());
            limit.setLimitValue(item.getLimitValue());
            limit.setResetPeriod(item.getResetPeriod() == null ? "NONE" : item.getResetPeriod().toUpperCase());
            limitRepository.save(limit);
        }
    }

    private void validateDurationUnit(String unit) {
        if (unit == null || !DURATION_UNITS.contains(unit.trim().toUpperCase())) {
            throw new SuperAdminService.BadRequestException(
                    "Invalid duration_unit. Must be one of: " + DURATION_UNITS);
        }
    }

    private SubscriptionPlanEntity findPlan(UUID id) {
        return planRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Subscription plan not found: " + id));
    }

    private void writeAudit(UUID actorId, String actorName, String actorRole, String action,
                             String targetLabel, Object before, Object after, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action,
                "SUBSCRIPTION_PLAN_MANAGEMENT", targetLabel, before, after, ip);
    }

    private Map<String, Object> planState(SubscriptionPlanEntity plan) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", plan.getId());
        state.put("planName", plan.getPlanName());
        state.put("price", plan.getPrice());
        state.put("currency", plan.getCurrency());
        state.put("durationValue", plan.getDurationValue());
        state.put("durationUnit", plan.getDurationUnit());
        state.put("isRecommended", plan.isRecommended());
        state.put("status", plan.getStatus());
        state.put("active", plan.isActive());
        state.put("featureCount", featureRepository.findByPlan_Id(plan.getId()).size());
        state.put("limitCount", limitRepository.findByPlan_Id(plan.getId()).size());
        return state;
    }

    private SubscriptionPlanDto.PlanResponse toResponse(SubscriptionPlanEntity plan) {
        SubscriptionPlanDto.PlanResponse r = new SubscriptionPlanDto.PlanResponse();
        r.setId(plan.getId());
        r.setCreatedBy(plan.getCreatedBy());
        r.setUpdatedBy(plan.getUpdatedBy());
        r.setPlanName(plan.getPlanName());
        r.setDescription(plan.getDescription());
        r.setPrice(plan.getPrice());
        r.setCurrency(plan.getCurrency());
        r.setDurationValue(plan.getDurationValue());
        r.setDurationUnit(plan.getDurationUnit());
        r.setRecommended(plan.isRecommended());
        r.setStatus(plan.getStatus());
        r.setActive(plan.isActive());
        r.setCreatedAt(plan.getCreatedAt());
        r.setUpdatedAt(plan.getUpdatedAt());
        r.setDeletedAt(plan.getDeletedAt());

        r.setFeatures(featureRepository.findByPlan_Id(plan.getId()).stream().map(f -> {
            SubscriptionPlanDto.FeatureResponse fr = new SubscriptionPlanDto.FeatureResponse();
            fr.setId(f.getId());
            fr.setFeatureName(f.getFeatureName());
            fr.setFeatureDescription(f.getFeatureDescription());
            return fr;
        }).collect(Collectors.toList()));

        r.setLimits(limitRepository.findByPlan_Id(plan.getId()).stream().map(l -> {
            SubscriptionPlanDto.LimitResponse lr = new SubscriptionPlanDto.LimitResponse();
            lr.setId(l.getId());
            lr.setLimitKey(l.getLimitKey());
            lr.setLimitLabel(l.getLimitLabel());
            lr.setLimitValue(l.getLimitValue());
            lr.setResetPeriod(l.getResetPeriod());
            return lr;
        }).collect(Collectors.toList()));

        return r;
    }

    private SubscriptionPlanDto.PlanListItemResponse toListItem(SubscriptionPlanEntity plan) {
        SubscriptionPlanDto.PlanListItemResponse r = new SubscriptionPlanDto.PlanListItemResponse();
        r.setId(plan.getId());
        r.setPlanName(plan.getPlanName());
        r.setCurrency(plan.getCurrency());
        r.setDurationUnit(plan.getDurationUnit());
        r.setStatus(plan.getStatus());
        r.setPrice(plan.getPrice());
        r.setDurationValue(plan.getDurationValue());
        r.setRecommended(plan.isRecommended());
        r.setActive(plan.isActive());
        return r;
    }
}
