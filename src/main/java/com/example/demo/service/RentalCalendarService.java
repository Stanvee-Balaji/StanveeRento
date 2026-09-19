//package com.example.demo.service;
//
//import com.example.demo.dto.RentalCalendarDto;
//import com.example.demo.entity.*;
//import com.example.demo.repository.*;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Module 3 — Rental Calendar & Availability.
// *
// * Blocking is done at the INVENTORY UNIT level (one row per SKU), which is
// * what lets you block 5 of 10 identical suits for washing while the other
// * 5 stay bookable. Product-level "availability" is simply:
// *
// *   available(date) = total active units - blocked units(date) - booked units(date)
// *
// * `bookedUnits` is wired to 0 / a no-op hook (getOccupiedInventoryIds) since
// * a bookings/orders table wasn't in the provided schema. Once you have one,
// * implement that single method and occupancy + conflict checks flow through
// * automatically — nothing else in this service needs to change.
// *
// * Callable by:
// *   - Super Admin: unrestricted (SuperAdminRentalCalendarController)
// *   - Sub-Admin: gated by CalendarPermissionCodes.CALENDAR_* via
// *     AuthorizationService (AdminRentalCalendarController)
// *
// * Both controllers delegate here; actor-agnostic, same shape as
// * ProductService / SubscriptionPlanService.
// */
//@Service
//public class RentalCalendarService {
//
//    private static final Set<String> BLOCK_TYPES =
//            Set.of("MAINTENANCE", "CLEANING", "WASHING", "DAMAGE", "MANUAL_OVERRIDE", "OTHER");
//    private static final Set<String> SCOPES = Set.of("GLOBAL", "CATEGORY", "PRODUCT");
//
//    private final InventoryBlockRepository blockRepository;
//    private final CalendarConfigRepository configRepository;
//    private final InventoryRepository inventoryRepository;
//    private final ProductRepository productRepository;
//    private final CategoryRepository categoryRepository;
//    private final SuperAdminService superAdminService; // reused only for writeAudit(...)
//
//    public RentalCalendarService(
//            InventoryBlockRepository blockRepository,
//            CalendarConfigRepository configRepository,
//            InventoryRepository inventoryRepository,
//            ProductRepository productRepository,
//            CategoryRepository categoryRepository,
//            SuperAdminService superAdminService) {
//        this.blockRepository = blockRepository;
//        this.configRepository = configRepository;
//        this.inventoryRepository = inventoryRepository;
//        this.productRepository = productRepository;
//        this.categoryRepository = categoryRepository;
//        this.superAdminService = superAdminService;
//    }
//
//    // ═══════════════════════════════════════════
//    //  BLOCK
//    // ═══════════════════════════════════════════
//
//    /** Block explicit inventory units (known SKUs) for a date range. */
//    @Transactional
//    public List<RentalCalendarDto.BlockResponse> blockByIds(
//            RentalCalendarDto.BlockInventoryRequest request,
//            UUID actorId, String actorName, String actorType, String ip) {
//
//        validateBlockType(request.getBlockType());
//        validateRange(request.getStartDate(), request.getEndDate());
//
//        List<InventoryEntity> units = request.getInventoryIds().stream()
//                .map(id -> inventoryRepository.findById(id)
//                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
//                                "Inventory unit not found: " + id)))
//                .toList();
//
//        List<InventoryBlockEntity> created = new ArrayList<>();
//        for (InventoryEntity unit : units) {
//            assertNoOverlap(unit.getId(), request.getStartDate(), request.getEndDate());
//            created.add(saveBlock(unit, request.getBlockType(), request.getStartDate(),
//                    request.getEndDate(), request.getNotes(), actorId, actorType));
//        }
//
//        writeAudit(actorId, actorName, actorType, "BLOCK",
//                units.size() + " unit(s) - " + request.getBlockType(),
//                null, blockBatchState(created), ip);
//
//        return created.stream().map(this::toResponse).toList();
//    }
//
//    /**
//     * Block N units of a product without naming SKUs. Auto-selects the
//     * first N currently-available (active, unblocked, not conflicting)
//     * units for the given range — e.g. "block 5 of the 10 suits for
//     * washing, Jun 10–12".
//     */
//    @Transactional
//    public List<RentalCalendarDto.BlockResponse> blockByQuantity(
//            RentalCalendarDto.BlockByQuantityRequest request,
//            UUID actorId, String actorName, String actorType, String ip) {
//
//        validateBlockType(request.getBlockType());
//        validateRange(request.getStartDate(), request.getEndDate());
//
//        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
//                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
//                        "Product not found: " + request.getProductId()));
//
//        List<InventoryEntity> allUnits = inventoryRepository.findByProduct_Id(product.getId());
//        List<UUID> occupiedIds = getOccupiedInventoryIds(product.getId(), request.getStartDate(), request.getEndDate());
//
//        List<InventoryEntity> available = allUnits.stream()
//                .filter(InventoryEntity::isActive)
//                .filter(u -> !occupiedIds.contains(u.getId()))
//                .toList();
//
//        if (available.size() < request.getQuantity()) {
//            throw new SuperAdminService.BadRequestException(
//                    "Only " + available.size() + " unit(s) available for " + product.getName() +
//                    " in that range; cannot block " + request.getQuantity());
//        }
//
//        List<InventoryEntity> chosen = available.subList(0, request.getQuantity());
//        List<InventoryBlockEntity> created = new ArrayList<>();
//        for (InventoryEntity unit : chosen) {
//            created.add(saveBlock(unit, request.getBlockType(), request.getStartDate(),
//                    request.getEndDate(), request.getNotes(), actorId, actorType));
//        }
//
//        writeAudit(actorId, actorName, actorType, "BLOCK_BY_QUANTITY",
//                request.getQuantity() + "x " + product.getName() + " - " + request.getBlockType(),
//                null, blockBatchState(created), ip);
//
//        return created.stream().map(this::toResponse).toList();
//    }
//
//    private InventoryBlockEntity saveBlock(InventoryEntity unit, String blockType, LocalDate start,
//                                            LocalDate end, String notes, UUID actorId, String actorType) {
//        InventoryBlockEntity block = new InventoryBlockEntity();
//        block.setInventory(unit);
//        block.setProduct(unit.getProduct());
//        block.setBlockType(blockType.toUpperCase());
//        block.setStartDate(start);
//        block.setEndDate(end);
//        block.setNotes(notes);
//        block.setActive(true);
//        block.setCreatedBy(actorId);
//        block.setCreatedByType(actorType);
//        block = blockRepository.save(block);
//
//        // Keep the legacy inventory.is_blocked / blocked_from / blocked_to
//        // columns in sync as a "current state" cache for quick list views
//        // (only meaningful when the block covers "now").
//        syncInventoryFlag(unit);
//
//        return block;
//    }
//
//    // ═══════════════════════════════════════════
//    //  UNBLOCK
//    // ═══════════════════════════════════════════
//
//    @Transactional
//    public RentalCalendarDto.BlockResponse unblock(
//            UUID blockId, RentalCalendarDto.UnblockRequest request,
//            UUID actorId, String actorName, String actorType, String ip) {
//
//        InventoryBlockEntity block = blockRepository.findByIdAndActiveTrue(blockId)
//                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
//                        "Active block not found: " + blockId));
//
//        Map<String, Object> before = blockState(block);
//
//        block.setActive(false);
//        block.setReleasedAt(LocalDateTime.now());
//        block.setReleasedBy(actorId);
//        block.setReleasedByType(actorType);
//        block.setReleaseNotes(request == null ? null : request.getReleaseNotes());
//        block = blockRepository.save(block);
//
//        syncInventoryFlag(block.getInventory());
//
//        writeAudit(actorId, actorName, actorType, "UNBLOCK",
//                block.getInventory().getSku(), before, blockState(block), ip);
//
//        return toResponse(block);
//    }
//
//    // ═══════════════════════════════════════════
//    //  READ — blocks / availability
//    // ═══════════════════════════════════════════
//
//    @Transactional(readOnly = true)
//    public List<RentalCalendarDto.BlockResponse> listActiveBlocks(UUID productId, UUID inventoryId) {
//        List<InventoryBlockEntity> blocks;
//        if (inventoryId != null) {
//            blocks = blockRepository.findByInventory_IdAndActiveTrue(inventoryId);
//        } else if (productId != null) {
//            blocks = blockRepository.findByProduct_IdAndActiveTrue(productId);
//        } else {
//            throw new SuperAdminService.BadRequestException("productId or inventoryId is required");
//        }
//        return blocks.stream().map(this::toResponse).toList();
//    }
//
//    /**
//     * Day-by-day availability for a product across [from, to]. Blocked units
//     * are computed per-day from inventory_blocks; booked units are wired to
//     * getOccupiedInventoryIds(...) so real bookings plug in with zero
//     * changes to this method once that table exists.
//     */
//    @Transactional(readOnly = true)
//    public RentalCalendarDto.AvailabilityResponse getAvailability(UUID productId, LocalDate from, LocalDate to) {
//
//        validateRange(from, to);
//        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(productId)
//                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));
//
//        List<InventoryEntity> allUnits = inventoryRepository.findByProduct_Id(productId);
//        int totalActive = (int) allUnits.stream().filter(InventoryEntity::isActive).count();
//
//        List<InventoryBlockEntity> overlapping = blockRepository
//                .findByProduct_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
//                        productId, to, from);
//
//        List<RentalCalendarDto.DayAvailability> days = new ArrayList<>();
//        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
//            final LocalDate day = d;
//            long blockedCount = overlapping.stream()
//                    .filter(b -> !day.isBefore(b.getStartDate()) && !day.isAfter(b.getEndDate()))
//                    .map(b -> b.getInventory().getId())
//                    .distinct()
//                    .count();
//
//            int bookedCount = getOccupiedInventoryIds(productId, day, day).size();
//
//            RentalCalendarDto.DayAvailability da = new RentalCalendarDto.DayAvailability();
//            da.setDate(day);
//            da.setTotalUnits(totalActive);
//            da.setBlockedUnits((int) blockedCount);
//            da.setBookedUnits(bookedCount);
//            da.setAvailableUnits(Math.max(0, totalActive - (int) blockedCount - bookedCount));
//            days.add(da);
//        }
//
//        RentalCalendarDto.AvailabilityResponse response = new RentalCalendarDto.AvailabilityResponse();
//        response.setProductId(product.getId());
//        response.setProductName(product.getName());
//        response.setFrom(from);
//        response.setTo(to);
//        response.setDays(days);
//        return response;
//    }
//
//    /**
//     * Validates a proposed customer booking window against the resolved
//     * config (min rental duration, advance booking limit, cleaning buffer)
//     * and live availability. Call this from the booking/checkout flow once
//     * it exists — it doesn't create anything, just returns pass/fail +
//     * reasons, so the customer-facing calendar stays in real-time sync with
//     * whatever Admin has blocked.
//     */
//    @Transactional(readOnly = true)
//    public RentalCalendarDto.ValidateBookingResponse validateBooking(RentalCalendarDto.ValidateBookingRequest request) {
//
//        List<String> violations = new ArrayList<>();
//        RentalCalendarDto.ConfigResponse config = resolveConfig(request.getProductId());
//
//        long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
//        if (days < config.getMinRentalDays()) {
//            violations.add("Minimum rental duration is " + config.getMinRentalDays() + " day(s), requested " + days);
//        }
//        if (request.getStartDate().isAfter(LocalDate.now().plusDays(config.getAdvanceBookingDays()))) {
//            violations.add("Bookings can only be made up to " + config.getAdvanceBookingDays() + " day(s) in advance");
//        }
//        if (request.getStartDate().isBefore(LocalDate.now())) {
//            violations.add("Start date cannot be in the past");
//        }
//
//        // Effective end date including the mandatory post-rental cleaning buffer,
//        // used only for the availability check (not returned to the customer).
//        LocalDate effectiveEnd = request.getEndDate().plusDays(config.getCleaningBufferDays());
//        RentalCalendarDto.AvailabilityResponse availability =
//                getAvailability(request.getProductId(), request.getStartDate(), effectiveEnd);
//
//        boolean insufficientStock = availability.getDays().stream()
//                .anyMatch(d -> d.getAvailableUnits() < request.getQuantity());
//        if (insufficientStock) {
//            violations.add("Not enough units available for the requested quantity across the full date range " +
//                    "(including the " + config.getCleaningBufferDays() + "-day cleaning buffer)");
//        }
//
//        RentalCalendarDto.ValidateBookingResponse response = new RentalCalendarDto.ValidateBookingResponse();
//        response.setValid(violations.isEmpty());
//        response.setViolations(violations);
//        return response;
//    }
//
//    // ═══════════════════════════════════════════
//    //  CONFIG
//    // ═══════════════════════════════════════════
//
//    @Transactional
//    public RentalCalendarDto.ConfigResponse upsertConfig(
//            RentalCalendarDto.UpsertConfigRequest request,
//            UUID actorId, String actorName, String actorType, String ip) {
//
//        String scope = request.getScope() == null ? "" : request.getScope().toUpperCase();
//        if (!SCOPES.contains(scope)) {
//            throw new SuperAdminService.BadRequestException("Invalid scope. Must be one of: " + SCOPES);
//        }
//        if (request.getMinRentalDays() < 1) {
//            throw new SuperAdminService.BadRequestException("minRentalDays must be >= 1");
//        }
//
//        Optional<CalendarConfigEntity> existing;
//        CategoryEntity category = null;
//        ProductEntity product = null;
//
//        switch (scope) {
//            case "GLOBAL" -> existing = configRepository.findByScopeAndActiveTrue("GLOBAL");
//            case "CATEGORY" -> {
//                if (request.getCategoryId() == null)
//                    throw new SuperAdminService.BadRequestException("categoryId is required for CATEGORY scope");
//                category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
//                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Category not found"));
//                existing = configRepository.findByCategory_IdAndScopeAndActiveTrue(category.getId(), "CATEGORY");
//            }
//            default -> { // PRODUCT
//                if (request.getProductId() == null)
//                    throw new SuperAdminService.BadRequestException("productId is required for PRODUCT scope");
//                product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
//                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found"));
//                existing = configRepository.findByProduct_IdAndScopeAndActiveTrue(product.getId(), "PRODUCT");
//            }
//        }
//
//        CalendarConfigEntity entity = existing.orElseGet(CalendarConfigEntity::new);
//        Map<String, Object> before = entity.getId() == null ? null : configState(entity);
//
//        entity.setScope(scope);
//        entity.setCategory(category);
//        entity.setProduct(product);
//        entity.setCleaningBufferDays(request.getCleaningBufferDays());
//        entity.setMinRentalDays(request.getMinRentalDays());
//        entity.setAdvanceBookingDays(request.getAdvanceBookingDays());
//        entity.setActive(true);
//        entity.setUpdatedBy(actorId);
//        entity = configRepository.save(entity);
//
//        writeAudit(actorId, actorName, actorType, before == null ? "CREATE_CONFIG" : "UPDATE_CONFIG",
//                scope + (product != null ? ":" + product.getName() : category != null ? ":" + category.getName() : ""),
//                before, configState(entity), ip);
//
//        return toConfigResponse(entity);
//    }
//
//    @Transactional(readOnly = true)
//    public List<RentalCalendarDto.ConfigResponse> listConfigs() {
//        return configRepository.findByActiveTrue().stream().map(this::toConfigResponse).toList();
//    }
//
//    /** Resolution order: PRODUCT override > CATEGORY override > GLOBAL default. */
//    @Transactional(readOnly = true)
//    public RentalCalendarDto.ConfigResponse resolveConfig(UUID productId) {
//        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(productId)
//                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));
//
//        Optional<CalendarConfigEntity> productConfig =
//                configRepository.findByProduct_IdAndScopeAndActiveTrue(productId, "PRODUCT");
//        if (productConfig.isPresent()) return toConfigResponse(productConfig.get());
//
//        if (product.getCategory() != null) {
//            Optional<CalendarConfigEntity> categoryConfig = configRepository
//                    .findByCategory_IdAndScopeAndActiveTrue(product.getCategory().getId(), "CATEGORY");
//            if (categoryConfig.isPresent()) return toConfigResponse(categoryConfig.get());
//        }
//
//        CalendarConfigEntity global = configRepository.findByScopeAndActiveTrue("GLOBAL")
//                .orElseThrow(() -> new IllegalStateException(
//                        "No GLOBAL calendar_config row exists — this should never happen; re-run the seed migration"));
//        return toConfigResponse(global);
//    }
//
//    // ═══════════════════════════════════════════
//    //  INTERNAL HELPERS
//    // ═══════════════════════════════════════════
//
//    private void assertNoOverlap(UUID inventoryId, LocalDate start, LocalDate end) {
//        List<InventoryBlockEntity> overlaps = blockRepository
//                .findByInventory_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
//                        inventoryId, end, start);
//        if (!overlaps.isEmpty()) {
//            throw new SuperAdminService.BadRequestException(
//                    "Inventory unit already has an active block overlapping that range");
//        }
//    }
//
//    private void syncInventoryFlag(InventoryEntity unit) {
//        LocalDate today = LocalDate.now();
//        boolean blockedNow = blockRepository.findByInventory_IdAndActiveTrue(unit.getId()).stream()
//                .anyMatch(b -> !today.isBefore(b.getStartDate()) && !today.isAfter(b.getEndDate()));
//        unit.setBlocked(blockedNow);
//        inventoryRepository.save(unit);
//    }
//
//    /**
//     * Hook for real bookings. Returns inventory unit IDs of `productId` that
//     * are occupied by a confirmed booking overlapping [from, to]. Returns
//     * empty until a bookings/orders table is wired in — see the class
//     * Javadoc and the SQL migration NOTE.
//     */
//    private List<UUID> getOccupiedInventoryIds(UUID productId, LocalDate from, LocalDate to) {
//        return List.of();
//    }
//
//    private void validateBlockType(String blockType) {
//        if (blockType == null || !BLOCK_TYPES.contains(blockType.toUpperCase())) {
//            throw new SuperAdminService.BadRequestException("Invalid blockType. Must be one of: " + BLOCK_TYPES);
//        }
//    }
//
//    private void validateRange(LocalDate start, LocalDate end) {
//        if (start == null || end == null || end.isBefore(start)) {
//            throw new SuperAdminService.BadRequestException("endDate must be on/after startDate");
//        }
//    }
//
//    private void writeAudit(UUID actorId, String actorName, String actorRole, String action,
//                             String targetLabel, Object before, Object after, String ip) {
//        superAdminService.writeAudit(actorId, actorName, actorRole, action,
//                "RENTAL_CALENDAR_MANAGEMENT", targetLabel, before, after, ip);
//    }
//
//    private Map<String, Object> blockState(InventoryBlockEntity b) {
//        Map<String, Object> state = new LinkedHashMap<>();
//        state.put("id", b.getId());
//        state.put("inventoryId", b.getInventory().getId());
//        state.put("sku", b.getInventory().getSku());
//        state.put("blockType", b.getBlockType());
//        state.put("startDate", b.getStartDate());
//        state.put("endDate", b.getEndDate());
//        state.put("active", b.isActive());
//        return state;
//    }
//
//    private List<Map<String, Object>> blockBatchState(List<InventoryBlockEntity> blocks) {
//        return blocks.stream().map(this::blockState).collect(Collectors.toList());
//    }
//
//    private Map<String, Object> configState(CalendarConfigEntity c) {
//        Map<String, Object> state = new LinkedHashMap<>();
//        state.put("id", c.getId());
//        state.put("scope", c.getScope());
//        state.put("cleaningBufferDays", c.getCleaningBufferDays());
//        state.put("minRentalDays", c.getMinRentalDays());
//        state.put("advanceBookingDays", c.getAdvanceBookingDays());
//        return state;
//    }
//
//    private RentalCalendarDto.BlockResponse toResponse(InventoryBlockEntity b) {
//        RentalCalendarDto.BlockResponse r = new RentalCalendarDto.BlockResponse();
//        r.setId(b.getId());
//        r.setInventoryId(b.getInventory().getId());
//        r.setSku(b.getInventory().getSku());
//        r.setProductId(b.getProduct().getId());
//        r.setProductName(b.getProduct().getName());
//        r.setBlockType(b.getBlockType());
//        r.setStartDate(b.getStartDate());
//        r.setEndDate(b.getEndDate());
//        r.setNotes(b.getNotes());
//        r.setActive(b.isActive());
//        r.setCreatedByType(b.getCreatedByType());
//        r.setCreatedAt(b.getCreatedAt());
//        r.setReleasedAt(b.getReleasedAt());
//        r.setReleaseNotes(b.getReleaseNotes());
//        return r;
//    }
//
//    private RentalCalendarDto.ConfigResponse toConfigResponse(CalendarConfigEntity c) {
//        RentalCalendarDto.ConfigResponse r = new RentalCalendarDto.ConfigResponse();
//        r.setId(c.getId());
//        r.setScope(c.getScope());
//        r.setCategoryId(c.getCategory() != null ? c.getCategory().getId() : null);
//        r.setProductId(c.getProduct() != null ? c.getProduct().getId() : null);
//        r.setCleaningBufferDays(c.getCleaningBufferDays());
//        r.setMinRentalDays(c.getMinRentalDays());
//        r.setAdvanceBookingDays(c.getAdvanceBookingDays());
//        r.setUpdatedAt(c.getUpdatedAt());
//        return r;
//    }
//
//    
//    
//}


