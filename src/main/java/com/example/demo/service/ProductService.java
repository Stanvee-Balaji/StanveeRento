package com.example.demo.service;

import com.example.demo.dto.ProductDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-side Product management (rental catalog items, e.g. outfits).
 * Callable by:
 *   - Super Admin: unrestricted (see SuperAdminProductController)
 *   - Sub-Admin: gated by ProductPermissionCodes.PRODUCT_* via
 *     AuthorizationService, re-checked live against role_permissions on
 *     every call (see AdminProductController)
 *
 * Both controllers delegate here; this service is actor-agnostic and just
 * needs actorId/actorName/actorRole/ip for the audit trail — same shape as
 * SubscriptionPlanService.
 */
@Service
public class ProductService {

    private static final Set<String> CONDITIONS = Set.of("NEW", "GOOD", "FAIR", "POOR");
    private static final Set<String> INVENTORY_STATUSES = Set.of("AVAILABLE", "RENTED", "CLEANING", "DAMAGED");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository imageRepository,
            InventoryRepository inventoryRepository,
            SuperAdminService superAdminService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
        this.inventoryRepository = inventoryRepository;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════

    @Transactional
    public ProductDto.ProductResponse create(
            ProductDto.CreateProductRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        CategoryEntity category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new SuperAdminService.BadRequestException(
                        "Category not found: " + request.getCategoryId()));
        if (!category.isActive()) {
            throw new SuperAdminService.BadRequestException("Category is inactive: " + category.getName());
        }

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setColour(request.getColour());
        product.setOccasion(request.getOccasion());
        product.setPerDayPrice(request.getPerDayPrice());
        product.setWeekendPrice(request.getWeekendPrice());
        product.setSecurityDeposit(request.getSecurityDeposit());
        product.setOfferPrice(request.getOfferPrice());
        product.setActive(true);
        product.setVisible(request.isVisible());
        product.setCreatedBy(actorId);

        product = productRepository.save(product);
        replaceImages(product, request.getImages());
        replaceInventory(product, request.getInventory());

