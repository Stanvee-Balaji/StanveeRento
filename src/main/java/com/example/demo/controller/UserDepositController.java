package com.example.demo.controller;

import com.example.demo.dto.DepositDto.UserDepositResponse;
import com.example.demo.service.DepositService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only for the buyer. Always scoped to the logged-in user. Auth: Bearer token (same as subscriptions). */
@RestController
@RequestMapping("/api/v1/user/deposits")
public class UserDepositController {

    private final DepositService service;
    private final UserAuthUtil userAuth;

    public UserDepositController(DepositService service, UserAuthUtil userAuth) {
        this.service = service;
        this.userAuth = userAuth;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UserDepositResponse>>> my(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Deposits fetched", service.myDeposits(username), 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<UserDepositResponse>> one(@PathVariable String orderId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Deposit fetched", service.myDeposit(username, orderId), 200));
    }
}
