package com.example.demo.controller;

import com.example.demo.dto.WishlistDto;
import com.example.demo.service.WishlistService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Wishlist API for logged-in Stanvee users.
 * Every call needs:  Authorization: Bearer <token from POST /api/auth/login>
 * (Reuses UserAuthUtil created for the cart.)
 *
 *  GET    /api/v1/wishlist                          → view wishlist
 *  GET    /api/v1/wishlist/count                    → badge count
 *  GET    /api/v1/wishlist/check/{productId}        → is this product wishlisted? (heart icon)
 *  POST   /api/v1/wishlist/add/items                → add product
 *  DELETE /api/v1/wishlist/remove/items/{productId} → remove product
 *  DELETE /api/v1/wishlist                          → clear wishlist
 */
@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserAuthUtil userAuth;

    public WishlistController(WishlistService wishlistService, UserAuthUtil userAuth) {
        this.wishlistService = wishlistService;
        this.userAuth = userAuth;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistDto.WishlistResponse>> getWishlist(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", wishlistService.getWishlist(username), 200));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<WishlistDto.WishlistCountResponse>> count(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Wishlist count fetched", wishlistService.count(username), 200));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<WishlistDto.WishlistCheckResponse>> check(
            @PathVariable UUID productId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Wishlist status fetched", wishlistService.check(username, productId), 200));
    }

    @PostMapping("/add/items")
    public ResponseEntity<ApiResponse<WishlistDto.WishlistResponse>> add(
            @Valid @RequestBody WishlistDto.AddItemRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.add(username, request), 200));
    }

    @DeleteMapping("/remove/items/{productId}")
    public ResponseEntity<ApiResponse<WishlistDto.WishlistResponse>> remove(
            @PathVariable UUID productId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlistService.remove(username, productId), 200));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<WishlistDto.WishlistResponse>> clear(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared", wishlistService.clear(username), 200));
    }
}
