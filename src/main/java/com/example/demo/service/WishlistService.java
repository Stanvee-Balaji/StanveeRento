package com.example.demo.service;

import com.example.demo.dto.WishlistDto;
import com.example.demo.entity.ProductEntity;
import com.example.demo.entity.WishlistItemEntity;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Wishlist for Stanvee users. Owner is ALWAYS the username from the JWT.
 */
@Service
public class WishlistService {

    private static final int MAX_ITEMS = 100;

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

    public WishlistService(WishlistItemRepository wishlistRepository,
                           ProductRepository productRepository,
                           ProductImageRepository imageRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
    }

    // ───────── READ ─────────

    @Transactional(readOnly = true)
    public WishlistDto.WishlistResponse getWishlist(String username) {
        return buildResponse(username);
    }

    @Transactional(readOnly = true)
    public WishlistDto.WishlistCountResponse count(String username) {
        return new WishlistDto.WishlistCountResponse((int) wishlistRepository.countByUsername(username));
    }

    @Transactional(readOnly = true)
    public WishlistDto.WishlistCheckResponse check(String username, UUID productId) {
        return new WishlistDto.WishlistCheckResponse(
                productId, wishlistRepository.existsByUsernameAndProduct_Id(username, productId));
    }

    // ───────── ADD ─────────

    @Transactional
    public WishlistDto.WishlistResponse add(String username, WishlistDto.AddItemRequest req) {
        ProductEntity product = productRepository.findByIdAndDeletedAtIsNull(req.getProductId())
                .filter(ProductEntity::isActive)
                .filter(ProductEntity::isVisible)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Product not found: " + req.getProductId()));

        if (wishlistRepository.existsByUsernameAndProduct_Id(username, product.getId())) {
            throw new SuperAdminService.BadRequestException("Product is already in your wishlist");
        }
        if (wishlistRepository.countByUsername(username) >= MAX_ITEMS) {
            throw new SuperAdminService.BadRequestException("Wishlist is full (max " + MAX_ITEMS + " items)");
        }

        WishlistItemEntity item = new WishlistItemEntity();
        item.setUsername(username);
        item.setProduct(product);
        wishlistRepository.save(item);

        return buildResponse(username);
    }

    // ───────── REMOVE / CLEAR ─────────

    @Transactional
    public WishlistDto.WishlistResponse remove(String username, UUID productId) {
        WishlistItemEntity item = wishlistRepository.findByUsernameAndProduct_Id(username, productId)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Product is not in your wishlist: " + productId));
        wishlistRepository.delete(item);
        return buildResponse(username);
    }

    @Transactional
    public WishlistDto.WishlistResponse clear(String username) {
        wishlistRepository.deleteByUsername(username);
        return buildResponse(username);
    }

    // ───────── INTERNAL ─────────

    private WishlistDto.WishlistResponse buildResponse(String username) {
        List<WishlistDto.WishlistItemResponse> items = wishlistRepository
                .findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toItem)
                .toList();

        WishlistDto.WishlistResponse r = new WishlistDto.WishlistResponse();
        r.setUsername(username);
        r.setItems(items);
        r.setItemCount(items.size());
        return r;
    }

    private WishlistDto.WishlistItemResponse toItem(WishlistItemEntity w) {
        ProductEntity p = w.getProduct();

        WishlistDto.WishlistItemResponse ir = new WishlistDto.WishlistItemResponse();
        ir.setWishlistItemId(w.getId());
        ir.setProductId(p.getId());
        ir.setProductName(p.getName());
        ir.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);
        ir.setColour(p.getColour());
        ir.setOccasion(p.getOccasion());
        ir.setPerDayPrice(p.getPerDayPrice());
        ir.setOfferPrice(p.getOfferPrice());
        ir.setAvailable(p.getDeletedAt() == null && p.isActive() && p.isVisible());
        ir.setAddedAt(w.getCreatedAt());
        imageRepository.findByProduct_Id(p.getId()).stream().findFirst()
                .ifPresent(img -> ir.setCoverImageUrl(img.getImageUrl()));
        return ir;
    }
}
