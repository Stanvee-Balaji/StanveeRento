package com.example.demo.service;

import com.example.demo.dto.InventoryDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.util.LocalINDTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-side Inventory Management (read-only dashboard, stock lists, unit
 * details, breakdowns, CSV export).
 * Callable by:
 *   - Super Admin: unrestricted (SuperAdminInventoryController)
 *   - Sub-Admin: gated by InventoryPermissionCodes.INVENTORY_* via
 *     AuthorizationService (AdminInventoryController)
 *
 * Actor-agnostic, same shape as ProductService / RentalCalendarService.
 *
 * ── How a unit's live STATE is computed (precedence, first match wins) ──
 *   1. inventory.is_active = false              -> INACTIVE
 *   2. inventory.status = RENTED                -> RENTED
 *   3. inventory.status = DAMAGED               -> DAMAGED
 *   4. active inventory_blocks row covers TODAY -> BLOCKED
 *   5. inventory.status = CLEANING              -> CLEANING
 *   6. otherwise                                -> AVAILABLE
 *
 * "Rented" comes from inventory.status = RENTED because there is no
 * orders/bookings table yet. When one exists, change ONLY Snapshot.state(...)
 * (add a rule for "unit has a confirmed booking covering today").
 *
 * Data is loaded once per request and aggregated in memory. That is fine for
 * a rental catalogue (hundreds to a few thousand units). If it grows to tens
 * of thousands, replace load() with GROUP BY queries.
 */
@Service
public class InventoryManagementService {

