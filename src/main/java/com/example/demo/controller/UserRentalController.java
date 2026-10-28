package com.example.demo.controller;

import com.example.demo.dto.RentalOrderDto.*;
import com.example.demo.service.UserRentalService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * User-facing rental booking API (login required, always scoped to the logged-in user).
 * Auth: Authorization: Bearer <token from POST /api/auth/login>  (same as cart / wishlist / subscriptions)
 *
 *  GET   /api/v1/user/rentals/config                 delivery fee rules etc.
 *  GET   /api/v1/user/rentals/availability           per-day free units for a size
 *  POST  /api/v1/user/rentals/summary                price + eligibility preview (creates nothing)
 *  POST  /api/v1/user/rentals/book                   place the booking (+ Airpay payload when something is payable)
 *  GET   /api/v1/user/rentals/my                     my bookings
 *  GET   /api/v1/user/rentals/{orderId}              one booking (also the status poll after payment)
 *  POST  /api/v1/user/rentals/{orderId}/cancel       cancel
 */
@RestController
@RequestMapping("/api/v1/user/rentals")
public class UserRentalController {

    private final UserRentalService service;
    private final SuperAdminUtil util;     // only for getClientIp(...)
    private final UserAuthUtil userAuth;

    public UserRentalController(UserRentalService service, SuperAdminUtil util, UserAuthUtil userAuth) {
        this.service = service;
        this.util = util;
        this.userAuth = userAuth;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ConfigResponse>> config(HttpServletRequest http) {
        userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Booking config fetched", service.config(), 200));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> availability(
            @RequestParam UUID productId, @RequestParam String size, @RequestParam(required = false) String colour,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, HttpServletRequest http) {
        userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Availability fetched", service.availability(productId, size, colour, from, to), 200));
    }

    @PostMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> summary(
            @Valid @RequestBody OrderRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Summary calculated", service.summary(username, request), 200));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<BookResponse>> book(
            @Valid @RequestBody OrderRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        BookResponse r = service.book(username, request, util.getClientIp(http));
        return ResponseEntity.ok(ApiResponse.success(r.paymentRequired() ? "Booking created, payment pending" : "Booking confirmed", r, 200));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResult<OrderResponse>>> my(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched", service.my(username, status, page, size), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> detail(@PathVariable String orderId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Booking fetched", service.myOrder(username, orderId), 200));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest body, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled",
                service.cancel(username, orderId, body == null ? null : body.note()), 200));
    }

    /** POST /api/v1/user/rentals/{orderId}/return-request  body (optional): { "note": "..." } */
    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @PathVariable String orderId, @RequestBody(required = false) NoteRequest body, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Return requested",
                service.requestReturn(username, orderId, body == null ? null : body.note()), 200));
    }

}
