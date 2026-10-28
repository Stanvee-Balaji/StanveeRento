package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.AuditLogDto;
import com.example.demo.dto.PaymentDtos.PaymentRequest;
import com.example.demo.dto.RentalOrderDto.*;
import com.example.demo.dto.SubscriptionPurchaseDto;
import com.example.demo.entity.*;
import com.example.demo.entity.RentalOrderEntity.*;
import com.example.demo.repository.*;
import com.example.demo.util.LocalINDTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Rental order placing (subscription-based).
 *
 * MONEY RULES
 *   - Subscribers' rental value is covered by the plan (config.subscriptionCoversRental).
 *   - Only the product security deposit and the delivery fee are ever payable.
 *   - total payable == 0  -> order is CONFIRMED immediately (no Airpay).
 *   - total payable  > 0  -> order is PAYMENT_PENDING (units held) and becomes CONFIRMED only through a
 *     VALID-hash Airpay IPN whose amount equals the stored total (or an explicit admin confirm).
 *   - There is NO payment retry: a failed / cancelled / expired payment ends the order, the customer books again.
 *
 * SAFETY
 *   - Every transition runs under a row lock and checks the current status first -> duplicate IPNs are no-ops.
 *   - Booking locks the product rows (ordered by id) before allocating units, and the DB has an exclusion
 *     constraint on rental_order_units as the last line of defence -> no overlapping bookings.
 *   - CONFIRMED / fulfilled orders are never downgraded by late FAILED / CANCELLED callbacks.
 *   - A SUCCESS callback for an order that is no longer PENDING moves it to NEEDS_REVIEW (money received, never silently lost).
 *
 * Actor-agnostic admin methods (same shape as SubscriptionPurchaseService): Super Admin and Sub-Admin controllers
 * both call them with actorId / actorName / actorRole / ip.
 */
@Service
public class RentalOrderService {

    private static final Logger log = LoggerFactory.getLogger(RentalOrderService.class);

    /** Airpay order ids: 20 chars max, alphanumeric. Subscriptions use "SUB"; AirpayService routes callbacks on this prefix. */
    public static final String ORDER_PREFIX = "ORD";
    private static final String MODULE = "RENTAL_ORDER_MANAGEMENT";
    // ADAPT: copy the customvar / txnsubtype values UserSubscriptionService uses for its PaymentRequest
    private static final String CUSTOMVAR = "RENTAL_ORDER";
    private static final String TXN_SUBTYPE = "2";

