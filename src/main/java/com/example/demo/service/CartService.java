package com.example.demo.service;

import com.example.demo.dto.CartDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cart for Stanvee users. The cart owner is ALWAYS the username taken from the
 * JWT (never from the request body), so a user can only touch their own cart.
 */
@Service
public class CartService {

    private static final String ACTIVE = "ACTIVE";
    private static final int MAX_ITEMS = 20;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository imageRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       InventoryRepository inventoryRepository,
                       ProductImageRepository imageRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.imageRepository = imageRepository;
    }

    // ───────── READ ─────────

    @Transactional(readOnly = true)
    public CartDto.CartResponse getCart(String username) {
        CartEntity cart = cartRepository
                .findFirstByUsernameAndStatusOrderByCreatedAtDesc(username, ACTIVE).orElse(null);
        return buildResponse(username, cart);
    }

    @Transactional(readOnly = true)
    public CartDto.CartCountResponse count(String username) {
        return cartRepository.findFirstByUsernameAndStatusOrderByCreatedAtDesc(username, ACTIVE)
                .map(c -> new CartDto.CartCountResponse((int) cartItemRepository.countByCart_Id(c.getId())))
                .orElse(new CartDto.CartCountResponse(0));
    }

    // ───────── ADD ─────────

    @Transactional
    public CartDto.CartResponse addItem(String username, CartDto.AddItemRequest req) {
        String size = req.getSize().trim();
        ProductEntity product = validate(req.getProductId(), size, req.getStartDate(), req.getEndDate());

        CartEntity cart = cartRepository
                .findFirstByUsernameAndStatusOrderByCreatedAtDesc(username, ACTIVE)
                .orElseGet(() -> {
                    CartEntity c = new CartEntity();
                    c.setUsername(username);
                    c.setStatus(ACTIVE);
                    return cartRepository.save(c);
                });

        if (cartItemRepository.countByCart_Id(cart.getId()) >= MAX_ITEMS) {
            throw new SuperAdminService.BadRequestException("Cart is full (max " + MAX_ITEMS + " items)");
        }

        cartItemRepository.findByCart_IdAndProduct_IdAndSizeIgnoreCaseAndStartDateAndEndDate(
                cart.getId(), product.getId(), size, req.getStartDate(), req.getEndDate())
                .ifPresent(x -> {
                    throw new SuperAdminService.BadRequestException(
                            "This product with the same size and dates is already in your cart");
                });

        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setProduct(product);
        item.setSize(size);
        item.setStartDate(req.getStartDate());
        item.setEndDate(req.getEndDate());
        cartItemRepository.save(item);

        touch(cart);
        return buildResponse(username, cart);
    }

    // ───────── UPDATE (size / dates) ─────────

    @Transactional
    public CartDto.CartResponse updateItem(String username, UUID itemId, CartDto.UpdateItemRequest req) {
        CartEntity cart = requireCart(username);
        CartItemEntity item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Cart item not found: " + itemId));

        String size = req.getSize().trim();
        ProductEntity product = validate(item.getProduct().getId(), size, req.getStartDate(), req.getEndDate());

        cartItemRepository.findByCart_IdAndProduct_IdAndSizeIgnoreCaseAndStartDateAndEndDate(
                cart.getId(), product.getId(), size, req.getStartDate(), req.getEndDate())
                .filter(other -> !other.getId().equals(item.getId()))
                .ifPresent(x -> {
                    throw new SuperAdminService.BadRequestException(
                            "Another cart item with the same product, size and dates already exists");
                });

        item.setSize(size);
        item.setStartDate(req.getStartDate());
        item.setEndDate(req.getEndDate());
        cartItemRepository.save(item);

        touch(cart);
        return buildResponse(username, cart);
    }

    // ───────── REMOVE / CLEAR ─────────

    @Transactional
    public CartDto.CartResponse removeItem(String username, UUID itemId) {
        CartEntity cart = requireCart(username);
        CartItemEntity item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Cart item not found: " + itemId));
        cartItemRepository.delete(item);
        touch(cart);
        return buildResponse(username, cart);
    }

    @Transactional
    public CartDto.CartResponse clear(String username) {
        CartEntity cart = cartRepository
                .findFirstByUsernameAndStatusOrderByCreatedAtDesc(username, ACTIVE).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCart_Id(cart.getId());
            touch(cart);
        }
        return buildResponse(username, cart);
    }

    // ───────── INTERNAL ─────────

    private CartEntity requireCart(String username) {
        return cartRepository.findFirstByUsernameAndStatusOrderByCreatedAtDesc(username, ACTIVE)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Cart is empty"));
    }

    private void touch(CartEntity cart) {
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /** Validates dates, product visibility, size and stock. Returns the product. */
    private ProductEntity validate(UUID productId, String size, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(IST);
        if (start.isBefore(today)) {
            throw new SuperAdminService.BadRequestException("startDate cannot be in the past");
        }
        if (end.isBefore(start)) {
            throw new SuperAdminService.BadRequestException("endDate must be on or after startDate");
        }

        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(ProductEntity::isActive)
                .filter(ProductEntity::isVisible)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Product not found: " + productId));

        List<InventoryEntity> sizeUnits = inventoryRepository.findByProduct_Id(productId).stream()
                .filter(i -> i.getSize() != null && i.getSize().trim().equalsIgnoreCase(size))
                .toList();
        if (sizeUnits.isEmpty()) {
            throw new SuperAdminService.BadRequestException("Size '" + size + "' does not exist for this product");
        }
        boolean anyFree = sizeUnits.stream().anyMatch(i -> isFreeForDates(i, start, end));
        if (!anyFree) {
            throw new SuperAdminService.BadRequestException(
                    "Size '" + size + "' is not available for " + start + " to " + end);
        }
        return product;
    }

    /** Same rules as UserProductService.isAvailableForDates. */
    private boolean isFreeForDates(InventoryEntity inv, LocalDate start, LocalDate end) {
        if (!inv.isActive()) return false;
        if (!"AVAILABLE".equalsIgnoreCase(inv.getStatus())) return false;
        if (inv.isBlocked()) {
            LocalDateTime bF = inv.getBlockedFrom();
            LocalDateTime bT = inv.getBlockedTo();
            if (bF == null || bT == null) return false;
            LocalDateTime from = start.atStartOfDay();
            LocalDateTime to = end.atTime(LocalTime.MAX);
            return bT.isBefore(from) || bF.isAfter(to);
        }
        return true;
    }

    private CartDto.CartResponse buildResponse(String username, CartEntity cart) {
        CartDto.CartResponse r = new CartDto.CartResponse();
        r.setUsername(username);

        List<CartDto.CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal deposit = BigDecimal.ZERO;

        if (cart != null) {
            r.setCartId(cart.getId());
            for (CartItemEntity ci : cartItemRepository.findByCart_IdOrderByCreatedAtAsc(cart.getId())) {
                CartDto.CartItemResponse ir = toItem(ci);
                items.add(ir);
                subtotal = subtotal.add(ir.getLineTotal());
                if (ir.getSecurityDeposit() != null) deposit = deposit.add(ir.getSecurityDeposit());
            }
        }

        r.setItems(items);
        r.setItemCount(items.size());
        r.setRentalSubtotal(subtotal);
        r.setTotalSecurityDeposit(deposit);
        r.setGrandTotal(subtotal.add(deposit));
        return r;
    }

    private CartDto.CartItemResponse toItem(CartItemEntity ci) {
        ProductEntity p = ci.getProduct();
        int days = (int) ChronoUnit.DAYS.between(ci.getStartDate(), ci.getEndDate()) + 1;
        BigDecimal perDay = p.getOfferPrice() != null ? p.getOfferPrice() : p.getPerDayPrice();

        boolean stillAvailable = p.getDeletedAt() == null && p.isActive() && p.isVisible()
                && inventoryRepository.findByProduct_Id(p.getId()).stream()
                        .filter(i -> i.getSize() != null && i.getSize().trim().equalsIgnoreCase(ci.getSize()))
                        .anyMatch(i -> isFreeForDates(i, ci.getStartDate(), ci.getEndDate()));

        CartDto.CartItemResponse ir = new CartDto.CartItemResponse();
        ir.setItemId(ci.getId());
        ir.setProductId(p.getId());
        ir.setProductName(p.getName());
        ir.setColour(p.getColour());
        imageRepository.findByProduct_Id(p.getId()).stream().findFirst()
                .ifPresent(img -> ir.setCoverImageUrl(img.getImageUrl()));
        ir.setSize(ci.getSize());
        ir.setStartDate(ci.getStartDate());
        ir.setEndDate(ci.getEndDate());
        ir.setDays(days);
        ir.setPricePerDay(perDay);
        ir.setLineTotal(perDay.multiply(BigDecimal.valueOf(days)));
        ir.setSecurityDeposit(p.getSecurityDeposit());
        ir.setStillAvailable(stillAvailable);
        return ir;
    }
}
