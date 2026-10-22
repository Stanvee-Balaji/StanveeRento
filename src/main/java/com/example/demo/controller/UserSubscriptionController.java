//package com.example.demo.controller;
//
//import com.example.demo.dto.SubscriptionPurchaseDto;
//import com.example.demo.service.UserSubscriptionService;
//import com.example.demo.util.ApiResponse;
//import com.example.demo.util.SuperAdminUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.Valid;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AnonymousAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
///**
// * User-facing subscription purchase API (login required). Always scoped to the logged-in user.
// *
// *  POST /api/v1/user/subscriptions/purchase        start a purchase -> Airpay iframe params
// *  GET  /api/v1/user/subscriptions/my              my purchase history
// *  GET  /api/v1/user/subscriptions/current         my currently running subscription (data = null if none)
// *  GET  /api/v1/user/subscriptions/{orderId}       poll one of MY orders after payment
// */
//@RestController
//@RequestMapping("/api/v1/user/subscriptions")
//public class UserSubscriptionController {
//
//    private final UserSubscriptionService service;
//    private final SuperAdminUtil util; // reused only for getClientIp(...)
//
//    public UserSubscriptionController(UserSubscriptionService service, SuperAdminUtil util) {
//        this.service = service;
//        this.util = util;
//    }
//
//    @PostMapping("/purchase")
//    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.InitiateResponse>> purchase(
//            @Valid @RequestBody SubscriptionPurchaseDto.PurchaseRequest request, HttpServletRequest http) {
//        return ResponseEntity.ok(ApiResponse.success("Payment initiated",
//                service.purchase(currentUsername(), request, util.getClientIp(http)), 200));
//    }
//
//    @GetMapping("/my")
//    public ResponseEntity<ApiResponse<List<SubscriptionPurchaseDto.UserPurchaseResponse>>> my() {
//        return ResponseEntity.ok(ApiResponse.success("Purchases fetched", service.myPurchases(currentUsername()), 200));
//    }
//
//    @GetMapping("/current")
//    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.UserPurchaseResponse>> current() {
//        SubscriptionPurchaseDto.UserPurchaseResponse cur = service.myCurrent(currentUsername());
//        return ResponseEntity.ok(ApiResponse.success(
//                cur == null ? "No active subscription" : "Active subscription fetched", cur, 200));
//    }
//
//    @GetMapping("/{orderId}")
//    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.UserPurchaseResponse>> status(@PathVariable String orderId) {
//        return ResponseEntity.ok(ApiResponse.success("Purchase fetched", service.myPurchase(currentUsername(), orderId), 200));
//    }
//
//    /**
//     * ADAPT IF NEEDED: returns the authenticated principal's name, which must equal the `username` used by
//     * carts / wishlist_items / stanvee_rento_user in your existing user JWT flow.
//     */
//    private String currentUsername() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
//        }
//        return auth.getName();
//    }
//}



package com.example.demo.controller;

import com.example.demo.dto.SubscriptionPurchaseDto;
import com.example.demo.service.UserSubscriptionService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User-facing subscription purchase API (login required). Always scoped to the logged-in user.
 * Auth: Authorization: Bearer <token from POST /api/auth/login>  (same as cart / wishlist)
 */
@RestController
@RequestMapping("/api/v1/user/subscriptions")
public class UserSubscriptionController {

    private final UserSubscriptionService service;
    private final SuperAdminUtil util;      // only for getClientIp(...)
    private final UserAuthUtil userAuth;    // same util the cart + wishlist use

    public UserSubscriptionController(UserSubscriptionService service,
                                      SuperAdminUtil util,
                                      UserAuthUtil userAuth) {
        this.service = service;
        this.util = util;
        this.userAuth = userAuth;
    }

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.InitiateResponse>> purchase(
            @Valid @RequestBody SubscriptionPurchaseDto.PurchaseRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated",
                service.purchase(username, request, util.getClientIp(http)), 200));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SubscriptionPurchaseDto.UserPurchaseResponse>>> my(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Purchases fetched", service.myPurchases(username), 200));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.UserPurchaseResponse>> current(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        SubscriptionPurchaseDto.UserPurchaseResponse cur = service.myCurrent(username);
        return ResponseEntity.ok(ApiResponse.success(
                cur == null ? "No active subscription" : "Active subscription fetched", cur, 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<SubscriptionPurchaseDto.UserPurchaseResponse>> status(
            @PathVariable String orderId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Purchase fetched", service.myPurchase(username, orderId), 200));
    }
}