    private static final Set<OrderStatus> CONFIRMABLE = EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.NEEDS_REVIEW);
    private static final Set<OrderStatus> OPEN_FOR_CANCEL = EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CONFIRMED, OrderStatus.NEEDS_REVIEW);
    private static final int MAX_AVAILABILITY_DAYS = 120;

    private final RentalOrderRepository orderRepo;
    private final RentalOrderUnitRepository unitRepo;
    private final RentalOrderEventRepository eventRepo;
    private final RentalBookingConfigRepository configRepo;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBlockRepository blockRepository;
    private final ProductImageRepository imageRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subRepo;
    private final AirpayIpnLogRepository ipnLogRepository;
    private final RentalCalendarService calendarService;
    private final TransactionService transactionService;
    private final SuperAdminService superAdminService; // reused only for writeAudit(...)
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    public RentalOrderService(RentalOrderRepository orderRepo, RentalOrderUnitRepository unitRepo,
                              RentalOrderEventRepository eventRepo, RentalBookingConfigRepository configRepo,
                              ProductRepository productRepository, InventoryRepository inventoryRepository,
                              InventoryBlockRepository blockRepository, ProductImageRepository imageRepository,
                              SubscriptionPlanRepository planRepository, UserSubscriptionRepository subRepo,
                              AirpayIpnLogRepository ipnLogRepository, RentalCalendarService calendarService,
                              TransactionService transactionService, SuperAdminService superAdminService,
                              JdbcTemplate jdbc) {
        this.orderRepo = orderRepo;
        this.unitRepo = unitRepo;
        this.eventRepo = eventRepo;
        this.configRepo = configRepo;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.blockRepository = blockRepository;
        this.imageRepository = imageRepository;
        this.planRepository = planRepository;
        this.subRepo = subRepo;
        this.ipnLogRepository = ipnLogRepository;
        this.calendarService = calendarService;
        this.transactionService = transactionService;
        this.superAdminService = superAdminService;
        this.jdbc = jdbc;
    }

    // ═══════════════════════════════════════════
    //  CONFIG
    // ═══════════════════════════════════════════

    @Transactional
    public RentalBookingConfigEntity config() {
        return configRepo.findFirstByActiveTrueOrderByCreatedAtDesc().orElseGet(() -> {
            RentalBookingConfigEntity c = new RentalBookingConfigEntity();
            c.setActive(true);
            c.setDeliveryEnabled(true);
            c.setDeliveryFee(new BigDecimal("99.00"));
            c.setFreeDeliveryAbove(null);
            c.setWaiveDeliveryForSubscribers(false);
            c.setSubscriptionCoversRental(true);
            c.setPendingHoldMinutes(20);
            c.setCancellationCutoffDays(1);
            c.setMaxItemsPerOrder(10);
            return configRepo.save(c);
        });
    }

    @Transactional
    public ConfigResponse getConfig() { return toConfigResponse(config()); }

    @Transactional
    public ConfigResponse upsertConfig(ConfigRequest r, UUID actorId, String actorName, String actorRole, String ip) {
        RentalBookingConfigEntity c = config();
        Map<String, Object> before = configState(c);
        c.setDeliveryEnabled(r.deliveryEnabled());
        c.setDeliveryFee(money(r.deliveryFee()));
        c.setFreeDeliveryAbove(r.freeDeliveryAbove() == null ? null : money(r.freeDeliveryAbove()));
        c.setWaiveDeliveryForSubscribers(r.waiveDeliveryForSubscribers());
        c.setSubscriptionCoversRental(r.subscriptionCoversRental());
        c.setPendingHoldMinutes(r.pendingHoldMinutes());
        c.setCancellationCutoffDays(r.cancellationCutoffDays());
        c.setMaxItemsPerOrder(r.maxItemsPerOrder());
        c.setUpdatedBy(actorId);
        c = configRepo.save(c);
        superAdminService.writeAudit(actorId, actorName, actorRole, "UPDATE_CONFIG", MODULE, "BOOKING_CONFIG",
                before, configState(c), ip);
        return toConfigResponse(c);
    }

    // ═══════════════════════════════════════════
    //  USER: AVAILABILITY / SUMMARY / BOOK
    // ═══════════════════════════════════════════

    /** Day-by-day free units of one size (orders, blocks and cleaning buffers considered). */
    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID productId, String size, String colour, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from))
            throw new SuperAdminService.BadRequestException("'to' must be on/after 'from'");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_AVAILABILITY_DAYS)
            throw new SuperAdminService.BadRequestException("Range too large (max " + MAX_AVAILABILITY_DAYS + " days)");
        if (size == null || size.isBlank()) throw new SuperAdminService.BadRequestException("size is required");

        ProductEntity p = productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(x -> x.isActive() && x.isVisible())
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));

        List<InventoryEntity> units = matchingUnits(p, size, colour);
        List<RentalOrderUnitEntity> held = unitRepo.findActiveOverlapping(productId, from, to);
        List<InventoryBlockEntity> blocks = blockRepository
                .findByProduct_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(productId, to, from);

        List<DayAvailability> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            int free = (int) units.stream().filter(u ->
                    held.stream().noneMatch(h -> h.getInventoryId().equals(u.getId())
                            && !day.isBefore(h.getStartDate()) && !day.isAfter(h.getBlockedUntil()))
                    && blocks.stream().noneMatch(b -> b.getInventory().getId().equals(u.getId())
                            && !day.isBefore(b.getStartDate()) && !day.isAfter(b.getEndDate()))).count();
            days.add(new DayAvailability(day, units.size(), free));
        }
        return new AvailabilityResponse(p.getId(), p.getName(), size.trim(), clean(colour), from, to, days);
    }

    /** Priced breakdown + every eligibility check. Creates nothing, reserves nothing. */
    @Transactional
    public SummaryResponse summary(String username, OrderRequest req) {
        Quote q = buildQuote(username, req, false);
        List<ItemView> items = q.items.stream().map(c -> c.toView(true)).toList();
        PriceBreakdown b = new PriceBreakdown(q.currency, q.rental, q.discount, q.rentalPayable, q.deposit,
                q.delivery, q.deliveryNote, q.total, q.total.signum() > 0);
        return new SummaryResponse(q.violations.isEmpty() && q.address != null, q.total.signum() > 0,
                q.violations, q.sub, items, b, q.address);
    }

    /**
     * Creates the order and holds the units. Payment (when needed) is started by UserRentalService AFTER this
     * transaction commits, so no DB locks are held during the Airpay HTTP call.
     */
    @Transactional
    public BookingResult createBooking(String username, OrderRequest req) {
        Quote q = buildQuote(username, req, true);
        if (!q.violations.isEmpty())
            throw new SuperAdminService.BadRequestException(String.join(" | ", q.violations));

        LocalDateTime now = LocalINDTime.now();
        boolean payReq = q.total.signum() > 0;

        String email = null;
        if (payReq) {
            email = clean(req.buyerEmail());
            if (email == null) {
                List<String> found = jdbc.query("select email from stanvee_rento_user where username = ?",
                        (rs, i) -> rs.getString(1), username);
                email = found.isEmpty() ? null : clean(found.get(0));
            }
            if (email == null)
                throw new SuperAdminService.BadRequestException("An email address is required for payment. Send buyerEmail.");
        }

        // Serialize concurrent bookings of the same products (ordered by id -> no deadlocks).
        Set<UUID> productIds = new TreeSet<>();
        for (ItemCalc c : q.items) productIds.add(c.product.getId());
        for (UUID pid : productIds) em.find(ProductEntity.class, pid, LockModeType.PESSIMISTIC_WRITE);

        RentalOrderEntity o = new RentalOrderEntity();
        o.setOrderId(newOrderId(now));
        o.setUsername(username);
//        o.setSubscriptionId(q.running.getId());
//        o.setSubscriptionOrderId(q.running.getOrderId());
//        o.setPlanId(q.running.getPlanId());
//        o.setPlanName(q.running.getPlanName());
//        o.setSubscriptionExpiry(q.sub.effectiveExpiry());
        if (q.running != null && q.sub != null) {
            o.setSubscriptionId(q.running.getId());
            o.setSubscriptionOrderId(q.running.getOrderId());
            o.setPlanId(q.running.getPlanId());
            o.setPlanName(q.running.getPlanName());
            o.setSubscriptionExpiry(q.sub.effectiveExpiry());
        }
        o.setStatus(payReq ? OrderStatus.PAYMENT_PENDING : OrderStatus.CONFIRMED);
        o.setPaymentStatus(payReq ? PaymentStatus.PENDING : PaymentStatus.NOT_REQUIRED);
        o.setFulfilmentStatus(FulfilmentStatus.NOT_STARTED);
        o.setDepositStatus(q.deposit.signum() > 0 ? DepositStatus.PENDING : DepositStatus.NOT_APPLICABLE);
        o.setCurrency(q.currency);
        o.setRentalValue(q.rental);
        o.setSubscriptionDiscount(q.discount);
        o.setRentalPayable(q.rentalPayable);
        o.setDepositAmount(q.deposit);
        o.setDeliveryFee(q.delivery);
        o.setTotalPayable(q.total);
        o.setSubscriptionCoversRental(q.covers);
        o.setNotes(clean(req.notes()));
        o.setPricingSnapshot(json(pricingSnapshot(q)));
        o.setStartDate(q.items.stream().map(c -> c.req.startDate()).min(Comparator.naturalOrder()).orElseThrow());
        o.setEndDate(q.items.stream().map(c -> c.req.endDate()).max(Comparator.naturalOrder()).orElseThrow());
        AddressView a = q.address;
        o.setAddressId(a.id());
        o.setShipFullName(a.fullName());
        o.setShipPhone(a.phone());
        o.setShipLine1(a.line1());
        o.setShipLine2(a.line2());
        o.setShipLandmark(a.landmark());
        o.setShipCity(a.city());
        o.setShipState(a.state());
        o.setShipPincode(a.pincode());
        o.setShipCountry(a.country());
        o.setBuyerEmail(email);
        o.setBuyerPhone(a.phone());
        if (payReq) o.setExpiresAt(now.plusMinutes(q.cfg.getPendingHoldMinutes()));
        else {
            o.setPaidAt(null);
            o.setConfirmedAt(now);
//            o.setActivatedBy("SUBSCRIPTION");
//            o.setStatusReason("Covered by your subscription - no payment required");
            o.setActivatedBy(q.sub != null ? "SUBSCRIPTION" : "SYSTEM");
            o.setStatusReason(q.sub != null ? "Covered by your subscription - no payment required" : "No payment required");
        }

        List<RentalOrderItemEntity> savedItems = new ArrayList<>();
        for (ItemCalc c : q.items) {
            RentalOrderItemEntity it = new RentalOrderItemEntity();
            it.setOrder(o);
            it.setProductId(c.product.getId());
            it.setProductName(c.product.getName());
            it.setCoverImageUrl(c.cover);
            it.setSize(c.req.size().trim());
            it.setColour(clean(c.req.colour()));
            it.setQuantity(c.req.quantity());
            it.setStartDate(c.req.startDate());
            it.setEndDate(c.req.endDate());
            it.setRentalDays(c.days);
            it.setWeekdayCount(c.weekdays);
            it.setWeekendCount(c.weekends);
            it.setDayRate(c.dayRate);
            it.setWeekendRate(c.weekendRate);
            it.setOfferApplied(c.offer);
            it.setRentalValue(c.rentalValue);
            it.setSubscriptionDiscount(c.discount);
            it.setRentalPayable(c.payable);
            it.setDepositPerUnit(c.depositPer);
            it.setDepositTotal(c.depositTotal);
            o.getItems().add(it);
            savedItems.add(it);
        }
        o = orderRepo.saveAndFlush(o);

        // Authoritative availability check + allocation (we hold the product locks now).
        try {
            for (int i = 0; i < q.items.size(); i++) {
                ItemCalc c = q.items.get(i);
                RentalOrderItemEntity it = savedItems.get(i);
                List<InventoryEntity> free = freeUnits(c.product, c.req.size(), c.req.colour(), c.req.startDate(), c.blockedUntil);
                if (free.size() < c.req.quantity())
                    throw new SuperAdminService.BadRequestException(c.product.getName() + ": only " + free.size()
                            + " unit(s) left for these dates - someone else just booked. Please try again.");
                for (InventoryEntity inv : free.subList(0, c.req.quantity())) {
                    RentalOrderUnitEntity u = new RentalOrderUnitEntity();
                    u.setOrderPk(o.getId());
                    u.setItemId(it.getId());
                    u.setOrderId(o.getOrderId());
                    u.setProductId(c.product.getId());
                    u.setInventoryId(inv.getId());
                    u.setSku(inv.getSku());
                    u.setSize(inv.getSize());
                    u.setColour(inv.getColour());
                    u.setStartDate(c.req.startDate());
                    u.setEndDate(c.req.endDate());
                    u.setBlockedUntil(c.blockedUntil);
                    u.setActive(true);
                    unitRepo.saveAndFlush(u);
                }
            }
        } catch (DataIntegrityViolationException e) {
            throw new SuperAdminService.BadRequestException("Some units were just booked by someone else. Please try again.");
        }

        event(o, "ORDER_CREATED", null, o.getStatus().name(), "USER:" + username, "USER",
                payReq ? "Booking created - payment of " + q.total + " " + q.currency + " pending" : "Booking confirmed - covered by subscription", true);

        PaymentRequest pr = payReq ? buildPaymentRequest(o) : null;
        return new BookingResult(toResponse(o), payReq, pr);
    }

    /** Airpay could not even be reached (token / init failure). Only touches PAYMENT_PENDING orders. */
    @Transactional
    public void markInitFailed(String orderId, String reason) {
        orderRepo.findByOrderIdForUpdate(orderId).ifPresent(o -> {
            if (o.getStatus() != OrderStatus.PAYMENT_PENDING) return;
            Map<String, Object> before = state(o);
            o.setStatus(OrderStatus.PAYMENT_FAILED);
            o.setPaymentStatus(PaymentStatus.FAILED);
            o.setExpiresAt(null);
            o.setStatusReason(truncate("Payment could not be started: " + reason, 500));
            orderRepo.save(o);
            releaseUnits(o, "Payment init failed");
            event(o, "PAYMENT_INIT_FAILED", before.get("status").toString(), o.getStatus().name(), "SYSTEM", "SYSTEM",
                    "Payment could not be started", true);
        });
    }

    // ═══════════════════════════════════════════
    //  PAYMENT-DRIVEN LIFECYCLE
    // ═══════════════════════════════════════════

    /**
     * Single entry point for Airpay results (AirpayService.resolveIpn routes here when orderId starts with "ORD"). Idempotent.
     *
     * @param hashStatus VALID | MISMATCH | NOT_PRESENT
     * @param ipnAmount  amount reported by Airpay (covered by the secure hash)
     */
    @Transactional
    public void onPaymentResolved(String orderId, TransactionStatus resolved, String hashStatus,
                                  String ipnAmount, String message) {
        Optional<RentalOrderEntity> opt = orderRepo.findByOrderIdForUpdate(orderId);
        if (opt.isEmpty()) {
            log.error("orderId={} payment result but no rental order exists", orderId);
            return;
        }
        RentalOrderEntity o = opt.get();
        Map<String, Object> before = state(o);
        String from = o.getStatus().name();

        if (resolved == TransactionStatus.SUCCESS) {
            if (o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.NEEDS_REVIEW) {
                event(o, "DUPLICATE_SUCCESS_IGNORED", from, from, "SYSTEM", "SYSTEM", "Duplicate success IPN ignored", false);
                return;
            }
            if (o.getStatus() != OrderStatus.PAYMENT_PENDING) {
                // FAILED / CANCELLED order, units already released, but the customer's money arrived.
                o.setPaymentStatus(PaymentStatus.PAID);
                o.setPaidAt(LocalINDTime.now());
                moveToReview(o, from, "Payment received after the order was already " + from + " - refund or manual review needed");
                return;
            }
            if (!"VALID".equals(hashStatus)) {
                moveToReview(o, from, "Airpay reported success but secure hash status was " + hashStatus);
                return;
            }
            if (!amountMatches(o, ipnAmount)) {
                moveToReview(o, from, "Paid amount " + ipnAmount + " does not match the expected amount " + o.getTotalPayable());
                return;
            }
            markConfirmed(o, "AUTO", "Payment confirmed by Airpay IPN");
            orderRepo.save(o);
            event(o, "PAYMENT_SUCCESS", from, o.getStatus().name(), "SYSTEM", "SYSTEM", "Payment received - booking confirmed", true);
            return;
        }

        // non-success results may only affect an order that is still waiting
        if (o.getStatus() != OrderStatus.PAYMENT_PENDING) {
            event(o, "LATE_RESULT_IGNORED", from, from, "SYSTEM", "SYSTEM", resolved + " IPN ignored (order already " + from + ")", false);
            return;
        }
        if (resolved == TransactionStatus.HASH_MISMATCH) {
            moveToReview(o, from, "IPN secure hash mismatch");
            return;
        }
        boolean cancelled = resolved == TransactionStatus.CANCELLED;
        o.setStatus(cancelled ? OrderStatus.CANCELLED : OrderStatus.PAYMENT_FAILED);
        o.setPaymentStatus(PaymentStatus.FAILED);
        o.setExpiresAt(null);
        if (cancelled) { o.setCancelledAt(LocalINDTime.now()); o.setCancelledBy("PAYMENT_GATEWAY"); }
        o.setStatusReason(truncate(message == null ? resolved.name() : message, 500));
        orderRepo.save(o);
        releaseUnits(o, "Payment " + resolved);
        event(o, cancelled ? "PAYMENT_CANCELLED" : "PAYMENT_FAILED", from, o.getStatus().name(), "SYSTEM", "SYSTEM",
                cancelled ? "Payment was cancelled" : "Payment failed", true);
    }

    /** PAYMENT_PENDING orders whose hold window passed become PAYMENT_FAILED and free their units. */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Kolkata")
    @Transactional
    public void expireStalePending() {
        List<RentalOrderEntity> stale = orderRepo.findByStatusAndExpiresAtBefore(OrderStatus.PAYMENT_PENDING, LocalINDTime.now());
        int n = 0;
        for (RentalOrderEntity s : stale) {
            RentalOrderEntity o = orderRepo.findByOrderIdForUpdate(s.getOrderId()).orElse(null);
            if (o == null || o.getStatus() != OrderStatus.PAYMENT_PENDING) continue;
            o.setStatus(OrderStatus.PAYMENT_FAILED);
            o.setPaymentStatus(PaymentStatus.FAILED);
            o.setExpiresAt(null);
            o.setStatusReason("Payment window expired");
            orderRepo.save(o);
            releaseUnits(o, "Payment window expired");
            event(o, "PAYMENT_EXPIRED", "PAYMENT_PENDING", o.getStatus().name(), "SYSTEM", "SYSTEM", "Payment window expired", true);
            n++;
        }
        if (n > 0) log.info("Expired {} unpaid rental order(s)", n);
    }

    // ═══════════════════════════════════════════
    //  USER: READ / CANCEL
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResult<OrderResponse> myList(String username, String status, int page, int size) {
        OrderStatus st = parseStatus(status);
        Specification<RentalOrderEntity> spec = (r, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(r.get("username"), username));
            if (st != null) p.add(cb.equal(r.get("status"), st));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return page(spec, page, size);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse myOrder(String username, String orderId) {
        RentalOrderEntity o = find(orderId);
        if (!o.getUsername().equals(username))
            throw new SuperAdminService.ResourceNotFoundException("Order not found: " + orderId);
        List<EventView> tl = eventRepo.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .filter(RentalOrderEventEntity::isUserVisible)
                .map(e -> new EventView(e.getEventType(), e.getFromStatus(), e.getToStatus(), null, null, e.getMessage(), e.getCreatedAt()))
                .toList();
        return new OrderDetailResponse(toResponse(o), tl);
    }

    /** Customer cancel: unpaid PAYMENT_PENDING, or CONFIRMED before dispatch and before the cutoff. */
    @Transactional
    public OrderResponse cancelMine(String username, String orderId, String reason) {
        RentalOrderEntity o = lock(orderId);
        if (!o.getUsername().equals(username))
            throw new SuperAdminService.ResourceNotFoundException("Order not found: " + orderId);
        if (o.getStatus() == OrderStatus.CONFIRMED) {
            int cutoff = config().getCancellationCutoffDays();
            LocalDate today = LocalINDTime.now().toLocalDate();
            if (o.getFulfilmentStatus() != FulfilmentStatus.NOT_STARTED || o.getStartDate().isBefore(today.plusDays(cutoff)))
                throw new SuperAdminService.BadRequestException(
                        "This order can no longer be cancelled online (cancel at least " + cutoff + " day(s) before the start date and before dispatch).");
        } else if (o.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new SuperAdminService.BadRequestException("Order in status " + o.getStatus() + " cannot be cancelled.");
        }
        Map<String, Object> before = state(o);
        cancelCommon(o, "USER:" + username, reason == null || reason.isBlank() ? "Cancelled by customer" : reason);
        event(o, "ORDER_CANCELLED", before.get("status").toString(), o.getStatus().name(), "USER:" + username, "USER", o.getStatusReason(), true);
        return toResponse(o);
    }

    /** Customer says the items are ready to go back. Admin still marks RETURNED after checking them. */
    @Transactional
    public OrderResponse requestReturn(String username, String orderId, String note) {
        RentalOrderEntity o = lock(orderId);
        if (!o.getUsername().equals(username))
            throw new SuperAdminService.ResourceNotFoundException("Order not found: " + orderId);
        if (o.getStatus() != OrderStatus.CONFIRMED)
            throw new SuperAdminService.BadRequestException("Only confirmed orders can be returned (current: " + o.getStatus() + ").");
        if (o.getFulfilmentStatus() == FulfilmentStatus.RETURNED || o.getFulfilmentStatus() == FulfilmentStatus.COMPLETED)
            throw new SuperAdminService.BadRequestException("This order has already been returned.");
        if (o.getFulfilmentStatus() != FulfilmentStatus.DELIVERED)
            throw new SuperAdminService.BadRequestException("You can request a return only after the order is delivered (current: " + o.getFulfilmentStatus() + ").");
        boolean already = eventRepo.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .anyMatch(e -> "RETURN_REQUESTED".equals(e.getEventType()));
        if (already)
            throw new SuperAdminService.BadRequestException("A return has already been requested for this order.");
        String msg = "Return requested by customer" + (note == null || note.isBlank() ? "" : ": " + note.trim());
        String f = o.getFulfilmentStatus().name();
        event(o, "RETURN_REQUESTED", f, f, "USER:" + username, "USER", msg, true);
        return toResponse(o);
    }

    // ═══════════════════════════════════════════
    //  ADMIN: READ
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResult<OrderResponse> list(String status, String paymentStatus, String fulfilmentStatus, String username,
                                          String orderId, LocalDate from, LocalDate to, int page, int size) {
        OrderStatus st = parseStatus(status);
        PaymentStatus ps = parseEnum(PaymentStatus.class, paymentStatus, "paymentStatus");
        FulfilmentStatus fs = parseEnum(FulfilmentStatus.class, fulfilmentStatus, "fulfilmentStatus");
        Specification<RentalOrderEntity> spec = (r, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (st != null) p.add(cb.equal(r.get("status"), st));
            if (ps != null) p.add(cb.equal(r.get("paymentStatus"), ps));
            if (fs != null) p.add(cb.equal(r.get("fulfilmentStatus"), fs));
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(r.get("username")), "%" + username.trim().toLowerCase() + "%"));
            if (orderId != null && !orderId.isBlank()) p.add(cb.equal(r.get("orderId"), orderId.trim()));
            if (from != null) p.add(cb.greaterThanOrEqualTo(r.get("createdAt"), from.atStartOfDay()));
            if (to != null) p.add(cb.lessThan(r.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return page(spec, page, size);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getDetail(String orderId) {
        RentalOrderEntity o = find(orderId);
        var txn = transactionService.findByOrderId(orderId).map(transactionService::toDetailDto).orElse(null);
        List<SubscriptionPurchaseDto.IpnLogRow> ipn = ipnLogRepository.findByOrderIdOrderByReceivedAtAsc(orderId).stream()
                .map(l -> new SubscriptionPurchaseDto.IpnLogRow(l.getId(), l.getApTransactionId(), l.getAmount(),
                        l.getTransactionStatus(), l.getTransactionPaymentStatus(), l.getMessage(), l.getChmod(),
                        l.getSourceIp(), l.getReceivedAt()))
                .toList();
        List<EventView> events = eventRepo.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(e -> new EventView(e.getEventType(), e.getFromStatus(), e.getToStatus(), e.getActor(), e.getActorRole(), e.getMessage(), e.getCreatedAt()))
                .toList();
        return new AdminOrderDetailResponse(toResponse(o), txn, ipn, events);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditHistory(String orderId) {
        return transactionService.getAuditLogs(orderId).stream().map(transactionService::toAuditDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findStuck(int olderThanMinutes) {
        LocalDateTime cutoff = LocalINDTime.now().minusMinutes(Math.max(olderThanMinutes, 1));
        return orderRepo.findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                List.of(OrderStatus.PAYMENT_PENDING, OrderStatus.NEEDS_REVIEW), cutoff).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        LocalDateTime now = LocalINDTime.now();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) byStatus.put(s.name(), orderRepo.countByStatus(s));
        Map<String, Long> byFulfil = new LinkedHashMap<>();
        for (FulfilmentStatus s : FulfilmentStatus.values()) byFulfil.put(s.name(), orderRepo.countByFulfilmentStatus(s));
        long stuck = orderRepo.findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                List.of(OrderStatus.PAYMENT_PENDING, OrderStatus.NEEDS_REVIEW), now.minusMinutes(30)).size();
        return new StatsResponse(orderRepo.count(), orderRepo.countByCreatedAtAfter(now.toLocalDate().atStartOfDay()), stuck,
                byStatus, byFulfil, orderRepo.sumTotalByPaymentStatus(PaymentStatus.PAID),
                orderRepo.sumDepositByStatus(DepositStatus.HELD), orderRepo.sumTotalByPaymentStatus(PaymentStatus.REFUND_PENDING));
    }

    // ═══════════════════════════════════════════
    //  ADMIN: MANUAL ACTIONS (each one audited with before/after)
    // ═══════════════════════════════════════════

    /** Admin verified the payment on the Airpay dashboard -> confirm. Units must still be held. */
    @Transactional
    public OrderResponse confirm(String orderId, ConfirmRequest req, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        if (!CONFIRMABLE.contains(o.getStatus()))
            throw new SuperAdminService.BadRequestException("Cannot confirm an order in status " + o.getStatus() + ". Allowed: " + CONFIRMABLE);
        long needed = o.getItems().stream().mapToLong(RentalOrderItemEntity::getQuantity).sum();
        if (unitRepo.countByOrderPkAndActiveTrue(o.getId()) != needed)
            throw new SuperAdminService.BadRequestException(
                    "The reserved units were already released - the order cannot be confirmed. Use mark-refunded instead.");
        Transaction txn = transactionService.findByOrderId(orderId).orElseThrow(() ->
                new SuperAdminService.BadRequestException("No Airpay payment attempt exists for this order - nothing to confirm."));
        Map<String, Object> before = state(o);
        transactionService.markManualAdminConfirmed(txn, req.apTransactionId(), actorName, req.reason());
        markConfirmed(o, "ADMIN", "Manually confirmed by " + actorName + ": " + req.reason());
        orderRepo.save(o);
        adminAudit("MANUAL_CONFIRM", o, txn, before, req.reason(), actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    /** Admin verified the payment really failed. Refuses if Airpay says money was received. */
    @Transactional
    public OrderResponse reject(String orderId, ReasonRequest req, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        if (!CONFIRMABLE.contains(o.getStatus()))
            throw new SuperAdminService.BadRequestException("Only PAYMENT_PENDING or NEEDS_REVIEW orders can be rejected (current: " + o.getStatus() + ")");
        Transaction txn = transactionService.findByOrderId(orderId).orElse(null);
        if ((txn != null && txn.getStatus() == TransactionStatus.SUCCESS) || o.getPaymentStatus() == PaymentStatus.PAID)
            throw new SuperAdminService.BadRequestException(
                    "Money was received for this order - use confirm, or cancel + mark-refunded to return it.");
        Map<String, Object> before = state(o);
        if (txn != null) transactionService.markManualAdminRejected(txn, actorName, req.reason());
        o.setStatus(OrderStatus.PAYMENT_FAILED);
        o.setPaymentStatus(PaymentStatus.FAILED);
        o.setExpiresAt(null);
        o.setStatusReason(truncate("Rejected by " + actorName + ": " + req.reason(), 500));
        orderRepo.save(o);
        releaseUnits(o, "Rejected by admin");
        adminAudit("MANUAL_REJECT", o, txn, before, req.reason(), actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    /** Admin cancel (before dispatch). Paid orders become REFUND_PENDING. */
    @Transactional
    public OrderResponse cancelByAdmin(String orderId, ReasonRequest req, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        if (!OPEN_FOR_CANCEL.contains(o.getStatus()) || o.getFulfilmentStatus() != FulfilmentStatus.NOT_STARTED)
            throw new SuperAdminService.BadRequestException(
                    "Only orders that are not yet dispatched can be cancelled (status " + o.getStatus() + ", fulfilment " + o.getFulfilmentStatus() + ")");
        Map<String, Object> before = state(o);
        cancelCommon(o, "ADMIN:" + actorName, "Cancelled by " + actorName + ": " + req.reason());
        adminAudit("CANCEL", o, null, before, req.reason(), actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    @Transactional
    public OrderResponse dispatch(String orderId, NoteRequest n, UUID id, String name, String role, String ip) {
        return advance(orderId, FulfilmentStatus.NOT_STARTED, FulfilmentStatus.DISPATCHED, "DISPATCH", n, id, name, role, ip);
    }

    @Transactional
    public OrderResponse markDelivered(String orderId, NoteRequest n, UUID id, String name, String role, String ip) {
        return advance(orderId, FulfilmentStatus.DISPATCHED, FulfilmentStatus.DELIVERED, "DELIVER", n, id, name, role, ip);
    }

    @Transactional
    public OrderResponse markReturned(String orderId, NoteRequest n, UUID id, String name, String role, String ip) {
        return advance(orderId, FulfilmentStatus.DELIVERED, FulfilmentStatus.RETURNED, "RETURN", n, id, name, role, ip);
    }

    /** For orders with no held deposit (settle-deposit completes the others automatically). */
    @Transactional
    public OrderResponse complete(String orderId, NoteRequest n, UUID id, String name, String role, String ip) {
        return advance(orderId, FulfilmentStatus.RETURNED, FulfilmentStatus.COMPLETED, "COMPLETE", n, id, name, role, ip);
    }

    /** Records that a cancelled / reviewed order's payment was refunded (do the refund on the Airpay dashboard first). */
    @Transactional
    public OrderResponse markRefunded(String orderId, RefundRequest req, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        boolean okStatus = o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.NEEDS_REVIEW;
        boolean okPay = o.getPaymentStatus() == PaymentStatus.REFUND_PENDING || o.getPaymentStatus() == PaymentStatus.PAID;
        if (!okStatus || !okPay)
            throw new SuperAdminService.BadRequestException("Only a paid CANCELLED / NEEDS_REVIEW order can be marked refunded (status "
                    + o.getStatus() + ", payment " + o.getPaymentStatus() + ")");
        Map<String, Object> before = state(o);
        if (o.getStatus() == OrderStatus.NEEDS_REVIEW) {
            o.setStatus(OrderStatus.CANCELLED);
            o.setCancelledAt(LocalINDTime.now());
            o.setCancelledBy("ADMIN:" + actorName);
            releaseUnits(o, "Refunded by admin");
        }
        o.setPaymentStatus(PaymentStatus.REFUNDED);
        o.setRefundedAt(LocalINDTime.now());
        o.setRefundReference(req.refundReference());
        if (o.getDepositAmount().signum() > 0) {
            o.setDepositStatus(DepositStatus.REFUNDED);
            o.setDepositRefundedAmount(o.getDepositAmount());
        }
        o.setStatusReason(truncate("Refunded (recorded by " + actorName + "): " + req.reason(), 500));
        orderRepo.save(o);
        adminAudit("MARK_REFUNDED", o, null, before, req.reason(), actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    /** After the items are RETURNED: split the held deposit into refund + deduction, then the order is COMPLETED. */
    @Transactional
    public OrderResponse settleDeposit(String orderId, DepositSettleRequest req, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        if (o.getStatus() != OrderStatus.CONFIRMED || o.getFulfilmentStatus() != FulfilmentStatus.RETURNED)
            throw new SuperAdminService.BadRequestException("Deposit can be settled only after the order is RETURNED");
        if (o.getDepositStatus() != DepositStatus.HELD)
            throw new SuperAdminService.BadRequestException("No held deposit to settle (deposit status " + o.getDepositStatus() + ")");
        BigDecimal refund = money(req.refundAmount()), deduct = money(req.deductionAmount());
        if (refund.add(deduct).compareTo(o.getDepositAmount()) != 0)
            throw new SuperAdminService.BadRequestException("refundAmount + deductionAmount must equal the held deposit " + o.getDepositAmount());
        Map<String, Object> before = state(o);
        o.setDepositRefundedAmount(refund);
        o.setDepositDeductionAmount(deduct);
        o.setDepositStatus(deduct.signum() == 0 ? DepositStatus.REFUNDED : refund.signum() == 0 ? DepositStatus.FORFEITED : DepositStatus.PARTIALLY_REFUNDED);
        o.setDepositSettledAt(LocalINDTime.now());
        o.setDepositSettleNote(truncate(req.note(), 300));
        o.setFulfilmentStatus(FulfilmentStatus.COMPLETED);
        o.setCompletedAt(LocalINDTime.now());
        orderRepo.save(o);
        adminAudit("DEPOSIT_SETTLE", o, null, before, "refund " + refund + ", deduction " + deduct + (req.note() == null ? "" : " - " + req.note()),
                actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    private OrderResponse advance(String orderId, FulfilmentStatus expected, FulfilmentStatus next, String action,
                                  NoteRequest n, UUID actorId, String actorName, String actorRole, String ip) {
        RentalOrderEntity o = lock(orderId);
        if (o.getStatus() != OrderStatus.CONFIRMED)
            throw new SuperAdminService.BadRequestException("Only CONFIRMED orders can be fulfilled (current: " + o.getStatus() + ")");
        if (o.getFulfilmentStatus() != expected)
            throw new SuperAdminService.BadRequestException("Expected fulfilment " + expected + " but order is " + o.getFulfilmentStatus());
        if (next == FulfilmentStatus.COMPLETED && o.getDepositStatus() == DepositStatus.HELD)
            throw new SuperAdminService.BadRequestException("Settle the held deposit first (deposit-settle completes the order).");
        Map<String, Object> before = state(o);
        LocalDateTime now = LocalINDTime.now();
        o.setFulfilmentStatus(next);
        switch (next) {
            case DISPATCHED -> o.setDispatchedAt(now);
            case DELIVERED -> o.setDeliveredAt(now);
            case RETURNED -> o.setReturnedAt(now);
            case COMPLETED -> o.setCompletedAt(now);
            default -> { }
        }
        orderRepo.save(o);
        adminAudit(action, o, null, before, n == null ? null : n.note(), actorId, actorName, actorRole, ip);
        return toResponse(o);
    }

    // ═══════════════════════════════════════════
    //  QUOTE (shared by summary + book)
    // ═══════════════════════════════════════════

    private static class ItemCalc {
        ItemRequest req;
        ProductEntity product;
        String cover;
        LocalDate blockedUntil;
        int days, weekdays, weekends;
        BigDecimal dayRate = BigDecimal.ZERO, weekendRate = BigDecimal.ZERO, rentalValue = BigDecimal.ZERO,
                discount = BigDecimal.ZERO, payable = BigDecimal.ZERO, depositPer = BigDecimal.ZERO, depositTotal = BigDecimal.ZERO;
        boolean offer;
        Integer available;
        List<String> violations = new ArrayList<>();

        ItemView toView(boolean withChecks) {
            return new ItemView(null, req.productId(), product == null ? null : product.getName(), cover, req.size(), req.colour(),
                    req.quantity(), req.startDate(), req.endDate(), days, weekdays, weekends, dayRate, weekendRate, offer,
                    rentalValue, discount, payable, depositPer, depositTotal,
                    withChecks ? available : null, withChecks ? violations : null, List.of());
        }
    }

    private static class Quote {
        RentalBookingConfigEntity cfg;
        UserSubscriptionEntity running;
        SubscriptionView sub;
        AddressView address;
        String currency = "INR";
        boolean covers;
        List<ItemCalc> items = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        BigDecimal rental = BigDecimal.ZERO, discount = BigDecimal.ZERO, rentalPayable = BigDecimal.ZERO,
                deposit = BigDecimal.ZERO, delivery = BigDecimal.ZERO, total = BigDecimal.ZERO;
        String deliveryNote;
    }

    private Quote buildQuote(String username, OrderRequest req, boolean requireAddress) {
        Quote q = new Quote();
        q.cfg = config();
        LocalDate today = LocalINDTime.now().toLocalDate();

        if (req.items() == null || req.items().isEmpty()) {
            q.violations.add("At least one item is required.");
            return q;
        }
        if (req.items().size() > q.cfg.getMaxItemsPerOrder())
            q.violations.add("You can book at most " + q.cfg.getMaxItemsPerOrder() + " items per order.");

        resolveSubscription(username, q);

        if (req.addressId() == null) {
            if (requireAddress) q.violations.add("Delivery address is required.");
        } else {
            q.address = loadAddress(username, req.addressId());
            if (q.address == null) q.violations.add("Delivery address not found for your account.");
        }

        q.covers = q.sub != null && q.cfg.isSubscriptionCoversRental();
        for (ItemRequest ir : req.items()) {
            ItemCalc c = priceItem(ir, q, today);
            q.items.add(c);
            q.rental = q.rental.add(c.rentalValue);
            q.discount = q.discount.add(c.discount);
            q.rentalPayable = q.rentalPayable.add(c.payable);
            q.deposit = q.deposit.add(c.depositTotal);
            String prefix = c.product == null ? "" : c.product.getName() + ": ";
            for (String v : c.violations) q.violations.add(prefix + v);
        }

        // delivery rules
        RentalBookingConfigEntity cfg = q.cfg;
        if (q.sub != null) {
            q.delivery = BigDecimal.ZERO; q.deliveryNote = "Free delivery for subscribers";
        } else if (!cfg.isDeliveryEnabled()) {
            q.delivery = BigDecimal.ZERO; q.deliveryNote = "No delivery charge";
        } else if (cfg.getFreeDeliveryAbove() != null && q.rental.compareTo(cfg.getFreeDeliveryAbove()) >= 0) {
            q.delivery = BigDecimal.ZERO; q.deliveryNote = "Free delivery on rentals above " + cfg.getFreeDeliveryAbove();
        } else {
            q.delivery = money(cfg.getDeliveryFee()); q.deliveryNote = "Flat delivery fee";
        }
        q.rental = money(q.rental); q.discount = money(q.discount); q.rentalPayable = money(q.rentalPayable);
        q.deposit = money(q.deposit);
        q.total = money(q.rentalPayable.add(q.deposit).add(q.delivery));
        return q;
    }

    /** Rules 1, 2, 4-6: active subscription, deposit paid, effective expiry (renewals chained). */
    private void resolveSubscription(String username, Quote q) {
        LocalDateTime now = LocalINDTime.now();
        Specification<UserSubscriptionEntity> spec = (r, cq, cb) -> cb.and(
                cb.equal(r.get("username"), username),
                cb.equal(r.get("status"), UserSubscriptionEntity.Status.ACTIVE),
                cb.greaterThan(r.get("endDate"), now));
        List<UserSubscriptionEntity> active = subRepo.findAll(spec, Sort.by(Sort.Direction.ASC, "startDate"));
        UserSubscriptionEntity running = active.stream()
                .filter(s -> s.getStartDate() != null && !s.getStartDate().isAfter(now)).findFirst().orElse(null);
        if (running == null) {
            q.violations.add("You need an active subscription to book a rental.");
            return;
        }
        LocalDateTime expiry = running.getEndDate();
        for (UserSubscriptionEntity s : active) {
            if (s.getStartDate() != null && !s.getStartDate().isAfter(expiry) && s.getEndDate().isAfter(expiry)) expiry = s.getEndDate();
        }
        SubscriptionPlanEntity plan = planRepository.findById(running.getPlanId()).orElse(null);
        boolean depReq = plan != null && plan.isDepositRequired();
        BigDecimal depAmt = plan != null && plan.getDepositAmount() != null ? plan.getDepositAmount() : BigDecimal.ZERO;
        boolean depPaid = !depReq || running.getPaidAt() != null;
        if (!depPaid) q.violations.add("Your subscription deposit has not been paid yet.");
        if (running.getCurrency() != null) q.currency = running.getCurrency();
        q.running = running;
        q.sub = new SubscriptionView(running.getOrderId(), running.getPlanId(), running.getPlanName(), running.getStartDate(),
                running.getEndDate(), expiry, depReq, depAmt, depPaid);
    }

    /** Rules 3, 7-14. */
    private ItemCalc priceItem(ItemRequest ir, Quote q, LocalDate today) {
        ItemCalc c = new ItemCalc();
        c.req = ir;
        if (ir.endDate().isBefore(ir.startDate())) {
            c.violations.add("End date must be on or after the start date.");
            return c;
        }
        ProductEntity p = productRepository.findByIdAndDeletedAtIsNull(ir.productId()).orElse(null);
        if (p == null) {
            c.violations.add("Product not found.");
            return c;
        }
        c.product = p;
        if (!p.isActive() || !p.isVisible()) c.violations.add("This product is not available for rental.");
        if (ir.startDate().isBefore(today)) c.violations.add("Start date cannot be in the past.");

        RentalCalendarDtoHolder cfg = RentalCalendarDtoHolder.of(calendarService.resolveConfig(p.getId()));
        c.days = (int) ChronoUnit.DAYS.between(ir.startDate(), ir.endDate()) + 1;
        if (c.days < cfg.minRentalDays) c.violations.add("Minimum rental duration is " + cfg.minRentalDays + " day(s), requested " + c.days + ".");
        if (ir.startDate().isAfter(today.plusDays(cfg.advanceBookingDays)))
            c.violations.add("Bookings can only be made up to " + cfg.advanceBookingDays + " day(s) in advance.");
        c.blockedUntil = ir.endDate().plusDays(cfg.cleaningBufferDays);

        if (q.sub != null) {
            LocalDate expiry = q.sub.effectiveExpiry().toLocalDate();
            if (ir.startDate().isAfter(expiry)) c.violations.add("Rental starts after your subscription expires (" + expiry + "). Renew or extend first.");
            else if (ir.endDate().isAfter(expiry)) c.violations.add("Rental end date exceeds your subscription expiry (" + expiry + "). Renew or extend first.");
            // No rows in product_plan_access for this product = available to ALL plans.
            Integer totalLinks = jdbc.queryForObject("select count(*) from product_plan_access where product_id = ?",
                    Integer.class, p.getId());
            if (totalLinks != null && totalLinks > 0) {
                Integer n = jdbc.queryForObject("select count(*) from product_plan_access where plan_id = ? and product_id = ?",
                        Integer.class, q.running.getPlanId(), p.getId());
                if (n == null || n == 0) c.violations.add("This product is not included in your plan (" + q.running.getPlanName() + ").");
            }}

        // pricing: offer price replaces the per-day price; weekend price (Sat/Sun) applies when set
        BigDecimal perDay = nz(p.getPerDayPrice());
        c.offer = p.getOfferPrice() != null && p.getOfferPrice().signum() > 0 && p.getOfferPrice().compareTo(perDay) < 0;
        c.dayRate = money(c.offer ? p.getOfferPrice() : perDay);
        c.weekendRate = money(p.getWeekendPrice() != null ? p.getWeekendPrice() : c.dayRate);
        for (LocalDate d = ir.startDate(); !d.isAfter(ir.endDate()); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) c.weekends++; else c.weekdays++;
        }
        c.rentalValue = money(c.dayRate.multiply(BigDecimal.valueOf(c.weekdays))
                .add(c.weekendRate.multiply(BigDecimal.valueOf(c.weekends))).multiply(BigDecimal.valueOf(ir.quantity())));
        c.discount = q.covers ? c.rentalValue : BigDecimal.ZERO;
        c.payable = money(c.rentalValue.subtract(c.discount));
        c.depositPer = q.sub != null ? money(BigDecimal.ZERO) : money(nz(p.getSecurityDeposit()));
        c.depositTotal = money(c.depositPer.multiply(BigDecimal.valueOf(ir.quantity())));
        c.cover = imageRepository.findByProduct_Id(p.getId()).stream().findFirst().map(ProductImageEntity::getImageUrl).orElse(null);

        // rules 8, 9, 12 (advisory here; re-checked under lock in createBooking)
        List<InventoryEntity> free = freeUnits(p, ir.size(), ir.colour(), ir.startDate(), c.blockedUntil);
        c.available = free.size();
        if (free.size() < ir.quantity())
            c.violations.add("Only " + free.size() + " unit(s) of size " + ir.size().trim() + " available for these dates (includes the "
                    + cfg.cleaningBufferDays + "-day cleaning buffer).");
        return c;
    }

    /** Small read-only copy of the resolved calendar config. */
    private static final class RentalCalendarDtoHolder {
        int minRentalDays, advanceBookingDays, cleaningBufferDays;
        static RentalCalendarDtoHolder of(com.example.demo.dto.RentalCalendarDto.ConfigResponse r) {
            RentalCalendarDtoHolder h = new RentalCalendarDtoHolder();
            h.minRentalDays = r.getMinRentalDays();
            h.advanceBookingDays = r.getAdvanceBookingDays();
            h.cleaningBufferDays = r.getCleaningBufferDays();
            return h;
        }
    }

    private List<InventoryEntity> matchingUnits(ProductEntity p, String size, String colour) {
        String sz = size.trim();
        String col = clean(colour);
        return inventoryRepository.findByProduct_Id(p.getId()).stream()
                .filter(InventoryEntity::isActive)
                .filter(i -> !"DAMAGED".equalsIgnoreCase(i.getStatus()))
                .filter(i -> i.getSize() != null && i.getSize().trim().equalsIgnoreCase(sz))
                .filter(i -> col == null || (i.getColour() != null && i.getColour().trim().equalsIgnoreCase(col)))
                .toList();
    }

    /** Units of the size not held by an order and not blocked for any day in [start, until]. */
    private List<InventoryEntity> freeUnits(ProductEntity p, String size, String colour, LocalDate start, LocalDate until) {
        Set<UUID> busy = new HashSet<>(unitRepo.findOccupiedInventoryIds(p.getId(), start, until));
        for (InventoryBlockEntity b : blockRepository
                .findByProduct_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(p.getId(), until, start)) {
            busy.add(b.getInventory().getId());
        }
        return matchingUnits(p, size, colour).stream()
                .filter(i -> !busy.contains(i.getId()))
                .sorted(Comparator.comparing(i -> i.getSku() == null ? "" : i.getSku()))
                .toList();
    }

    // ═══════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════

    private void markConfirmed(RentalOrderEntity o, String by, String reason) {
        LocalDateTime now = LocalINDTime.now();
        boolean paid = o.getTotalPayable().signum() > 0;
        o.setStatus(OrderStatus.CONFIRMED);
        o.setPaymentStatus(paid ? PaymentStatus.PAID : PaymentStatus.NOT_REQUIRED);
        if (paid && o.getPaidAt() == null) o.setPaidAt(now);
        o.setConfirmedAt(now);
        o.setExpiresAt(null);
        o.setActivatedBy(by);
        o.setDepositStatus(o.getDepositAmount().signum() > 0 ? DepositStatus.HELD : DepositStatus.NOT_APPLICABLE);
        o.setStatusReason(truncate(reason, 500));
    }

    private void moveToReview(RentalOrderEntity o, String from, String reason) {
        o.setStatus(OrderStatus.NEEDS_REVIEW);
        o.setStatusReason(truncate(reason, 500));
        orderRepo.save(o);
        log.warn("orderId={} -> NEEDS_REVIEW: {}", o.getOrderId(), reason);
        event(o, "NEEDS_REVIEW", from, "NEEDS_REVIEW", "SYSTEM", "SYSTEM", reason, true);
    }

    private void cancelCommon(RentalOrderEntity o, String by, String reason) {
        boolean wasPaid = o.getPaymentStatus() == PaymentStatus.PAID;
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelledAt(LocalINDTime.now());
        o.setCancelledBy(by);
        o.setExpiresAt(null);
        o.setStatusReason(truncate(reason, 500));
        if (wasPaid) o.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        else if (o.getPaymentStatus() == PaymentStatus.PENDING) o.setPaymentStatus(PaymentStatus.FAILED);
        orderRepo.save(o);
        releaseUnits(o, "Order cancelled");
    }

    private void releaseUnits(RentalOrderEntity o, String reason) {
        LocalDateTime now = LocalINDTime.now();
        List<RentalOrderUnitEntity> live = unitRepo.findByOrderPkAndActiveTrue(o.getId());
        for (RentalOrderUnitEntity u : live) {
            u.setActive(false);
            u.setReleasedAt(now);
            u.setReleaseReason(truncate(reason, 200));
        }
        unitRepo.saveAll(live);
    }

    private boolean amountMatches(RentalOrderEntity o, String ipnAmount) {
        try {
            if (ipnAmount == null) return false;
            BigDecimal paid = new BigDecimal(ipnAmount.trim());
            BigDecimal expected = transactionService.findByOrderId(o.getOrderId()).map(Transaction::getAmount).orElse(o.getTotalPayable());
            return paid.compareTo(expected) == 0 && paid.compareTo(o.getTotalPayable()) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private PaymentRequest buildPaymentRequest(RentalOrderEntity o) {
        String name = alnum(o.getShipFullName());
        String[] parts = name.trim().split("\\s+", 2);
        String first = parts[0].isEmpty() ? "Customer" : parts[0];
        String last = parts.length > 1 && !parts[1].isBlank() ? parts[1] : first;
        String addr = alnum(String.join(" ", Arrays.asList(nn(o.getShipLine1()), nn(o.getShipLine2()), nn(o.getShipLandmark()))));
        return new PaymentRequest(o.getOrderId(), o.getBuyerEmail(), o.getBuyerPhone().replaceAll("[^0-9]", ""),
                first, last, truncate(addr, 255), o.getTotalPayable().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                alnum(o.getShipCity()), alnum(o.getShipState()), alnum(o.getShipPincode()).replace(" ", ""),
                alnum(o.getShipCountry()), CUSTOMVAR, TXN_SUBTYPE);
    }

    private AddressView loadAddress(String username, UUID id) {
        List<AddressView> l = jdbc.query(
                "select id, full_name, phone, address_line1, address_line2, landmark, city, state, pincode, country "
                        + "from user_addresses where id = ? and username = ?",
                (rs, i) -> new AddressView(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("phone"),
                        rs.getString("address_line1"), rs.getString("address_line2"), rs.getString("landmark"),
                        rs.getString("city"), rs.getString("state"), rs.getString("pincode"), rs.getString("country")),
                id, username);
        return l.isEmpty() ? null : l.get(0);
    }

    private PageResult<OrderResponse> page(Specification<RentalOrderEntity> spec, int page, int size) {
        Page<RentalOrderEntity> r = orderRepo.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(r.getContent().stream().map(this::toResponse).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    private RentalOrderEntity find(String orderId) {
        return orderRepo.findByOrderId(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Order not found: " + orderId));
    }

    private RentalOrderEntity lock(String orderId) {
        return orderRepo.findByOrderIdForUpdate(orderId).orElseThrow(() ->
                new SuperAdminService.ResourceNotFoundException("Order not found: " + orderId));
    }

    private OrderStatus parseStatus(String s) { return parseEnum(OrderStatus.class, s, "status"); }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String s, String field) {
        if (s == null || s.isBlank()) return null;
        try {
            return Enum.valueOf(type, s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SuperAdminService.BadRequestException("Invalid " + field + ". Must be one of: " + Arrays.toString(type.getEnumConstants()));
        }
    }

    private String newOrderId(LocalDateTime now) {
        // ORD (3) + yyMMddHHmmss (12) + 5 random = 20 chars (Airpay max is 20, alphanumeric only)
        return ORDER_PREFIX + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    }

    private void event(RentalOrderEntity o, String type, String from, String to, String actor, String role, String msg, boolean visible) {
        RentalOrderEventEntity e = new RentalOrderEventEntity();
        e.setOrderId(o.getOrderId());
        e.setEventType(type);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setActor(actor);
        e.setActorRole(role);
        e.setMessage(truncate(msg, 500));
        e.setUserVisible(visible);
        eventRepo.save(e);
    }

    /** Global audit (audit_logs) + order timeline + order-level audit (transaction_audit_logs). */
    private void adminAudit(String action, RentalOrderEntity o, Transaction txn, Map<String, Object> before, String reason,
                            UUID actorId, String actorName, String actorRole, String ip) {
        Map<String, Object> after = state(o);
        superAdminService.writeAudit(actorId, actorName, actorRole, action, MODULE, o.getOrderId(), before, after, ip);
        event(o, "ADMIN_" + action, String.valueOf(before.get("status")), o.getStatus().name(), actorName, actorRole,
                action + (reason == null ? "" : ": " + reason), action.equals("MANUAL_CONFIRM") || action.equals("CANCEL") || action.equals("DISPATCH")
                        || action.equals("DELIVER") || action.equals("RETURN") || action.equals("COMPLETE"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("reason", reason);
        payload.put("actor", actorName);
        payload.put("role", actorRole);
        payload.put("ip", ip);
        payload.put("before", before);
        payload.put("after", after);
        Transaction t = txn != null ? txn : transactionService.findByOrderId(o.getOrderId()).orElse(null);
        transactionService.logAudit(o.getOrderId(), t, AuditEventType.ADMIN_ACTION, actorName + " (" + actorRole + ")",
                json(payload), "[RENTAL_ORDER] " + action + (reason == null ? "" : ": " + reason));
    }

    private Map<String, Object> state(RentalOrderEntity o) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("orderId", o.getOrderId());
        s.put("username", o.getUsername());
        s.put("status", o.getStatus() == null ? null : o.getStatus().name());
        s.put("paymentStatus", o.getPaymentStatus() == null ? null : o.getPaymentStatus().name());
        s.put("fulfilmentStatus", o.getFulfilmentStatus() == null ? null : o.getFulfilmentStatus().name());
        s.put("depositStatus", o.getDepositStatus() == null ? null : o.getDepositStatus().name());
        s.put("totalPayable", o.getTotalPayable());
        s.put("statusReason", o.getStatusReason());
        return s;
    }

    private Map<String, Object> configState(RentalBookingConfigEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deliveryEnabled", c.isDeliveryEnabled());
        m.put("deliveryFee", c.getDeliveryFee());
        m.put("freeDeliveryAbove", c.getFreeDeliveryAbove());
        m.put("waiveDeliveryForSubscribers", c.isWaiveDeliveryForSubscribers());
        m.put("subscriptionCoversRental", c.isSubscriptionCoversRental());
        m.put("pendingHoldMinutes", c.getPendingHoldMinutes());
        m.put("cancellationCutoffDays", c.getCancellationCutoffDays());
        m.put("maxItemsPerOrder", c.getMaxItemsPerOrder());
        return m;
    }

    /** Rule 22: everything that decided the price is frozen on the order. */
    private Map<String, Object> pricingSnapshot(Quote q) {
        Map<String, Object> m = new LinkedHashMap<>();
//        m.put("subscriptionOrderId", q.sub.orderId());
//        m.put("planId", String.valueOf(q.sub.planId()));
//        m.put("planName", q.sub.planName());
//        m.put("subscriptionStart", String.valueOf(q.sub.startDate()));
//        m.put("subscriptionEnd", String.valueOf(q.sub.endDate()));
//        m.put("effectiveExpiry", String.valueOf(q.sub.effectiveExpiry()));
//        m.put("subscriptionDepositRequired", q.sub.planDepositRequired());
//        m.put("subscriptionDepositAmount", q.sub.planDepositAmount());
        if (q.sub != null) {
            m.put("subscriptionOrderId", q.sub.orderId());
            m.put("planId", String.valueOf(q.sub.planId()));
            m.put("planName", q.sub.planName());
            m.put("subscriptionStart", String.valueOf(q.sub.startDate()));
            m.put("subscriptionEnd", String.valueOf(q.sub.endDate()));
            m.put("effectiveExpiry", String.valueOf(q.sub.effectiveExpiry()));
            m.put("subscriptionDepositRequired", q.sub.planDepositRequired());
            m.put("subscriptionDepositAmount", q.sub.planDepositAmount());
        } else {
            m.put("subscription", "NONE");
        }
        m.put("subscriptionCoversRental", q.covers);
        m.put("weekendDays", "SAT,SUN");
        m.put("deliveryFeeRule", q.cfg.getDeliveryFee());
        m.put("freeDeliveryAbove", q.cfg.getFreeDeliveryAbove());
        m.put("waiveDeliveryForSubscribers", q.cfg.isWaiveDeliveryForSubscribers());
        m.put("deliveryNote", q.deliveryNote);
        return m;
    }

    private ConfigResponse toConfigResponse(RentalBookingConfigEntity c) {
        return new ConfigResponse(c.getId(), c.isDeliveryEnabled(), c.getDeliveryFee(), c.getFreeDeliveryAbove(),
                c.isWaiveDeliveryForSubscribers(), c.isSubscriptionCoversRental(), c.getPendingHoldMinutes(),
                c.getCancellationCutoffDays(), c.getMaxItemsPerOrder(), c.getUpdatedAt());
    }

    static String label(OrderStatus s) {
        return switch (s) {
            case CONFIRMED -> "Success";
            case PAYMENT_PENDING -> "Payment Pending";
            case PAYMENT_FAILED -> "Payment Failed";
            case CANCELLED -> "Order Cancelled";
            case NEEDS_REVIEW -> "Under Review";
        };
    }

    private OrderResponse toResponse(RentalOrderEntity o) {
        Map<UUID, List<String>> skus = new HashMap<>();
        for (RentalOrderUnitEntity u : unitRepo.findByOrderPk(o.getId()))
            skus.computeIfAbsent(u.getItemId(), k -> new ArrayList<>()).add(u.getSku());
        List<ItemView> items = o.getItems().stream().map(i -> new ItemView(i.getId(), i.getProductId(), i.getProductName(),
                i.getCoverImageUrl(), i.getSize(), i.getColour(), i.getQuantity(), i.getStartDate(), i.getEndDate(),
                i.getRentalDays(), i.getWeekdayCount(), i.getWeekendCount(), i.getDayRate(), i.getWeekendRate(),
                i.isOfferApplied(), i.getRentalValue(), i.getSubscriptionDiscount(), i.getRentalPayable(),
                i.getDepositPerUnit(), i.getDepositTotal(), null, null,
                skus.getOrDefault(i.getId(), List.of()))).toList();
        AddressView addr = new AddressView(o.getAddressId(), o.getShipFullName(), o.getShipPhone(), o.getShipLine1(),
                o.getShipLine2(), o.getShipLandmark(), o.getShipCity(), o.getShipState(), o.getShipPincode(), o.getShipCountry());
        return new OrderResponse(o.getId(), o.getOrderId(), o.getUsername(),
                o.getStatus().name(), label(o.getStatus()), o.getPaymentStatus().name(), o.getFulfilmentStatus().name(),
                o.getDepositStatus().name(), o.getStatusReason(), o.getTotalPayable().signum() > 0, o.getCurrency(),
                o.getRentalValue(), o.getSubscriptionDiscount(), o.getRentalPayable(),
                o.getDepositAmount(), o.getDeliveryFee(), o.getTotalPayable(),
                o.getSubscriptionOrderId(), o.getPlanName(), addr, items,
                o.getStartDate(), o.getEndDate(), o.getExpiresAt(), o.getCreatedAt(),
                o.getPaidAt(), o.getConfirmedAt(), o.getCancelledAt(),
                o.getDispatchedAt(), o.getDeliveredAt(), o.getReturnedAt(),
                o.getCompletedAt(), o.getRefundedAt(), o.getRefundReference(),
                o.getDepositRefundedAmount(), o.getDepositDeductionAmount(), o.getDepositSettledAt());
    }

    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"could not serialize payload\"}";
        }
    }

    private static BigDecimal money(BigDecimal v) { return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String nn(String s) { return s == null ? "" : s; }
    private static String clean(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }
    private static String alnum(String s) { return s == null ? "" : s.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim(); }
    private static String truncate(String s, int max) { return s == null || s.length() <= max ? s : s.substring(0, max); }
}
