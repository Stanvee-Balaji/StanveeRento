package com.example.demo.controller;

import com.example.demo.dto.UserSubscriptionPlanDto;
import com.example.demo.service.UserSubscriptionPlanService;
import com.example.demo.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public / User-facing Subscription Plan API.
 *
 * All endpoints are read-only and serve only ACTIVE, non-deleted plans.
 * No authentication is required for browsing/comparing plans (adjust
 * your SecurityConfig if your app requires login first).
 *
 * Base URL: /api/v1/plans
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  GET  /api/v1/plans                        List available plans     │
 * │  GET  /api/v1/plans/{id}                   Single plan detail       │
 * │  GET  /api/v1/plans/recommended            Recommended plan(s)      │
 * │  GET  /api/v1/plans/compare?ids=…          Side-by-side comparison  │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/plans")
public class UserSubscriptionPlanController {

    private final UserSubscriptionPlanService planService;

    public UserSubscriptionPlanController(UserSubscriptionPlanService planService) {
        this.planService = planService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/v1/plans
    //  Optional query param: ?durationUnit=MONTH | YEAR | DAY
    //
    //  Returns all plans visible to users, sorted: recommended first, then
    //  ascending price. Use durationUnit to filter by billing cycle.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSubscriptionPlanDto.PlanSummaryResponse>>> listPlans(
            @RequestParam(required = false) String durationUnit) {

        List<UserSubscriptionPlanDto.PlanSummaryResponse> plans =
                planService.listPublicPlans(durationUnit);

        return ResponseEntity.ok(ApiResponse.success("Plans fetched successfully", plans, 200));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/v1/plans/recommended
    //
    //  Returns plan(s) flagged as recommended. Typically one plan, but the API
    //  is a list to be future-proof (e.g. "Best Value" and "Most Popular").
    //
    //  NOTE: This path must be declared BEFORE /{id} so Spring doesn't try
    //        to treat the literal "recommended" as a UUID.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<UserSubscriptionPlanDto.PlanSummaryResponse>>> getRecommended() {

        List<UserSubscriptionPlanDto.PlanSummaryResponse> plans =
                planService.getRecommendedPlans();

        return ResponseEntity.ok(ApiResponse.success("Recommended plans fetched", plans, 200));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/v1/plans/compare?ids=uuid1&ids=uuid2&ids=uuid3
    //
    //  Accepts 2–4 plan UUIDs (repeated ?ids= params or comma-separated).
    //  Returns a structured comparison matrix with:
    //    • plans[]           — summary card per plan
    //    • featureMatrix[]   — one row per unique feature; availability per plan
    //    • limitMatrix[]     — one row per unique limit key; value per plan
    //
    //  Example: GET /api/v1/plans/compare?ids=<uuid1>&ids=<uuid2>
    //
    //  NOTE: Must also be declared BEFORE /{id} for the same reason as /recommended.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<UserSubscriptionPlanDto.PlanComparisonResponse>> comparePlans(
            @RequestParam List<UUID> ids) {

        UserSubscriptionPlanDto.PlanComparisonResponse comparison =
                planService.comparePlans(ids);

        return ResponseEntity.ok(ApiResponse.success("Plans compared successfully", comparison, 200));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/v1/plans/{id}
    //
    //  Full detail for a single plan: description, all features, all limits.
    //  Returns 404 if the plan does not exist OR is not publicly visible
    //  (draft / inactive / archived / soft-deleted).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSubscriptionPlanDto.PlanDetailResponse>> getPlan(
            @PathVariable UUID id) {

        UserSubscriptionPlanDto.PlanDetailResponse detail = planService.getPlanDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Plan fetched successfully", detail, 200));
    }
}