        writeAudit(actorId, actorName, actorRole, "CREATE", product.getName(), null, productState(product), ip);
        return toResponse(product);
    }

    // ═══════════════════════════════════════════
    //  READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ProductDto.ProductListItemResponse> list(UUID categoryId, Boolean active, Boolean visible) {
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (categoryId != null) p.add(cb.equal(root.get("category").get("id"), categoryId));
            if (active != null) p.add(cb.equal(root.get("active"), active));
            if (visible != null) p.add(cb.equal(root.get("visible"), visible));
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return productRepository.findAll(spec).stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto.ProductResponse get(UUID id) {
        return toResponse(findProduct(id));
    }

    // ═══════════════════════════════════════════
    //  UPDATE (core fields)
    // ═══════════════════════════════════════════

    @Transactional
    public ProductDto.ProductResponse update(
            UUID id, ProductDto.UpdateProductRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(id);

        CategoryEntity category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new SuperAdminService.BadRequestException(
                        "Category not found: " + request.getCategoryId()));
        if (!category.isActive()) {
            throw new SuperAdminService.BadRequestException("Category is inactive: " + category.getName());
        }

        Map<String, Object> before = productState(product);

        product.setCategory(category);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setColour(request.getColour());
        product.setOccasion(request.getOccasion());
        product.setPerDayPrice(request.getPerDayPrice());
        product.setWeekendPrice(request.getWeekendPrice());
        product.setSecurityDeposit(request.getSecurityDeposit());
        product.setOfferPrice(request.getOfferPrice());
        product.setVisible(request.isVisible());
        product.setUpdatedBy(actorId);

        product = productRepository.save(product);

        writeAudit(actorId, actorName, actorRole, "UPDATE", product.getName(), before, productState(product), ip);
        return toResponse(product);
    }

    /** Full replace of a product's image list. */
    @Transactional
    public ProductDto.ProductResponse updateImages(
            UUID id, ProductDto.UpdateImagesRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(id);
        Map<String, Object> before = productState(product);

        replaceImages(product, request.getImages());
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        writeAudit(actorId, actorName, actorRole, "UPDATE_IMAGES", product.getName(), before, productState(product), ip);
        return toResponse(product);
    }

    /** Full replace of a product's inventory (size/SKU rows). */
    @Transactional
    public ProductDto.ProductResponse updateInventory(
            UUID id, ProductDto.UpdateInventoryRequest request,
            UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(id);
        Map<String, Object> before = productState(product);

        replaceInventory(product, request.getInventory());
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        writeAudit(actorId, actorName, actorRole, "UPDATE_INVENTORY", product.getName(), before, productState(product), ip);
        return toResponse(product);
    }

    /** Soft active/inactive toggle — controls visibility to end users, same idea as SubscriptionPlanService.setActive. */
    @Transactional
    public ProductDto.ProductResponse setActive(
            UUID id, boolean active,
            UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(id);
        boolean before = product.isActive();

        product.setActive(active);
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        writeAudit(actorId, actorName, actorRole, "ACTIVE_TOGGLE", product.getName(),
                Map.of("active", before), Map.of("active", active), ip);
        return toResponse(product);
    }

    /** Soft delete. Recommend enforcing at the API layer that no inventory row is currently RENTED before allowing this. */
    @Transactional
    public void delete(UUID id, UUID actorId, String actorName, String actorRole, String ip) {

        ProductEntity product = findProduct(id);
        Map<String, Object> before = productState(product);

        product.setActive(false);
        product.setVisible(false);
        product.setDeletedAt(LocalDateTime.now());
        product.setUpdatedBy(actorId);
        productRepository.save(product);

        writeAudit(actorId, actorName, actorRole, "DELETE", product.getName(), before,
                Map.of("active", false, "visible", false, "deleted", true), ip);
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    private void replaceImages(ProductEntity product, List<ProductDto.ImageItem> items) {
        imageRepository.deleteByProduct_Id(product.getId());
        product.getImages().clear();
        if (items == null) return;
        for (ProductDto.ImageItem item : items) {
            ProductImageEntity image = new ProductImageEntity();
            image.setProduct(product);
            image.setImageUrl(item.getImageUrl().trim());
            imageRepository.save(image);
        }
    }

    private void replaceInventory(ProductEntity product, List<ProductDto.InventoryItem> items) {
        inventoryRepository.deleteByProduct_Id(product.getId());
        product.getInventory().clear();
        if (items == null) return;
        Set<String> seenSkus = new HashSet<>();
        for (ProductDto.InventoryItem item : items) {
            String sku = item.getSku().trim().toUpperCase();
            if (!seenSkus.add(sku)) {
                throw new SuperAdminService.BadRequestException("Duplicate sku in request: " + sku);
            }
            inventoryRepository.findBySkuIgnoreCase(sku)
                    .filter(existing -> !existing.getProduct().getId().equals(product.getId()))
                    .ifPresent(existing -> {
                        throw new SuperAdminService.BadRequestException("SKU already in use: " + sku);
                    });
            if (item.getCondition() != null && !CONDITIONS.contains(item.getCondition().toUpperCase())) {
                throw new SuperAdminService.BadRequestException(
                        "Invalid condition '" + item.getCondition() + "'. Must be one of: " + CONDITIONS);
            }
            if (item.getStatus() != null && !INVENTORY_STATUSES.contains(item.getStatus().toUpperCase())) {
                throw new SuperAdminService.BadRequestException(
                        "Invalid status '" + item.getStatus() + "'. Must be one of: " + INVENTORY_STATUSES);
            }

            InventoryEntity inv = new InventoryEntity();
            inv.setProduct(product);
            inv.setSize(item.getSize().trim());
            inv.setSku(sku);
            inv.setCondition(item.getCondition() == null ? "NEW" : item.getCondition().toUpperCase());
            inv.setStatus(item.getStatus() == null ? "AVAILABLE" : item.getStatus().toUpperCase());
            inv.setActive(item.isActive());
            inv.setBlocked(item.isBlocked());
            inv.setBlockedFrom(item.getBlockedFrom());
            inv.setBlockedTo(item.getBlockedTo());
            inventoryRepository.save(inv);
        }
    }

    private ProductEntity findProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + id));
    }

    private void writeAudit(UUID actorId, String actorName, String actorRole, String action,
                             String targetLabel, Object before, Object after, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action,
                "PRODUCT_MANAGEMENT", targetLabel, before, after, ip);
    }

    private Map<String, Object> productState(ProductEntity product) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", product.getId());
        state.put("categoryId", product.getCategory() != null ? product.getCategory().getId() : null);
        state.put("name", product.getName());
        state.put("perDayPrice", product.getPerDayPrice());
        state.put("weekendPrice", product.getWeekendPrice());
        state.put("securityDeposit", product.getSecurityDeposit());
        state.put("offerPrice", product.getOfferPrice());
        state.put("active", product.isActive());
        state.put("visible", product.isVisible());
        state.put("imageCount", imageRepository.findByProduct_Id(product.getId()).size());
        state.put("inventoryCount", inventoryRepository.findByProduct_Id(product.getId()).size());
        return state;
    }

    private ProductDto.ProductResponse toResponse(ProductEntity product) {
        ProductDto.ProductResponse r = new ProductDto.ProductResponse();
        r.setId(product.getId());
        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        r.setCreatedBy(product.getCreatedBy());
        r.setName(product.getName());
        r.setDescription(product.getDescription());
        r.setColour(product.getColour());
        r.setOccasion(product.getOccasion());
        r.setPerDayPrice(product.getPerDayPrice());
        r.setWeekendPrice(product.getWeekendPrice());
        r.setSecurityDeposit(product.getSecurityDeposit());
        r.setOfferPrice(product.getOfferPrice());
        r.setActive(product.isActive());
        r.setVisible(product.isVisible());
        r.setCreatedAt(product.getCreatedAt());
        r.setUpdatedAt(product.getUpdatedAt());
        r.setDeletedAt(product.getDeletedAt());

        r.setImages(imageRepository.findByProduct_Id(product.getId()).stream().map(i -> {
            ProductDto.ImageResponse ir = new ProductDto.ImageResponse();
            ir.setId(i.getId());
            ir.setImageUrl(i.getImageUrl());
            return ir;
        }).collect(Collectors.toList()));

        r.setInventory(inventoryRepository.findByProduct_Id(product.getId()).stream().map(i -> {
            ProductDto.InventoryResponse ivr = new ProductDto.InventoryResponse();
            ivr.setId(i.getId());
            ivr.setSize(i.getSize());
            ivr.setSku(i.getSku());
            ivr.setCondition(i.getCondition());
            ivr.setStatus(i.getStatus());
            ivr.setActive(i.isActive());
            ivr.setBlocked(i.isBlocked());
            ivr.setBlockedFrom(i.getBlockedFrom());
            ivr.setBlockedTo(i.getBlockedTo());
            return ivr;
        }).collect(Collectors.toList()));

        return r;
    }

    private ProductDto.ProductListItemResponse toListItem(ProductEntity product) {
        ProductDto.ProductListItemResponse r = new ProductDto.ProductListItemResponse();
        r.setId(product.getId());
        r.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        r.setName(product.getName());
        r.setColour(product.getColour());
        r.setOccasion(product.getOccasion());
        r.setPerDayPrice(product.getPerDayPrice());
        r.setOfferPrice(product.getOfferPrice());
        r.setActive(product.isActive());
        r.setVisible(product.isVisible());
        imageRepository.findByProduct_Id(product.getId()).stream().findFirst()
                .ifPresent(img -> r.setCoverImageUrl(img.getImageUrl()));
        return r;
    }
}
