package com.example.demo.service;

import com.example.demo.dto.UserSubscriptionPlanDto;
import com.example.demo.entity.PlanFeatureEntity;
import com.example.demo.entity.PlanLimitEntity;
import com.example.demo.entity.SubscriptionPlanEntity;
import com.example.demo.repository.PlanFeatureRepository;
import com.example.demo.repository.PlanLimitRepository;
import com.example.demo.repository.SubscriptionPlanRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only service for user-facing subscription plan queries.
 *
 * Rules enforced here (not in the controller) so no internal/draft/archived
 * plans can ever leak to end users:
 *   • status must be ACTIVE
 *   • active flag must be true
 *   • deletedAt must be null
 *
 * No actor/audit context needed — these are public read operations.
 */
@Service
public class UserSubscriptionPlanService {

    private static final Set<String> VALID_DURATION_UNITS = Set.of("DAY", "MONTH", "YEAR");

    private final SubscriptionPlanRepository planRepository;
    private final PlanFeatureRepository featureRepository;
    private final PlanLimitRepository limitRepository;

    public UserSubscriptionPlanService(
            SubscriptionPlanRepository planRepository,
            PlanFeatureRepository featureRepository,
            PlanLimitRepository limitRepository) {
        this.planRepository = planRepository;
        this.featureRepository = featureRepository;
        this.limitRepository = limitRepository;
    }

    // ═══════════════════════════════════════════
    //  LIST  –  GET /api/v1/plans
    // ═══════════════════════════════════════════

    /**
     * Returns all publicly visible plans, optionally filtered by duration unit.
     * Results are ordered: recommended first, then by ascending price.
     *
     * @param durationUnit optional filter: DAY | MONTH | YEAR (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<UserSubscriptionPlanDto.PlanSummaryResponse> listPublicPlans(String durationUnit) {

        if (durationUnit != null && !durationUnit.isBlank()
                && !VALID_DURATION_UNITS.contains(durationUnit.trim().toUpperCase())) {
            throw new SuperAdminService.BadRequestException(
                    "Invalid duration_unit filter. Must be one of: " + VALID_DURATION_UNITS);
        }

        Specification<SubscriptionPlanEntity> spec = publicPlansSpec(durationUnit);

        return planRepository.findAll(spec).stream()
                .sorted(Comparator
                        .comparing(SubscriptionPlanEntity::isRecommended).reversed()   // recommended first
                        .thenComparing(p -> p.getPrice().doubleValue()))               // then ascending price
                .map(this::toSummary)
                .toList();
    }

    // ═══════════════════════════════════════════
    //  DETAIL  –  GET /api/v1/plans/{id}
    // ═══════════════════════════════════════════

    /**
     * Full detail for a single plan visible to the public.
     *
     * @throws SuperAdminService.ResourceNotFoundException if plan not found or not publicly visible.
     */
    @Transactional(readOnly = true)
    public UserSubscriptionPlanDto.PlanDetailResponse getPlanDetail(UUID id) {
        SubscriptionPlanEntity plan = findPublicPlan(id);
        return toDetail(plan);
    }

    // ═══════════════════════════════════════════
    //  RECOMMENDED  –  GET /api/v1/plans/recommended
    // ═══════════════════════════════════════════