package com.example.demo.service;

import com.example.demo.dto.RentalCalendarDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 3 — Rental Calendar & Availability.
 *
 * Blocking is done at the INVENTORY UNIT level (one row per SKU), which is
 * what lets you block 5 of 10 identical suits for washing while the other
 * 5 stay bookable. Product-level "availability" is simply:
 *
 *   available(date) = total active units - blocked units(date) - booked units(date)
 *
 * `bookedUnits` is wired to 0 / a no-op hook (getOccupiedInventoryIds) since
 * a bookings/orders table wasn't in the provided schema. Once you have one,
 * implement that single method and occupancy + conflict checks flow through
 * automatically — nothing else in this service needs to change.
 *
 * Callable by:
 *   - Super Admin: unrestricted (SuperAdminRentalCalendarController)
 *   - Sub-Admin: gated by CalendarPermissionCodes.CALENDAR_* via
 *     AuthorizationService (AdminRentalCalendarController)
 *
 * Both controllers delegate here; actor-agnostic, same shape as
 * ProductService / SubscriptionPlanService.
 */
@Service
public class RentalCalendarService {

    private static final Set<String> BLOCK_TYPES =
            Set.of("MAINTENANCE", "CLEANING", "WASHING", "DAMAGE", "MANUAL_OVERRIDE", "OTHER");
    private static final Set<String> SCOPES = Set.of("GLOBAL", "CATEGORY", "PRODUCT");

