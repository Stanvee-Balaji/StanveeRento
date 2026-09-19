package com.example.demo.util;

/**
 * Permission codes for the Product Manager module.
 *
 * Same rules as PlanPermissionCodes:
 * - These are NOT hardcoded/enforced by the backend — plain string keys.
 * - A code only "exists" once a matching row is created in the
 *   `permissions` table (via Super Admin -> Modules & Permissions screen),
 *   under a Module whose code matches MODULE_CODE below.
 * - A code only "grants" access once it is attached to a Role
 *   (Super Admin -> Roles -> Add/Edit Role -> Permissions screen).
 * - Spelling MUST match the `code` column exactly (case-insensitive) or
 *   AuthorizationService.checkPermission(...) will 403 every time,
 *   regardless of role setup.
 *
 * Steps to activate in the UI before wiring a new endpoint to a code below:
 *   1. Modules screen: create/confirm module with code = MODULE_CODE.
 *   2. Permissions screen: create a permission row with code = the constant
 *      value, under that module, and mark it Active.
 *   3. Roles screen: attach that permission to whichever Sub-Admin role(s)
 *      should have it.
 *   4. Only then use the constant in AdminProductController.
 */


public final class ProductPermissionCodes {
    private ProductPermissionCodes() {}

    public static final String MODULE_CODE = "PRODUCT";

    public static final String PRODUCT_VIEW              = "PRODUCT_VIEW";
    public static final String PRODUCT_CREATE            = "PRODUCT_CREATE";
    public static final String PRODUCT_EDIT              = "PRODUCT_EDIT";
    public static final String PRODUCT_DELETE            = "PRODUCT_DELETE";
    public static final String PRODUCT_MANAGE_IMAGES     = "PRODUCT_MANAGE_IMAGES";
    public static final String PRODUCT_MANAGE_INVENTORY  = "PRODUCT_MANAGE_INVENTORY";
    public static final String PRODUCT_BULK_UPLOAD = "PRODUCT_BULK_UPLOAD";

    // Category management is small enough that most teams gate it behind
    // PRODUCT_EDIT rather than issuing separate codes. If you want it
    // separated, add CATEGORY_CREATE / CATEGORY_EDIT / CATEGORY_DELETE here
    // and create matching rows in the UI the same way as above.
}