    private static final Set<String> BREAKDOWN_FIELDS =
            Set.of("category", "colour", "size", "condition", "status");
    private static final Set<String> UNIT_STATES =
            Set.of("AVAILABLE", "RENTED", "BLOCKED", "CLEANING", "DAMAGED", "INACTIVE");
    private static final Set<String> STOCK_STATUSES = Set.of("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK");
    private static final int DEFAULT_LOW_STOCK = 2;
    private static final String MODULE = "INVENTORY_MANAGEMENT";

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBlockRepository blockRepository;
    private final ProductImageRepository imageRepository;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)

    public InventoryManagementService(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            InventoryBlockRepository blockRepository,
            ProductImageRepository imageRepository,
            SuperAdminService superAdminService) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.blockRepository = blockRepository;
        this.imageRepository = imageRepository;
        this.superAdminService = superAdminService;
    }

    // ═══════════════════════════════════════════
    //  SUMMARY
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public InventoryDto.SummaryResponse summary(Integer lowStockThreshold) {
        return buildSummary(load(), threshold(lowStockThreshold));
    }

    // ═══════════════════════════════════════════
    //  DASHBOARD (summary + charts + alerts in one call)
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public InventoryDto.DashboardResponse dashboard(Integer lowStockThreshold, Integer upcomingDays) {
        Snapshot s = load();
        int th = threshold(lowStockThreshold);
        List<InventoryDto.ProductStockRow> rows = buildProductRows(s, th);

        InventoryDto.DashboardResponse r = new InventoryDto.DashboardResponse();
        r.setSummary(buildSummary(s, th));
        r.setByCategory(buildBreakdown(s, "category"));
        r.setByColour(buildBreakdown(s, "colour"));
        r.setBySize(buildBreakdown(s, "size"));
        r.setByCondition(buildBreakdown(s, "condition"));
        r.setByStatus(buildBreakdown(s, "status"));
        r.setOutOfStockProducts(rows.stream()
                .filter(p -> p.isActive() && "OUT_OF_STOCK".equals(p.getStockStatus())).toList());
        r.setLowStockProducts(rows.stream()
                .filter(p -> p.isActive() && "LOW_STOCK".equals(p.getStockStatus())).toList());
        r.setUpcomingBlocks(buildUpcoming(s, days(upcomingDays)));
        return r;
    }

    // ═══════════════════════════════════════════
    //  BREAKDOWN
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<InventoryDto.BreakdownItem> breakdown(String by) {
        String field = by == null ? "" : by.trim().toLowerCase();
        if (!BREAKDOWN_FIELDS.contains(field)) {
            throw new SuperAdminService.BadRequestException(
                    "Invalid 'by'. Must be one of: " + BREAKDOWN_FIELDS);
        }
        return buildBreakdown(load(), field);
    }

    // ═══════════════════════════════════════════
    //  PRODUCTS (stock per product)
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public InventoryDto.PageResponse<InventoryDto.ProductStockRow> listProducts(
            UUID categoryId, String search, String stockStatus, Integer lowStockThreshold,
            int page, int size, String sortBy, String sortDir) {

        String ss = null;
        if (stockStatus != null && !stockStatus.isBlank()) {
            ss = stockStatus.trim().toUpperCase();
            if (!STOCK_STATUSES.contains(ss)) {
                throw new SuperAdminService.BadRequestException(
                        "Invalid stockStatus. Must be one of: " + STOCK_STATUSES);
            }
        }
        final String stockFilter = ss;
        final String q = search == null ? null : search.trim().toLowerCase();

        List<InventoryDto.ProductStockRow> rows = buildProductRows(load(), threshold(lowStockThreshold)).stream()
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .filter(p -> q == null || q.isEmpty()
                        || (p.getName() != null && p.getName().toLowerCase().contains(q)))
                .filter(p -> stockFilter == null || stockFilter.equals(p.getStockStatus()))
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<InventoryDto.ProductStockRow> cmp = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "totalunits"  -> Comparator.comparingInt(InventoryDto.ProductStockRow::getTotalUnits);
            case "available", "availableunits" -> Comparator.comparingInt(InventoryDto.ProductStockRow::getAvailableUnits);
            case "rented", "rentedunits" -> Comparator.comparingInt(InventoryDto.ProductStockRow::getRentedUnits);
            case "utilization" -> Comparator.comparingDouble(InventoryDto.ProductStockRow::getUtilizationPercent);
            case "createdat"   -> Comparator.comparing(InventoryDto.ProductStockRow::getCreatedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()));
            default            -> Comparator.comparing(p -> p.getName() == null ? "" : p.getName().toLowerCase());
        };
        if (!"asc".equalsIgnoreCase(sortDir) && sortBy != null && !sortBy.isBlank()
                && !"name".equalsIgnoreCase(sortBy)) {
            cmp = cmp.reversed(); // numeric / date sorts default to DESC
        } else if ("desc".equalsIgnoreCase(sortDir)) {
            cmp = cmp.reversed();
        }
        rows.sort(cmp);
        return paginate(rows, page, size);
    }

    @Transactional(readOnly = true)
    public InventoryDto.ProductInventoryDetailResponse getProduct(UUID productId, Integer lowStockThreshold) {
        Snapshot s = load();
        ProductEntity product = s.products.get(productId);
        if (product == null) {
            throw new SuperAdminService.ResourceNotFoundException("Product not found: " + productId);
        }

        List<InventoryEntity> units = s.units.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .toList();

        InventoryDto.ProductInventoryDetailResponse r = new InventoryDto.ProductInventoryDetailResponse();
        r.setProduct(toProductRow(s, product, units, threshold(lowStockThreshold)));
        r.setImageCount(imageRepository.findByProduct_Id(productId).size());
        r.setStockMatrix(buildMatrix(s, product, units));
        r.setUnits(units.stream().map(i -> toUnitRow(s, i)).sorted(unitOrder()).toList());

        List<InventoryDto.BlockRow> active = new ArrayList<>();
        for (InventoryEntity i : units) {
            for (InventoryBlockEntity b : s.blocks.getOrDefault(i.getId(), List.of())) {
                active.add(toBlockRow(b, product, i));
            }
        }
        active.sort(Comparator.comparing(InventoryDto.BlockRow::getStartDate));
        r.setActiveBlocks(active);
        return r;
    }

    // ═══════════════════════════════════════════
    //  UNITS
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public InventoryDto.PageResponse<InventoryDto.UnitRow> listUnits(
            UUID categoryId, UUID productId, String colour, String size, String condition,
            String state, String search, int page, int pageSize) {
        return paginate(filterUnits(load(), categoryId, productId, colour, size, condition, state, search),
                page, pageSize);
    }

    @Transactional(readOnly = true)
    public InventoryDto.UnitDetailResponse getUnit(UUID inventoryId) {
        Snapshot s = load();
        InventoryEntity unit = s.units.stream()
                .filter(i -> i.getId().equals(inventoryId)).findFirst()
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Inventory unit not found: " + inventoryId));

        ProductEntity product = s.products.get(unit.getProduct().getId());

        // full history: active + released, newest first
        List<InventoryDto.BlockRow> history = blockRepository.findAll().stream()
                .filter(b -> b.getInventory() != null && b.getInventory().getId().equals(inventoryId))
                .sorted(Comparator.comparing(InventoryBlockEntity::getStartDate).reversed())
                .map(b -> toBlockRow(b, product, unit))
                .toList();

        InventoryDto.UnitDetailResponse r = new InventoryDto.UnitDetailResponse();
        r.setUnit(toUnitRow(s, unit));
        r.setBlocks(history);
        return r;
    }

    // ═══════════════════════════════════════════
    //  UPCOMING BLOCKS
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<InventoryDto.BlockRow> upcomingBlocks(Integer days) {
        return buildUpcoming(load(), days(days));
    }

    // ═══════════════════════════════════════════
    //  EXPORT (CSV of units, same filters as listUnits)
    // ═══════════════════════════════════════════

    @Transactional
    public byte[] exportUnitsCsv(
            UUID categoryId, UUID productId, String colour, String size, String condition,
            String state, String search,
            UUID actorId, String actorName, String actorRole, String ip) {

        List<InventoryDto.UnitRow> rows =
                filterUnits(load(), categoryId, productId, colour, size, condition, state, search);

        StringBuilder sb = new StringBuilder();
        sb.append("SKU,Product,Category,Colour,Size,Condition,Status,State,Active,")
          .append("Current Block Type,Current Block Start,Current Block End,Next Block Start,Per Day Price\n");
        for (InventoryDto.UnitRow u : rows) {
            sb.append(csv(u.getSku())).append(',')
              .append(csv(u.getProductName())).append(',')
              .append(csv(u.getCategoryName())).append(',')
              .append(csv(u.getColour())).append(',')
              .append(csv(u.getSize())).append(',')
              .append(csv(u.getCondition())).append(',')
              .append(csv(u.getStatus())).append(',')
              .append(csv(u.getState())).append(',')
              .append(u.isActive()).append(',')
              .append(csv(u.getCurrentBlockType())).append(',')
              .append(csv(u.getCurrentBlockStart())).append(',')
              .append(csv(u.getCurrentBlockEnd())).append(',')
              .append(csv(u.getNextBlockStart())).append(',')
              .append(csv(u.getPerDayPrice())).append('\n');
        }

        superAdminService.writeAudit(actorId, actorName, actorRole, "EXPORT", MODULE,
                "units CSV (" + rows.size() + " rows)", null, Map.of("rows", rows.size()), ip);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ═══════════════════════════════════════════
    //  INTERNAL: SNAPSHOT
    // ═══════════════════════════════════════════

    /** Everything needed for one request, loaded once. */
    private static class Snapshot {
        LocalDate today;
        Map<UUID, ProductEntity> products;                 // non-deleted products only
        List<InventoryEntity> units;                       // units of those products
        Map<UUID, List<InventoryBlockEntity>> blocks;      // ACTIVE blocks by inventoryId

        InventoryBlockEntity current(UUID inventoryId) {
            return blocks.getOrDefault(inventoryId, List.of()).stream()
                    .filter(b -> !today.isBefore(b.getStartDate()) && !today.isAfter(b.getEndDate()))
                    .findFirst().orElse(null);
        }

        InventoryBlockEntity next(UUID inventoryId) {
            return blocks.getOrDefault(inventoryId, List.of()).stream()
                    .filter(b -> b.getStartDate().isAfter(today))
                    .min(Comparator.comparing(InventoryBlockEntity::getStartDate))
                    .orElse(null);
        }

        /** Single place that defines a unit's live state. Plug real bookings in here later. */
        String state(InventoryEntity i) {
            if (!i.isActive()) return "INACTIVE";
            String st = i.getStatus() == null ? "" : i.getStatus().trim().toUpperCase();
            if ("RENTED".equals(st)) return "RENTED";
            if ("DAMAGED".equals(st)) return "DAMAGED";
            if (current(i.getId()) != null) return "BLOCKED";
            if ("CLEANING".equals(st)) return "CLEANING";
            return "AVAILABLE";
        }
    }

    private Snapshot load() {
        Snapshot s = new Snapshot();
//        s.today = LocalDate.now();
        s.today = LocalINDTime.now().toLocalDate();

        s.products = new LinkedHashMap<>();
        for (ProductEntity p : productRepository.findAll()) {
            if (p.getDeletedAt() == null) s.products.put(p.getId(), p);
        }

        s.units = inventoryRepository.findAll().stream()
                .filter(i -> i.getProduct() != null && s.products.containsKey(i.getProduct().getId()))
                .toList();

        s.blocks = new HashMap<>();
        for (InventoryBlockEntity b : blockRepository.findAll()) {
            if (!b.isActive() || b.getInventory() == null) continue;
            s.blocks.computeIfAbsent(b.getInventory().getId(), k -> new ArrayList<>()).add(b);
        }
        return s;
    }

    /** Running counters for a group of units. */
    private static class Counts {
        int total, active, available, rented, blocked, cleaning, damaged, inactive;

        void add(String state) {
            total++;
            switch (state) {
                case "AVAILABLE" -> { active++; available++; }
                case "RENTED"    -> { active++; rented++; }
                case "BLOCKED"   -> { active++; blocked++; }
                case "CLEANING"  -> { active++; cleaning++; }
                case "DAMAGED"   -> { active++; damaged++; }
                default          -> inactive++;
            }
        }

        double utilization() {
            return active == 0 ? 0.0 : Math.round((rented + blocked) * 1000.0 / active) / 10.0;
        }

        int other() { return cleaning + damaged + inactive; }
    }

    // ═══════════════════════════════════════════
    //  INTERNAL: BUILDERS
    // ═══════════════════════════════════════════

    private InventoryDto.SummaryResponse buildSummary(Snapshot s, int th) {
        Counts c = new Counts();
        Map<String, Integer> byCondition = new TreeMap<>();
        for (InventoryEntity i : s.units) {
            c.add(s.state(i));
            byCondition.merge(upper(i.getCondition(), "UNSPECIFIED"), 1, Integer::sum);
        }

        List<InventoryDto.ProductStockRow> rows = buildProductRows(s, th);
        LocalDate in7 = s.today.plusDays(7);

        int upcoming = 0, endingToday = 0;
        for (List<InventoryBlockEntity> list : s.blocks.values()) {
            for (InventoryBlockEntity b : list) {
                if (b.getStartDate().isAfter(s.today) && !b.getStartDate().isAfter(in7)) upcoming++;
                if (b.getEndDate().isEqual(s.today)) endingToday++;
            }
        }

        InventoryDto.SummaryResponse r = new InventoryDto.SummaryResponse();
//        r.setGeneratedAt(LocalDateTime.now());
        r.setGeneratedAt(LocalINDTime.now());
        r.setTotalProducts(s.products.size());
        r.setActiveProducts((int) s.products.values().stream().filter(ProductEntity::isActive).count());
        r.setVisibleProducts((int) s.products.values().stream().filter(ProductEntity::isVisible).count());
        r.setFeaturedProducts((int) s.products.values().stream().filter(ProductEntity::isFeatured).count());
        r.setNewArrivalProducts((int) s.products.values().stream().filter(ProductEntity::isNewArrival).count());
        r.setTotalUnits(c.total);
        r.setActiveUnits(c.active);
        r.setAvailableUnits(c.available);
        r.setRentedUnits(c.rented);
        r.setBlockedUnits(c.blocked);
        r.setCleaningUnits(c.cleaning);
        r.setDamagedUnits(c.damaged);
        r.setInactiveUnits(c.inactive);
        r.setOutOfStockProducts((int) rows.stream()
                .filter(p -> p.isActive() && "OUT_OF_STOCK".equals(p.getStockStatus())).count());
        r.setLowStockProducts((int) rows.stream()
                .filter(p -> p.isActive() && "LOW_STOCK".equals(p.getStockStatus())).count());
        r.setLowStockThreshold(th);
        r.setUpcomingBlocks7d(upcoming);
        r.setBlocksEndingToday(endingToday);
        r.setUtilizationPercent(c.utilization());
        r.setUnitsByCondition(byCondition);
        return r;
    }

    private List<InventoryDto.BreakdownItem> buildBreakdown(Snapshot s, String by) {
        Map<String, Counts> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (InventoryEntity i : s.units) {
            ProductEntity p = s.products.get(i.getProduct().getId());
            String key = switch (by) {
                case "category"  -> p.getCategory() != null ? p.getCategory().getName() : "Uncategorised";
                case "colour"    -> colourOf(i, p);
                case "size"      -> clean(i.getSize()) == null ? "UNSPECIFIED" : i.getSize().trim();
                case "condition" -> upper(i.getCondition(), "UNSPECIFIED");
                default          -> s.state(i);
            };
            groups.computeIfAbsent(key, k -> new Counts()).add(s.state(i));
        }

        List<InventoryDto.BreakdownItem> out = new ArrayList<>();
        for (Map.Entry<String, Counts> e : groups.entrySet()) {
            Counts c = e.getValue();
            InventoryDto.BreakdownItem b = new InventoryDto.BreakdownItem();
            b.setKey(e.getKey());
            b.setTotalUnits(c.total);
            b.setActiveUnits(c.active);
            b.setAvailableUnits(c.available);
            b.setRentedUnits(c.rented);
            b.setBlockedUnits(c.blocked);
            b.setOtherUnits(c.other());
            b.setUtilizationPercent(c.utilization());
            out.add(b);
        }
        out.sort(Comparator.comparingInt(InventoryDto.BreakdownItem::getTotalUnits).reversed());
        return out;
    }

    private List<InventoryDto.ProductStockRow> buildProductRows(Snapshot s, int th) {
        Map<UUID, List<InventoryEntity>> byProduct = s.units.stream()
                .collect(Collectors.groupingBy(i -> i.getProduct().getId()));

        List<InventoryDto.ProductStockRow> rows = new ArrayList<>();
        for (ProductEntity p : s.products.values()) {
            rows.add(toProductRow(s, p, byProduct.getOrDefault(p.getId(), List.of()), th));
        }
        return rows;
    }

    private InventoryDto.ProductStockRow toProductRow(
            Snapshot s, ProductEntity p, List<InventoryEntity> units, int th) {

        Counts c = new Counts();
        Map<String, String> colours = new LinkedHashMap<>();
        for (InventoryEntity i : units) {
            c.add(s.state(i));
            if (i.isActive()) colours.putIfAbsent(colourOf(i, p).toLowerCase(), colourOf(i, p));
        }

        InventoryDto.ProductStockRow r = new InventoryDto.ProductStockRow();
        r.setProductId(p.getId());
        r.setName(p.getName());
        r.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        r.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);
        r.setColours(new ArrayList<>(colours.values()));
        r.setTotalUnits(c.total);
        r.setActiveUnits(c.active);
        r.setAvailableUnits(c.available);
        r.setRentedUnits(c.rented);
        r.setBlockedUnits(c.blocked);
        r.setCleaningUnits(c.cleaning);
        r.setDamagedUnits(c.damaged);
        r.setInactiveUnits(c.inactive);
        r.setUtilizationPercent(c.utilization());
        r.setStockStatus(c.available == 0 ? "OUT_OF_STOCK" : c.available <= th ? "LOW_STOCK" : "IN_STOCK");
        r.setPerDayPrice(p.getPerDayPrice());
        r.setWeekendPrice(p.getWeekendPrice());
        r.setOfferPrice(p.getOfferPrice());
        r.setSecurityDeposit(p.getSecurityDeposit());
        r.setActive(p.isActive());
        r.setVisible(p.isVisible());
        r.setFeatured(p.isFeatured());
        r.setNewArrival(p.isNewArrival());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    private List<InventoryDto.ColourSizeStock> buildMatrix(
            Snapshot s, ProductEntity p, List<InventoryEntity> units) {

        Map<String, InventoryDto.ColourSizeStock> map = new LinkedHashMap<>();
        Map<String, Counts> counts = new HashMap<>();
        for (InventoryEntity i : units) {
            String colour = colourOf(i, p);
            String size = clean(i.getSize()) == null ? "UNSPECIFIED" : i.getSize().trim();
            String key = colour.toLowerCase() + "|" + size.toLowerCase();
            map.computeIfAbsent(key, k -> {
                InventoryDto.ColourSizeStock g = new InventoryDto.ColourSizeStock();
                g.setColour(colour);
                g.setSize(size);
                return g;
            });
            counts.computeIfAbsent(key, k -> new Counts()).add(s.state(i));
        }
        for (Map.Entry<String, InventoryDto.ColourSizeStock> e : map.entrySet()) {
            Counts c = counts.get(e.getKey());
            InventoryDto.ColourSizeStock g = e.getValue();
            g.setTotalUnits(c.total);
            g.setAvailableUnits(c.available);
            g.setRentedUnits(c.rented);
            g.setBlockedUnits(c.blocked);
            g.setOtherUnits(c.other());
        }
        return new ArrayList<>(map.values());
    }

    private List<InventoryDto.UnitRow> filterUnits(
            Snapshot s, UUID categoryId, UUID productId, String colour, String size,
            String condition, String state, String search) {

        String st = null;
        if (state != null && !state.isBlank()) {
            st = state.trim().toUpperCase();
            if (!UNIT_STATES.contains(st)) {
                throw new SuperAdminService.BadRequestException(
                        "Invalid state. Must be one of: " + UNIT_STATES);
            }
        }
        final String stateFilter = st;
        final String colourQ = clean(colour) == null ? null : colour.trim().toLowerCase();
        final String sizeQ = clean(size) == null ? null : size.trim().toLowerCase();
        final String condQ = clean(condition) == null ? null : condition.trim().toLowerCase();
        final String q = clean(search) == null ? null : search.trim().toLowerCase();

        return s.units.stream()
                .filter(i -> productId == null || i.getProduct().getId().equals(productId))
                .filter(i -> {
                    if (categoryId == null) return true;
                    ProductEntity p = s.products.get(i.getProduct().getId());
                    return p.getCategory() != null && categoryId.equals(p.getCategory().getId());
                })
                .filter(i -> colourQ == null
                        || colourOf(i, s.products.get(i.getProduct().getId())).equalsIgnoreCase(colourQ))
                .filter(i -> sizeQ == null || (i.getSize() != null && i.getSize().trim().equalsIgnoreCase(sizeQ)))
                .filter(i -> condQ == null || (i.getCondition() != null && i.getCondition().trim().equalsIgnoreCase(condQ)))
                .filter(i -> stateFilter == null || stateFilter.equals(s.state(i)))
                .filter(i -> {
                    if (q == null) return true;
                    ProductEntity p = s.products.get(i.getProduct().getId());
                    return (i.getSku() != null && i.getSku().toLowerCase().contains(q))
                            || (p.getName() != null && p.getName().toLowerCase().contains(q));
                })
                .map(i -> toUnitRow(s, i))
                .sorted(unitOrder())
                .toList();
    }

    private List<InventoryDto.BlockRow> buildUpcoming(Snapshot s, int days) {
        LocalDate until = s.today.plusDays(days);
        Map<UUID, InventoryEntity> unitById = s.units.stream()
                .collect(Collectors.toMap(InventoryEntity::getId, i -> i, (a, b) -> a));

        List<InventoryDto.BlockRow> out = new ArrayList<>();
        for (List<InventoryBlockEntity> list : s.blocks.values()) {
            for (InventoryBlockEntity b : list) {
                if (b.getStartDate().isAfter(s.today) && !b.getStartDate().isAfter(until)) {
                    InventoryEntity unit = unitById.get(b.getInventory().getId());
                    if (unit == null) continue;
                    out.add(toBlockRow(b, s.products.get(unit.getProduct().getId()), unit));
                }
            }
        }
        out.sort(Comparator.comparing(InventoryDto.BlockRow::getStartDate));
        return out;
    }

    // ═══════════════════════════════════════════
    //  INTERNAL: MAPPERS
    // ═══════════════════════════════════════════

    private InventoryDto.UnitRow toUnitRow(Snapshot s, InventoryEntity i) {
        ProductEntity p = s.products.get(i.getProduct().getId());
        InventoryBlockEntity cur = s.current(i.getId());
        InventoryBlockEntity nxt = s.next(i.getId());

        InventoryDto.UnitRow r = new InventoryDto.UnitRow();
        r.setInventoryId(i.getId());
        r.setSku(i.getSku());
        r.setProductId(p.getId());
        r.setProductName(p.getName());
        r.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        r.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);
        r.setColour(colourOf(i, p));
        r.setSize(i.getSize());
        r.setCondition(i.getCondition());
        r.setStatus(i.getStatus());
        r.setState(s.state(i));
        r.setActive(i.isActive());
        r.setPerDayPrice(p.getPerDayPrice());
        if (cur != null) {
            r.setCurrentBlockId(cur.getId());
            r.setCurrentBlockType(cur.getBlockType());
            r.setCurrentBlockStart(cur.getStartDate());
            r.setCurrentBlockEnd(cur.getEndDate());
        }
        if (nxt != null) {
            r.setNextBlockStart(nxt.getStartDate());
            r.setNextBlockEnd(nxt.getEndDate());
        }
        return r;
    }

    private InventoryDto.BlockRow toBlockRow(InventoryBlockEntity b, ProductEntity p, InventoryEntity i) {
        InventoryDto.BlockRow r = new InventoryDto.BlockRow();
        r.setId(b.getId());
        r.setInventoryId(i.getId());
        r.setSku(i.getSku());
        r.setProductId(p.getId());
        r.setProductName(p.getName());
        r.setColour(colourOf(i, p));
        r.setSize(i.getSize());
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

    // ═══════════════════════════════════════════
    //  INTERNAL: SMALL HELPERS
    // ═══════════════════════════════════════════

    private Comparator<InventoryDto.UnitRow> unitOrder() {
        return Comparator
                .comparing((InventoryDto.UnitRow u) -> u.getProductName() == null ? "" : u.getProductName().toLowerCase())
                .thenComparing(u -> u.getSku() == null ? "" : u.getSku());
    }

    private <T> InventoryDto.PageResponse<T> paginate(List<T> all, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        int totalPages = (int) Math.ceil(all.size() / (double) safeSize);

        InventoryDto.PageResponse<T> r = new InventoryDto.PageResponse<>();
        r.setContent(new ArrayList<>(all.subList(from, to)));
        r.setPage(safePage);
        r.setSize(safeSize);
        r.setTotalElements(all.size());
        r.setTotalPages(totalPages);
        r.setHasNext(safePage + 1 < totalPages);
        r.setHasPrevious(safePage > 0);
        return r;
    }

    private int threshold(Integer v) { return v == null || v < 0 ? DEFAULT_LOW_STOCK : v; }

    private int days(Integer v) { return v == null || v < 1 ? 7 : Math.min(v, 365); }

    private static String clean(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }

    private static String upper(String s, String fallback) {
        String c = clean(s);
        return c == null ? fallback : c.toUpperCase();
    }

    /** Colour of a unit; falls back to the product-level colour, then UNSPECIFIED. */
    private static String colourOf(InventoryEntity i, ProductEntity p) {
        String c = clean(i.getColour());
        if (c == null) c = clean(p.getColour());
        return c == null ? "UNSPECIFIED" : c;
    }

    private static String csv(Object o) {
        if (o == null) return "";
        String v = o.toString();
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