    /**
     * Returns plans flagged as recommended.
     * Most apps flag exactly one plan, but the API returns a list in case
     * multiple plans are marked recommended.
     */
    @Transactional(readOnly = true)
    public List<UserSubscriptionPlanDto.PlanSummaryResponse> getRecommendedPlans() {
        Specification<SubscriptionPlanEntity> spec = publicPlansSpec(null)
                .and((root, query, cb) -> cb.isTrue(root.get("isRecommended")));

        return planRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(p -> p.getPrice().doubleValue()))
                .map(this::toSummary)
                .toList();
    }

    // ═══════════════════════════════════════════
    //  COMPARE  –  GET /api/v1/plans/compare
    // ═══════════════════════════════════════════

    /**
     * Side-by-side comparison of 2–4 plans.
     *
     * Builds a feature-matrix (feature name → which plans have it) and a
     * limit-matrix (limit key → value per plan), making it trivial for a
     * frontend to render a comparison table.
     *
     * @param ids 2–4 plan UUIDs (duplicates are silently de-duplicated)
     * @throws SuperAdminService.BadRequestException if fewer than 2 or more than 4 unique ids are provided
     */
    @Transactional(readOnly = true)
    public UserSubscriptionPlanDto.PlanComparisonResponse comparePlans(List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new SuperAdminService.BadRequestException("Provide at least 2 plan ids to compare.");
        }

        // De-duplicate while preserving request order
        List<UUID> uniqueIds = ids.stream().distinct().toList();

        if (uniqueIds.size() < 2) {
            throw new SuperAdminService.BadRequestException("Provide at least 2 distinct plan ids to compare.");
        }
        if (uniqueIds.size() > 4) {
            throw new SuperAdminService.BadRequestException("You can compare at most 4 plans at a time.");
        }

        // Fetch each plan preserving requested order; validate each is publicly visible
        List<SubscriptionPlanEntity> plans = uniqueIds.stream()
                .map(this::findPublicPlan)
                .toList();

        // ── Feature matrix ────────────────────────────────────────────────────
        // Build: featureName → { planId → featureDescription }
        // Using LinkedHashMap to keep insertion order (first plan's feature order)
        LinkedHashMap<String, Map<UUID, String>> featureMap = new LinkedHashMap<>();

        for (SubscriptionPlanEntity plan : plans) {
            List<PlanFeatureEntity> features = featureRepository.findByPlan_Id(plan.getId());
            for (PlanFeatureEntity f : features) {
                featureMap
                        .computeIfAbsent(f.getFeatureName(), k -> new LinkedHashMap<>())
                        .put(plan.getId(), f.getFeatureDescription());
            }
        }

        List<UserSubscriptionPlanDto.FeatureMatrixRow> featureMatrix = featureMap.entrySet().stream()
                .map(entry -> {
                    UserSubscriptionPlanDto.FeatureMatrixRow row = new UserSubscriptionPlanDto.FeatureMatrixRow();
                    row.setFeatureName(entry.getKey());
                    // pick any non-null description for the row label
                    row.setFeatureDescription(
                            entry.getValue().values().stream().filter(Objects::nonNull).findFirst().orElse(null));
                    Map<String, Boolean> availability = new LinkedHashMap<>();
                    for (SubscriptionPlanEntity plan : plans) {
                        availability.put(plan.getId().toString(), entry.getValue().containsKey(plan.getId()));
                    }
                    row.setAvailability(availability);
                    return row;
                })
                .toList();

        // ── Limit matrix ──────────────────────────────────────────────────────
        // Build: limitKey → { planId → PlanLimitEntity }
        LinkedHashMap<String, Map<UUID, PlanLimitEntity>> limitMap = new LinkedHashMap<>();

        for (SubscriptionPlanEntity plan : plans) {
            List<PlanLimitEntity> limits = limitRepository.findByPlan_Id(plan.getId());
            for (PlanLimitEntity l : limits) {
                limitMap
                        .computeIfAbsent(l.getLimitKey(), k -> new LinkedHashMap<>())
                        .put(plan.getId(), l);
            }
        }

        List<UserSubscriptionPlanDto.LimitMatrixRow> limitMatrix = limitMap.entrySet().stream()
                .map(entry -> {
                    UserSubscriptionPlanDto.LimitMatrixRow row = new UserSubscriptionPlanDto.LimitMatrixRow();
                    row.setLimitKey(entry.getKey());

                    // Take label and resetPeriod from the first plan that defines this limit
                    PlanLimitEntity sample = entry.getValue().values().iterator().next();
                    row.setLimitLabel(sample.getLimitLabel());
                    row.setResetPeriod(sample.getResetPeriod());

                    Map<String, Integer> values = new LinkedHashMap<>();
                    for (SubscriptionPlanEntity plan : plans) {
                        PlanLimitEntity l = entry.getValue().get(plan.getId());
                        values.put(plan.getId().toString(), l != null ? l.getLimitValue() : null);
                    }
                    row.setValues(values);
                    return row;
                })
                .toList();

        // ── Assemble response ─────────────────────────────────────────────────
        UserSubscriptionPlanDto.PlanComparisonResponse response = new UserSubscriptionPlanDto.PlanComparisonResponse();
        response.setPlans(plans.stream().map(this::toSummary).toList());
        response.setFeatureMatrix(featureMatrix);
        response.setLimitMatrix(limitMatrix);
        return response;
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    /** Shared Specification: only ACTIVE, active=true, not deleted, optionally filtered by duration unit. */
    private Specification<SubscriptionPlanEntity> publicPlansSpec(String durationUnit) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));
            predicates.add(cb.isTrue(root.get("active")));
            if (durationUnit != null && !durationUnit.isBlank()) {
                predicates.add(cb.equal(root.get("durationUnit"), durationUnit.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Finds a plan only if it is publicly visible (ACTIVE, active=true, not deleted).
     * Throws 404 otherwise — intentionally does not distinguish "exists but hidden"
     * from "does not exist" to avoid information leakage.
     */
    private SubscriptionPlanEntity findPublicPlan(UUID id) {
        return planRepository.findAll(
                        publicPlansSpec(null).and(
                                (root, query, cb) -> cb.equal(root.get("id"), id)))
                .stream()
                .findFirst()
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Subscription plan not found: " + id));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private UserSubscriptionPlanDto.PlanSummaryResponse toSummary(SubscriptionPlanEntity plan) {
        UserSubscriptionPlanDto.PlanSummaryResponse r = new UserSubscriptionPlanDto.PlanSummaryResponse();
        r.setId(plan.getId());
        r.setPlanName(plan.getPlanName());
        r.setDescription(plan.getDescription());
        r.setPrice(plan.getPrice());
        r.setCurrency(plan.getCurrency());
        r.setDurationValue(plan.getDurationValue());
        r.setDurationUnit(plan.getDurationUnit());
        r.setRecommended(plan.isRecommended());
        r.setFeatureCount(featureRepository.findByPlan_Id(plan.getId()).size());
        r.setCreatedAt(plan.getCreatedAt());
        return r;
    }

    private UserSubscriptionPlanDto.PlanDetailResponse toDetail(SubscriptionPlanEntity plan) {
        UserSubscriptionPlanDto.PlanDetailResponse r = new UserSubscriptionPlanDto.PlanDetailResponse();
        r.setId(plan.getId());
        r.setPlanName(plan.getPlanName());
        r.setDescription(plan.getDescription());
        r.setPrice(plan.getPrice());
        r.setCurrency(plan.getCurrency());
        r.setDurationValue(plan.getDurationValue());
        r.setDurationUnit(plan.getDurationUnit());
        r.setRecommended(plan.isRecommended());
        r.setCreatedAt(plan.getCreatedAt());
        r.setUpdatedAt(plan.getUpdatedAt());

        r.setFeatures(featureRepository.findByPlan_Id(plan.getId()).stream().map(f -> {
            UserSubscriptionPlanDto.FeatureView fv = new UserSubscriptionPlanDto.FeatureView();
            fv.setId(f.getId());
            fv.setFeatureName(f.getFeatureName());
            fv.setFeatureDescription(f.getFeatureDescription());
            return fv;
        }).toList());

        r.setLimits(limitRepository.findByPlan_Id(plan.getId()).stream().map(l -> {
            UserSubscriptionPlanDto.LimitView lv = new UserSubscriptionPlanDto.LimitView();
            lv.setId(l.getId());
            lv.setLimitKey(l.getLimitKey());
            lv.setLimitLabel(l.getLimitLabel());
            lv.setLimitValue(l.getLimitValue());
            lv.setResetPeriod(l.getResetPeriod());
            return lv;
        }).toList());

        return r;
    }
}
