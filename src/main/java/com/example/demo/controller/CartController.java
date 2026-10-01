package com.example.demo.controller;

import com.example.demo.dto.CartDto;
import com.example.demo.service.CartService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Cart API for logged-in Stanvee users.
 * Every call needs:  Authorization: Bearer <token from POST /api/auth/login>
 * The username is read from the token — never sent in the body.
 *
 *  GET    /api/v1/cart               → view cart
 *  GET    /api/v1/cart/count         → badge count
 *  POST   /api/v1/cart/add/items     → add item
 *  PUT    /api/v1/cart/items/{id}    → change size / dates
 *  DELETE /api/v1/cart/remove/items/{id} → remove one item
 *  DELETE /api/v1/cart               → clear cart
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final UserAuthUtil userAuth;

    public CartController(CartService cartService, UserAuthUtil userAuth) {
        this.cartService = cartService;
        this.userAuth = userAuth;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> getCart(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart(username), 200));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<CartDto.CartCountResponse>> count(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Cart count fetched", cartService.count(username), 200));
    }

    @PostMapping("/add/items")
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> addItem(
            @Valid @RequestBody CartDto.AddItemRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addItem(username, request), 200));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody CartDto.UpdateItemRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cartService.updateItem(username, itemId, request), 200));
    }

    @DeleteMapping("/remove/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> removeItem(
            @PathVariable UUID itemId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeItem(username, itemId), 200));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> clear(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartService.clear(username), 200));
    }
}
