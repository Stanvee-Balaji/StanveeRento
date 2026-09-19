package com.example.demo.controller;

import com.example.demo.dto.AdminDto;
import com.example.demo.service.AdminService;
import com.example.demo.service.AuthorizationService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.SuperAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



// New: Sub-Admins now authenticate independently (roleType=ADMIN in the JWT).
// Every subsequent Admin API call is still re-authorized against the DB via
// AuthorizationService / @RequiresPermission — the JWT only proves identity.
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthorizationService authorizationService;
    private final SuperAdminUtil util; // reused only for getClientIp(...)

    public AdminController(AdminService adminService, AuthorizationService authorizationService, SuperAdminUtil util) {
        this.adminService = adminService;
        this.authorizationService = authorizationService;
        this.util = util;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminDto.LoginResponse>> login(
            @Valid @RequestBody AdminDto.LoginRequest request, HttpServletRequest http) {

        return ResponseEntity.ok(ApiResponse.success(
                "Login successful", adminService.login(request, util.getClientIp(http)), 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminDto.AdminResponse>> me(HttpServletRequest http) {

        var adminId = authorizationService.getAuthenticatedAdminId(http);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", adminService.get(adminId), 200));
    }
}




