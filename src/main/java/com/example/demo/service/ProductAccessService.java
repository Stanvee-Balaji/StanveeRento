package com.example.demo.service;

import com.example.demo.dto.ProductPlanAccessDto;
import com.example.demo.entity.ProductEntity;
import com.example.demo.entity.ProductPlanAccessEntity;
import com.example.demo.entity.SubscriptionPlanEntity;
import com.example.demo.repository.ProductPlanAccessRepository;
import com.example.demo.repository.ProductRepository;
//import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.util.LocalINDTime;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plan-based product access.
 *   - product has NO rows in product_plan_access  -> open to all plans (unchanged behaviour)
 *   - product has rows                            -> only users with an ACTIVE, non-expired
 *                                                    subscription on one of those plans
 */
@Service
public class ProductAccessService {

    private final ProductPlanAccessRepository accessRepository;
    private final ProductRepository productRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)
    private final JdbcTemplate jdbc;

    public ProductAccessService(ProductPlanAccessRepository accessRepository,
                                ProductRepository productRepository,
                                SubscriptionPlanRepository planRepository,
                                SuperAdminService superAdminService,
                                JdbcTemplate jdbc) {
        this.accessRepository = accessRepository;
        this.productRepository = productRepository;
        this.planRepository = planRepository;
        this.superAdminService = superAdminService;
        this.jdbc = jdbc;
    }

    // ═══════════ READ ═══════════

    @Transactional(readOnly = true)
    public ProductPlanAccessDto.AccessResponse getAccessInfo(UUID productId) {
        List<UUID> ids = accessRepository.findPlanIdsByProductId(productId);
        ProductPlanAccessDto.AccessResponse r = new ProductPlanAccessDto.AccessResponse();
        r.setProductId(productId);
        r.setRestricted(!ids.isEmpty());
        r.setPlans(toPlanRefs(ids));
        return r;
    }

    /** Admin read: also verifies product exists. */
    @Transactional(readOnly = true)
    public ProductPlanAccessDto.AccessResponse getForAdmin(UUID productId) {
        findProduct(productId);
        return getAccessInfo(productId);
    }

    // ═══════════ ADMIN WRITE ═══════════

    @Transactional
    public ProductPlanAccessDto.AccessResponse replaceAllowedPlans(
            UUID productId, List<UUID> planIds,
            UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(productId);
        Map<String, Object> before = state(getAccessInfo(productId));

        replaceRows(productId, planIds);

        ProductPlanAccessDto.AccessResponse after = getAccessInfo(productId);
        superAdminService.writeAudit(actorId, actorName, actorRole, "UPDATE_PLAN_ACCESS",
                "PRODUCT_MANAGEMENT", product.getName(), before, state(after), ip);
        return after;
    }

    /** Used by create product too. No audit here (caller audits). */
    @Transactional
    public void replaceRows(UUID productId, List<UUID> planIds) {
        Set<UUID> unique = planIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(planIds);
        for (UUID pid : unique) {
            if (pid == null || planRepository.findByIdAndDeletedAtIsNull(pid).isEmpty()) {
                throw new SuperAdminService.BadRequestException("Plan not found: " + pid);
            }
        }
        accessRepository.deleteByProductId(productId);
        for (UUID pid : unique) {
            ProductPlanAccessEntity e = new ProductPlanAccessEntity();
            e.setProductId(productId);
            e.setPlanId(pid);
            accessRepository.save(e);
        }
    }

    @Transactional
    public void clearForProduct(UUID productId) {
        accessRepository.deleteByProductId(productId);
    }

    // ═══════════ USER CHECK ═══════════

    /** Throws PlanAccessDeniedException (HTTP 403) when the user's plan cannot rent this product. */
    @Transactional(readOnly = true)
    public void assertCanRent(String username, UUID productId) {
        assertCanRent(username, productId, null);
    }

    @Transactional(readOnly = true)
    public void assertCanRent(String username, UUID productId, LocalDate rentalEndDate) {
        List<UUID> allowed = accessRepository.findPlanIdsByProductId(productId);
        if (allowed.isEmpty()) return; // open to all

        Set<UUID> userPlans = activePlanIds(username, rentalEndDate);
        boolean ok = userPlans.stream().anyMatch(allowed::contains);
        if (ok) return;

        String names = toPlanRefs(allowed).stream()
                .map(ProductPlanAccessDto.PlanRef::getPlanName)
                .collect(Collectors.joining(", "));
        String msg = userPlans.isEmpty()
                ? "An active subscription is required for this item. Available for: " + names + "."
                : "This item is available only for: " + names + ". Please upgrade your plan.";
        throw new PlanAccessDeniedException(msg);
    }

    @Transactional(readOnly = true)
    public ProductPlanAccessDto.UserAccessResponse getUserAccess(
            String username, UUID productId, LocalDate rentalEndDate) {

        findProduct(productId);
        ProductPlanAccessDto.AccessResponse info = getAccessInfo(productId);

        ProductPlanAccessDto.UserAccessResponse r = new ProductPlanAccessDto.UserAccessResponse();
        r.setProductId(productId);
        r.setRestricted(info.isRestricted());
        r.setRequiredPlans(info.getPlans());
        try {
            assertCanRent(username, productId, rentalEndDate);
            r.setCanRent(true);
        } catch (PlanAccessDeniedException e) {
            r.setCanRent(false);
            r.setReason(e.getMessage());
        }
        return r;
    }

    // ═══════════ HELPERS ═══════════

    /** Plan ids of the user's ACTIVE subscriptions that are still valid now and on the rental end date. */
    private Set<UUID> activePlanIds(String username, LocalDate rentalEndDate) {
//        LocalDateTime cutoff = LocalDateTime.now();
        LocalDateTime cutoff = LocalINDTime.now();
        if (rentalEndDate != null && rentalEndDate.atStartOfDay().isAfter(cutoff)) {
            cutoff = rentalEndDate.atStartOfDay();
        }
        List<UUID> ids = jdbc.query(
                "SELECT plan_id FROM user_subscriptions "
                        + "WHERE username = ? AND status = 'ACTIVE' "
                        + "AND (end_date IS NULL OR end_date >= ?)",
                (rs, i) -> rs.getObject("plan_id", UUID.class),
                username, Timestamp.valueOf(cutoff));
        return new HashSet<>(ids);
    }

    private List<ProductPlanAccessDto.PlanRef> toPlanRefs(List<UUID> ids) {
        if (ids.isEmpty()) return new ArrayList<>();
        return planRepository.findAllById(ids).stream()
                .filter(p -> p.getDeletedAt() == null)
                .sorted(Comparator.comparing(p -> p.getPrice().doubleValue()))
                .map(p -> new ProductPlanAccessDto.PlanRef(p.getId(), p.getPlanName()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> state(ProductPlanAccessDto.AccessResponse a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("restricted", a.isRestricted());
        m.put("planIds", a.getPlans().stream().map(ProductPlanAccessDto.PlanRef::getId).toList());
        m.put("planNames", a.getPlans().stream().map(ProductPlanAccessDto.PlanRef::getPlanName).toList());
        return m;
    }

    private ProductEntity findProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + id));
    }

    // ═══════════ EXCEPTION ═══════════

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class PlanAccessDeniedException extends RuntimeException {
        public PlanAccessDeniedException(String message) { super(message); }
    }
}