package com.example.demo.util;

/**
 * Permission codes for the Subscription PURCHASE management module (admin side).
 *
 * These are plain string keys. Each one only works after a matching row exists in the
 * `permissions` table (code column), created via Super Admin -> Modules & Permissions,
 * and is then ticked on a Role. Spelling must match the UI `code` exactly (case-insensitive),
 * otherwise checkPermission(...) never matches and every call returns 403.
 *
 * Create in the UI:
 *   Module      : code = "SUBP"  (name: "SUBSCRIPTION PURCHASES")
 *   Permissions : SUBP_VIEW, SUBP_CONFIRM, SUBP_REJECT, SUBP_REVOKE, SUBP_REFUND
 */
public final class PurchasePermissionCodes {

    private PurchasePermissionCodes() {}

    public static final String MODULE_CODE = "SUBP";

    public static final String PURCHASE_VIEW    = "SUBP_VIEW";     // list, detail, stats, stuck, audit
    public static final String PURCHASE_CONFIRM = "SUBP_CONFIRM";  // manually activate after checking Airpay dashboard
    public static final String PURCHASE_REJECT  = "SUBP_REJECT";   // mark failed
    public static final String PURCHASE_REVOKE  = "SUBP_REVOKE";   // remove access
    public static final String PURCHASE_REFUND  = "SUBP_REFUND";   // record a refund
}