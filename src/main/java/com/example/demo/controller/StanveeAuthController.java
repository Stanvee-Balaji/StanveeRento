package com.example.demo.controller;

import com.example.demo.dto.StanveeAuthDTO.LoginRequest;
import com.example.demo.dto.StanveeAuthDTO.LoginResult;
import com.example.demo.service.StanveeAuthService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class StanveeAuthController {

    private final StanveeAuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public StanveeAuthController(StanveeAuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * Body: { "username": "SV6614168", "password": "976791" }
     *
     * Success response:
     * {
     *   "token": "eyJ...",
     *   "data": { "loginid": "SV6614168", "name": "...", "rwallet": "...", ... },
     *   "status": "SUCCESS",
     *   "response": "OK"
     * }
     *
     * Failure response (wrong credentials):
     * {
     *   "token": null,
     *   ...whatever Stanvee returned...
     * }
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        LoginResult result = authService.login(req);

        try {
            // Parse Stanvee's raw JSON into an ObjectNode so we can inject token
            ObjectNode root = (ObjectNode) mapper.readTree(result.getRawJson());

            // Inject token at the TOP of the response (null if login failed)
            if (result.getJwtToken() != null) {
                root.put("token", result.getJwtToken());
            } else {
                root.putNull("token");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(root));

        } catch (Exception e) {
            // Stanvee returned non-JSON — return as-is, no token injection
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.getRawJson());
        }
    }
}