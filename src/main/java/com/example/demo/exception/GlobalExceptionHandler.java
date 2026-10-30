


package com.example.demo.exception;

import com.example.demo.service.SuperAdminService;
import com.example.demo.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.demo.service.ProductAccessService;
import com.example.demo.service.SuperAdminService;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place that turns every exception into a proper ApiResponse.
 *
 * The 500 handler behaves differently per Spring profile:
 *   - dev  → returns the real exception class + message so you can debug without
 *             opening the server console every time.
 *   - prod → returns only the correlation ID (safe, non-leaky). Search that ID
 *             in your log output to find the full stack trace.
 *
 * Switch profiles in application.properties / application.yml:
 *   spring.profiles.active=dev     ← local / staging
 *   spring.profiles.active=prod    ← production
 *
 * If spring.profiles.active is not set at all, the handler falls back to "dev"
 * behaviour so you see errors immediately out of the box.
 */



@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Injected from spring.profiles.active.
     * Defaults to "dev" when no profile is set so new projects see real errors immediately.
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // ── 401 ─────────────────────────────────────

    @ExceptionHandler(SuperAdminService.UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(SuperAdminService.UnauthorizedException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage(), null, 401));
    }

    // ── 403 ─────────────────────────────────────

    @ExceptionHandler(SuperAdminService.ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(SuperAdminService.ForbiddenException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage(), null, 403));
    }

    
    // ── 404 ─────────────────────────────────────
    
    @ExceptionHandler(ProductAccessService.PlanAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanAccess(ProductAccessService.PlanAccessDeniedException e) {
        log.warn("Plan access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage(), null, 403));
    }

    // ── 404 ─────────────────────────────────────

    @ExceptionHandler(SuperAdminService.ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(SuperAdminService.ResourceNotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage(), null, 404));
    }

    // ── 400 (business rule) ──────────────────────

    @ExceptionHandler(SuperAdminService.BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(SuperAdminService.BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), null, 400));
    }

    // ── 400 (@Valid failures) ────────────────────
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), null, 400));
    }

    // ── 400 (@Valid failures) ────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", fieldErrors, 400));
    }

    // ── 500 (unexpected) ─────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        String correlationId = java.util.UUID.randomUUID().toString();
        log.error("Unhandled exception [{}]: {}", correlationId, e.getMessage(), e);

        boolean isDev = !"prod".equalsIgnoreCase(activeProfile);

        if (isDev) {
            // ── DEV: expose the real error so you don't have to grep logs ──
            Map<String, String> detail = new LinkedHashMap<>();
            detail.put("ref",       correlationId);
            detail.put("exception", e.getClass().getName());
            detail.put("message",   e.getMessage() != null ? e.getMessage() : "(no message)");

            // Include the root cause if it differs from the top-level exception
            Throwable cause = getRootCause(e);
            if (cause != e) {
                detail.put("causedBy",      cause.getClass().getName());
                detail.put("causeMessage",  cause.getMessage() != null ? cause.getMessage() : "(no message)");
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error [dev]", detail, 500));
        }

        // ── PROD: safe, non-leaky response ──
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Something went wrong. Reference: " + correlationId, null, 500));
    }

    // ── helpers ──────────────────────────────────

    private Throwable getRootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}