    private final InventoryBlockRepository blockRepository;
    private final CalendarConfigRepository configRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)

    public RentalCalendarService(
            InventoryBlockRepository blockRepository,
            CalendarConfigRepository configRepository,
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SuperAdminService superAdminService) {
        this.blockRepository = blockRepository;
        this.configRepository = configRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  BLOCK
    // ═══════════════════════════════════════════

    /** Block explicit inventory units (known SKUs) for a date range. */
    @Transactional
    public List<RentalCalendarDto.BlockResponse> blockByIds(
            RentalCalendarDto.BlockInventoryRequest request,
            UUID actorId, String actorName, String actorType, String ip) {

        validateBlockType(request.getBlockType());
        validateRange(request.getStartDate(), request.getEndDate());

        List<InventoryEntity> units = request.getInventoryIds().stream()
                .map(id -> inventoryRepository.findById(id)
                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                                "Inventory unit not found: " + id)))
                .toList();

        List<InventoryBlockEntity> created = new ArrayList<>();
        for (InventoryEntity unit : units) {
            assertNoOverlap(unit.getId(), request.getStartDate(), request.getEndDate());
            created.add(saveBlock(unit, request.getBlockType(), request.getStartDate(),
                    request.getEndDate(), request.getNotes(), actorId, actorType));
        }

        writeAudit(actorId, actorName, actorType, "BLOCK",
                units.size() + " unit(s) - " + request.getBlockType(),
                null, blockBatchState(created), ip);

        return created.stream().map(this::toResponse).toList();
    }

    /**
     * Block N units of a product without naming SKUs. Auto-selects the
     * first N currently-available (active, unblocked, not conflicting)
     * units for the given range — e.g. "block 5 of the 10 suits for
     * washing, Jun 10–12".
     */
    @Transactional
    public List<RentalCalendarDto.BlockResponse> blockByQuantity(
            RentalCalendarDto.BlockByQuantityRequest request,
            UUID actorId, String actorName, String actorType, String ip) {

        validateBlockType(request.getBlockType());
        validateRange(request.getStartDate(), request.getEndDate());

        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));

        List<InventoryEntity> allUnits = inventoryRepository.findByProduct_Id(product.getId());
        List<UUID> occupiedIds = getOccupiedInventoryIds(product.getId(), request.getStartDate(), request.getEndDate());

        List<InventoryEntity> available = allUnits.stream()
                .filter(InventoryEntity::isActive)
                .filter(u -> !occupiedIds.contains(u.getId()))
                .toList();

        if (available.size() < request.getQuantity()) {
            throw new SuperAdminService.BadRequestException(
                    "Only " + available.size() + " unit(s) available for " + product.getName() +
                    " in that range; cannot block " + request.getQuantity());
        }

        List<InventoryEntity> chosen = available.subList(0, request.getQuantity());
        List<InventoryBlockEntity> created = new ArrayList<>();
        for (InventoryEntity unit : chosen) {
            created.add(saveBlock(unit, request.getBlockType(), request.getStartDate(),
                    request.getEndDate(), request.getNotes(), actorId, actorType));
        }

        writeAudit(actorId, actorName, actorType, "BLOCK_BY_QUANTITY",
                request.getQuantity() + "x " + product.getName() + " - " + request.getBlockType(),
                null, blockBatchState(created), ip);

        return created.stream().map(this::toResponse).toList();
    }

    private InventoryBlockEntity saveBlock(InventoryEntity unit, String blockType, LocalDate start,
                                            LocalDate end, String notes, UUID actorId, String actorType) {
        InventoryBlockEntity block = new InventoryBlockEntity();
        block.setInventory(unit);
        block.setProduct(unit.getProduct());
        block.setBlockType(blockType.toUpperCase());
        block.setStartDate(start);
        block.setEndDate(end);
        block.setNotes(notes);
        block.setActive(true);
        block.setCreatedBy(actorId);
        block.setCreatedByType(actorType);
        block = blockRepository.save(block);

        // Keep the legacy inventory.is_blocked / blocked_from / blocked_to
        // columns in sync as a "current state" cache for quick list views
        // (only meaningful when the block covers "now").
        syncInventoryFlag(unit);

        return block;
    }

    // ═══════════════════════════════════════════
    //  UNBLOCK
    // ═══════════════════════════════════════════

    @Transactional
    public RentalCalendarDto.BlockResponse unblock(
            UUID blockId, RentalCalendarDto.UnblockRequest request,
            UUID actorId, String actorName, String actorType, String ip) {

        InventoryBlockEntity block = blockRepository.findByIdAndActiveTrue(blockId)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Active block not found: " + blockId));

        Map<String, Object> before = blockState(block);

        block.setActive(false);
        block.setReleasedAt(LocalDateTime.now());
        block.setReleasedBy(actorId);
        block.setReleasedByType(actorType);
        block.setReleaseNotes(request == null ? null : request.getReleaseNotes());
        block = blockRepository.save(block);

        syncInventoryFlag(block.getInventory());

        writeAudit(actorId, actorName, actorType, "UNBLOCK",
                block.getInventory().getSku(), before, blockState(block), ip);

        return toResponse(block);
    }

    // ═══════════════════════════════════════════
    //  READ — blocks / availability
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RentalCalendarDto.BlockResponse> listActiveBlocks(UUID productId, UUID inventoryId) {
        List<InventoryBlockEntity> blocks;
        if (inventoryId != null) {
            blocks = blockRepository.findByInventory_IdAndActiveTrue(inventoryId);
        } else if (productId != null) {
            blocks = blockRepository.findByProduct_IdAndActiveTrue(productId);
        } else {
            throw new SuperAdminService.BadRequestException("productId or inventoryId is required");
        }
        return blocks.stream().map(this::toResponse).toList();
    }

    /**
     * Day-by-day availability for a product across [from, to]. Blocked units
     * are computed per-day from inventory_blocks; booked units are wired to
     * getOccupiedInventoryIds(...) so real bookings plug in with zero
     * changes to this method once that table exists.
     */
    @Transactional(readOnly = true)
    public RentalCalendarDto.AvailabilityResponse getAvailability(UUID productId, LocalDate from, LocalDate to) {

        validateRange(from, to);
        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));

        List<InventoryEntity> allUnits = inventoryRepository.findByProduct_Id(productId);
        int totalActive = (int) allUnits.stream().filter(InventoryEntity::isActive).count();

        List<InventoryBlockEntity> overlapping = blockRepository
                .findByProduct_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        productId, to, from);

        List<RentalCalendarDto.DayAvailability> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            long blockedCount = overlapping.stream()
                    .filter(b -> !day.isBefore(b.getStartDate()) && !day.isAfter(b.getEndDate()))
                    .map(b -> b.getInventory().getId())
                    .distinct()
                    .count();

            int bookedCount = getOccupiedInventoryIds(productId, day, day).size();

            RentalCalendarDto.DayAvailability da = new RentalCalendarDto.DayAvailability();
            da.setDate(day);
            da.setTotalUnits(totalActive);
            da.setBlockedUnits((int) blockedCount);
            da.setBookedUnits(bookedCount);
            da.setAvailableUnits(Math.max(0, totalActive - (int) blockedCount - bookedCount));
            days.add(da);
        }

        RentalCalendarDto.AvailabilityResponse response = new RentalCalendarDto.AvailabilityResponse();
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setFrom(from);
        response.setTo(to);
        response.setDays(days);
        return response;
    }

    /**
     * Validates a proposed customer booking window against the resolved
     * config (min rental duration, advance booking limit, cleaning buffer)
     * and live availability. Call this from the booking/checkout flow once
     * it exists — it doesn't create anything, just returns pass/fail +
     * reasons, so the customer-facing calendar stays in real-time sync with
     * whatever Admin has blocked.
     */
    @Transactional(readOnly = true)
    public RentalCalendarDto.ValidateBookingResponse validateBooking(RentalCalendarDto.ValidateBookingRequest request) {

        List<String> violations = new ArrayList<>();
        RentalCalendarDto.ConfigResponse config = resolveConfig(request.getProductId());

        long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days < config.getMinRentalDays()) {
            violations.add("Minimum rental duration is " + config.getMinRentalDays() + " day(s), requested " + days);
        }
        if (request.getStartDate().isAfter(LocalDate.now().plusDays(config.getAdvanceBookingDays()))) {
            violations.add("Bookings can only be made up to " + config.getAdvanceBookingDays() + " day(s) in advance");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            violations.add("Start date cannot be in the past");
        }

        // Effective end date including the mandatory post-rental cleaning buffer,
        // used only for the availability check (not returned to the customer).
        LocalDate effectiveEnd = request.getEndDate().plusDays(config.getCleaningBufferDays());
        RentalCalendarDto.AvailabilityResponse availability =
                getAvailability(request.getProductId(), request.getStartDate(), effectiveEnd);

        boolean insufficientStock = availability.getDays().stream()
                .anyMatch(d -> d.getAvailableUnits() < request.getQuantity());
        if (insufficientStock) {
            violations.add("Not enough units available for the requested quantity across the full date range " +
                    "(including the " + config.getCleaningBufferDays() + "-day cleaning buffer)");
        }

        RentalCalendarDto.ValidateBookingResponse response = new RentalCalendarDto.ValidateBookingResponse();
        response.setValid(violations.isEmpty());
        response.setViolations(violations);
        return response;
    }

    // ═══════════════════════════════════════════
    //  CONFIG
    // ═══════════════════════════════════════════

    @Transactional
    public RentalCalendarDto.ConfigResponse upsertConfig(
            RentalCalendarDto.UpsertConfigRequest request,
            UUID actorId, String actorName, String actorType, String ip) {

        String scope = request.getScope() == null ? "" : request.getScope().toUpperCase();
        if (!SCOPES.contains(scope)) {
            throw new SuperAdminService.BadRequestException("Invalid scope. Must be one of: " + SCOPES);
        }
        if (request.getMinRentalDays() < 1) {
            throw new SuperAdminService.BadRequestException("minRentalDays must be >= 1");
        }

        Optional<CalendarConfigEntity> existing;
        CategoryEntity category = null;
        ProductEntity product = null;

        switch (scope) {
            case "GLOBAL" -> existing = configRepository.findByScopeAndActiveTrue("GLOBAL");
            case "CATEGORY" -> {
                if (request.getCategoryId() == null)
                    throw new SuperAdminService.BadRequestException("categoryId is required for CATEGORY scope");
                category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Category not found"));
                existing = configRepository.findByCategory_IdAndScopeAndActiveTrue(category.getId(), "CATEGORY");
            }
            default -> { // PRODUCT
                if (request.getProductId() == null)
                    throw new SuperAdminService.BadRequestException("productId is required for PRODUCT scope");
                product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                        .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found"));
                existing = configRepository.findByProduct_IdAndScopeAndActiveTrue(product.getId(), "PRODUCT");
            }
        }

        CalendarConfigEntity entity = existing.orElseGet(CalendarConfigEntity::new);
        Map<String, Object> before = entity.getId() == null ? null : configState(entity);

        entity.setScope(scope);
        entity.setCategory(category);
        entity.setProduct(product);
        entity.setCleaningBufferDays(request.getCleaningBufferDays());
        entity.setMinRentalDays(request.getMinRentalDays());
        entity.setAdvanceBookingDays(request.getAdvanceBookingDays());
        entity.setActive(true);
        entity.setUpdatedBy(actorId);
        entity = configRepository.save(entity);

        writeAudit(actorId, actorName, actorType, before == null ? "CREATE_CONFIG" : "UPDATE_CONFIG",
                scope + (product != null ? ":" + product.getName() : category != null ? ":" + category.getName() : ""),
                before, configState(entity), ip);

        return toConfigResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<RentalCalendarDto.ConfigResponse> listConfigs() {
        return configRepository.findByActiveTrue().stream().map(this::toConfigResponse).toList();
    }

    /** Resolution order: PRODUCT override > CATEGORY override > GLOBAL default. */
    @Transactional
    public RentalCalendarDto.ConfigResponse resolveConfig(UUID productId) {
        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));

        Optional<CalendarConfigEntity> productConfig =
                configRepository.findByProduct_IdAndScopeAndActiveTrue(productId, "PRODUCT");
        if (productConfig.isPresent()) return toConfigResponse(productConfig.get());

        if (product.getCategory() != null) {
            Optional<CalendarConfigEntity> categoryConfig = configRepository
                    .findByCategory_IdAndScopeAndActiveTrue(product.getCategory().getId(), "CATEGORY");
            if (categoryConfig.isPresent()) return toConfigResponse(categoryConfig.get());
        }

        // No GLOBAL row yet (fresh DB, nobody has called upsertConfig for
        // GLOBAL scope yet). Instead of throwing, create one with sane
        // defaults right here and persist it — this only ever runs once;
        // every call after this finds the row via findByScopeAndActiveTrue
        // above. Admin can still change these values anytime afterwards
        // via upsertConfig.
        CalendarConfigEntity global = configRepository.findByScopeAndActiveTrue("GLOBAL")
                .orElseGet(this::createDefaultGlobalConfig);
        return toConfigResponse(global);
    }

    private CalendarConfigEntity createDefaultGlobalConfig() {
        CalendarConfigEntity global = new CalendarConfigEntity();
        global.setScope("GLOBAL");
        global.setMinRentalDays(1);
        global.setAdvanceBookingDays(90);
        global.setCleaningBufferDays(1);
        global.setActive(true);
        return configRepository.save(global);
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    private void assertNoOverlap(UUID inventoryId, LocalDate start, LocalDate end) {
        List<InventoryBlockEntity> overlaps = blockRepository
                .findByInventory_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        inventoryId, end, start);
        if (!overlaps.isEmpty()) {
            throw new SuperAdminService.BadRequestException(
                    "Inventory unit already has an active block overlapping that range");
        }
    }

    private void syncInventoryFlag(InventoryEntity unit) {
        LocalDate today = LocalDate.now();
        boolean blockedNow = blockRepository.findByInventory_IdAndActiveTrue(unit.getId()).stream()
                .anyMatch(b -> !today.isBefore(b.getStartDate()) && !today.isAfter(b.getEndDate()));
        unit.setBlocked(blockedNow);
        inventoryRepository.save(unit);
    }

    /**
     * Hook for real bookings. Returns inventory unit IDs of `productId` that
     * are occupied by a confirmed booking overlapping [from, to]. Returns
     * empty until a bookings/orders table is wired in — see the class
     * Javadoc and the SQL migration NOTE.
     */
    private List<UUID> getOccupiedInventoryIds(UUID productId, LocalDate from, LocalDate to) {
        return List.of();
    }

    private void validateBlockType(String blockType) {
        if (blockType == null || !BLOCK_TYPES.contains(blockType.toUpperCase())) {
            throw new SuperAdminService.BadRequestException("Invalid blockType. Must be one of: " + BLOCK_TYPES);
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new SuperAdminService.BadRequestException("endDate must be on/after startDate");
        }
    }

    private void writeAudit(UUID actorId, String actorName, String actorRole, String action,
                             String targetLabel, Object before, Object after, String ip) {
        superAdminService.writeAudit(actorId, actorName, actorRole, action,
                "RENTAL_CALENDAR_MANAGEMENT", targetLabel, before, after, ip);
    }

    private Map<String, Object> blockState(InventoryBlockEntity b) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", b.getId());
        state.put("inventoryId", b.getInventory().getId());
        state.put("sku", b.getInventory().getSku());
        state.put("blockType", b.getBlockType());
        state.put("startDate", b.getStartDate());
        state.put("endDate", b.getEndDate());
        state.put("active", b.isActive());
        return state;
    }

    private List<Map<String, Object>> blockBatchState(List<InventoryBlockEntity> blocks) {
        return blocks.stream().map(this::blockState).collect(Collectors.toList());
    }

    private Map<String, Object> configState(CalendarConfigEntity c) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", c.getId());
        state.put("scope", c.getScope());
        state.put("cleaningBufferDays", c.getCleaningBufferDays());
        state.put("minRentalDays", c.getMinRentalDays());
        state.put("advanceBookingDays", c.getAdvanceBookingDays());
        return state;
    }

    private RentalCalendarDto.BlockResponse toResponse(InventoryBlockEntity b) {
        RentalCalendarDto.BlockResponse r = new RentalCalendarDto.BlockResponse();
        r.setId(b.getId());
        r.setInventoryId(b.getInventory().getId());
        r.setSku(b.getInventory().getSku());
        r.setProductId(b.getProduct().getId());
        r.setProductName(b.getProduct().getName());
        r.setBlockType(b.getBlockType());
        r.setStartDate(b.getStartDate());
        r.setEndDate(b.getEndDate());
        r.setNotes(b.getNotes());
        r.setActive(b.isActive());
        r.setCreatedByType(b.getCreatedByType());
        r.setCreatedAt(b.getCreatedAt());
        r.setReleasedAt(b.getReleasedAt());
        r.setReleaseNotes(b.getReleaseNotes());
        return r;
    }

    private RentalCalendarDto.ConfigResponse toConfigResponse(CalendarConfigEntity c) {
        RentalCalendarDto.ConfigResponse r = new RentalCalendarDto.ConfigResponse();
        r.setId(c.getId());
        r.setScope(c.getScope());
        r.setCategoryId(c.getCategory() != null ? c.getCategory().getId() : null);
        r.setProductId(c.getProduct() != null ? c.getProduct().getId() : null);
        r.setCleaningBufferDays(c.getCleaningBufferDays());
        r.setMinRentalDays(c.getMinRentalDays());
        r.setAdvanceBookingDays(c.getAdvanceBookingDays());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }

    
    
}