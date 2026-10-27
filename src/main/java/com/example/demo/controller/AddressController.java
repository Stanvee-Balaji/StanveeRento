package com.example.demo.controller;

import com.example.demo.dto.AddressDto;
import com.example.demo.service.AddressService;
import com.example.demo.util.ApiResponse;
import com.example.demo.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Address API for logged-in Stanvee users.
 * Every call needs:  Authorization: Bearer <token from POST /api/auth/login>
 *
 *  GET    /api/v1/addresses                       → list my addresses (default first)
 *  GET    /api/v1/addresses/{addressId}           → get one address
 *  POST   /api/v1/addresses/add                   → save new address
 *  PUT    /api/v1/addresses/update/{addressId}    → update address
 *  PUT    /api/v1/addresses/default/{addressId}   → mark as default
 *  DELETE /api/v1/addresses/remove/{addressId}    → delete address
 */
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserAuthUtil userAuth;

    public AddressController(AddressService addressService, UserAuthUtil userAuth) {
        this.addressService = addressService;
        this.userAuth = userAuth;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AddressDto.AddressListResponse>> list(HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addressService.list(username), 200));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDto.AddressResponse>> get(
            @PathVariable UUID addressId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Address fetched", addressService.get(username, addressId), 200));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<AddressDto.AddressResponse>> add(
            @Valid @RequestBody AddressDto.AddressRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Address saved", addressService.add(username, request), 200));
    }

    @PutMapping("/update/{addressId}")
    public ResponseEntity<ApiResponse<AddressDto.AddressResponse>> update(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressDto.AddressRequest request, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.update(username, addressId, request), 200));
    }

    @PutMapping("/default/{addressId}")
    public ResponseEntity<ApiResponse<AddressDto.AddressListResponse>> setDefault(
            @PathVariable UUID addressId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Default address updated", addressService.setDefault(username, addressId), 200));
    }

    @DeleteMapping("/remove/{addressId}")
    public ResponseEntity<ApiResponse<AddressDto.AddressListResponse>> remove(
            @PathVariable UUID addressId, HttpServletRequest http) {
        String username = userAuth.requireUsername(http);
        return ResponseEntity.ok(ApiResponse.success("Address removed", addressService.remove(username, addressId), 200));
    }
}
