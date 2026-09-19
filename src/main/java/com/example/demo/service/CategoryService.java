package com.example.demo.service;

import com.example.demo.dto.CategoryDto;
import com.example.demo.entity.CategoryEntity;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Category management — callable by:
 *   - Super Admin: unrestricted (see SuperAdminCategoryController)
 *   - Sub-Admin: gated by permission codes via AuthorizationService
 *     (see AdminCategoryController when you add sub-admin support)
 *
 * Actor-agnostic: actorId / actorName / actorRole / ip are only used
 * for the audit trail — same shape as ProductService / SubscriptionPlanService.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;       // guard: block delete if products exist
    private final SuperAdminService superAdminService;       // reused for writeAudit(...)

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            SuperAdminService superAdminService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════

    @Transactional
    public CategoryDto.CategoryResponse create(
            CategoryDto.CreateCategoryRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        String trimmed = request.getName().trim();
        categoryRepository.findByNameIgnoreCaseAndDeletedAtIsNull(trimmed).ifPresent(c -> {
            throw new SuperAdminService.BadRequestException("Category name already exists: " + trimmed);
        });

        CategoryEntity category = new CategoryEntity();
        category.setName(trimmed);
        category.setActive(true);
        category = categoryRepository.save(category);

        writeAudit(actorId, actorName, actorRole, "CREATE",
                category.getName(), null, categoryState(category), ip);
        return toResponse(category);
    }

    // ═══════════════════════════════════════════
    //  READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CategoryDto.CategoryListItemResponse> list(Boolean active) {
        Specification<CategoryEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (active != null) p.add(cb.equal(root.get("active"), active));
            query.orderBy(cb.asc(root.get("name")));
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return categoryRepository.findAll(spec).stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto.CategoryResponse get(UUID id) {
        return toResponse(findCategory(id));
    }

    // ═══════════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════════

    @Transactional
    public CategoryDto.CategoryResponse update(
            UUID id, CategoryDto.UpdateCategoryRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        CategoryEntity category = findCategory(id);
        String trimmed = request.getName().trim();

        // Duplicate name check — exclude self so the category can keep its own name
        categoryRepository.findByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(trimmed, id).ifPresent(c -> {
            throw new SuperAdminService.BadRequestException("Category name already exists: " + trimmed);
        });

        Map<String, Object> before = categoryState(category);
        category.setName(trimmed);
        category = categoryRepository.save(category);

        writeAudit(actorId, actorName, actorRole, "UPDATE",
                category.getName(), before, categoryState(category), ip);
        return toResponse(category);
    }

    // ═══════════════════════════════════════════
    //  ACTIVE TOGGLE
    // ═══════════════════════════════════════════

    /**
     * Deactivating a category does NOT automatically deactivate its products.
     * ProductService already blocks creating/updating a product under an inactive
     * category, so existing active products remain visible until explicitly
     * deactivated. Warn the caller at the API level if needed.
     */
    @Transactional
    public CategoryDto.CategoryResponse setActive(
            UUID id, boolean active,
            UUID actorId, String actorName, String actorRole, String ip) {

        CategoryEntity category = findCategory(id);
        boolean before = category.isActive();

        category.setActive(active);
        category = categoryRepository.save(category);

        writeAudit(actorId, actorName, actorRole, "ACTIVE_TOGGLE",
                category.getName(),
                Map.of("active", before),
                Map.of("active", active), ip);
        return toResponse(category);
    }

    // ═══════════════════════════════════════════
    //  DELETE (soft)
    // ═══════════════════════════════════════════

    /**
     * Soft delete. Blocked if any non-deleted products still reference this category —
     * callers must reassign or delete those products first.
     */
    @Transactional
    public void delete(UUID id, UUID actorId, String actorName, String actorRole, String ip) {

        CategoryEntity category = findCategory(id);

        long linkedProducts = productRepository.countByCategoryIdAndDeletedAtIsNull(id);
        if (linkedProducts > 0) {
            throw new SuperAdminService.BadRequestException(
                    "Cannot delete category '" + category.getName() +
                    "' — it still has " + linkedProducts + " active product(s). " +
                    "Reassign or delete those products first.");
        }

        Map<String, Object> before = categoryState(category);
        category.setActive(false);
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);

        writeAudit(actorId, actorName, actorRole, "DELETE",
                category.getName(), before,
                Map.of("active", false, "deleted", true), ip);
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    private CategoryEntity findCategory(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Category not found: " + id));
    }

    private void writeAudit(UUID actorId, String actorName, String actorRole, String action,
                             String targetLabel, Object before, Object after, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action,
                "CATEGORY_MANAGEMENT", targetLabel, before, after, ip);
    }

    private Map<String, Object> categoryState(CategoryEntity category) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", category.getId());
        state.put("name", category.getName());
        state.put("active", category.isActive());
        return state;
    }

    private CategoryDto.CategoryResponse toResponse(CategoryEntity c) {
        CategoryDto.CategoryResponse r = new CategoryDto.CategoryResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setActive(c.isActive());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        r.setDeletedAt(c.getDeletedAt());
        return r;
    }

    private CategoryDto.CategoryListItemResponse toListItem(CategoryEntity c) {
        CategoryDto.CategoryListItemResponse r = new CategoryDto.CategoryListItemResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setActive(c.isActive());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
