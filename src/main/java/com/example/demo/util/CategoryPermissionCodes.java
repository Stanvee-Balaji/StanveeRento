package com.example.demo.util;

/**
 * Permission codes for the Category Management module.
 *
 * Same rules as ProductPermissionCodes / PlanPermissionCodes:
 * - These are NOT hardcoded/enforced by the backend — they are plain string
 *   keys compared against the `permissions.code` column at runtime.
 * - A code only "exists" once a matching row is created in the `permissions`
 *   table (Super Admin → Modules & Permissions), under a Module whose code
 *   equals MODULE_CODE below.
 * - A code only "grants" access once it is attached to a Role
 *   (Super Admin → Roles → Add/Edit Role → Permissions).
 * - Spelling MUST match the `code` column exactly (case-insensitive in the
 *   DB, but keep it consistent here) or AuthorizationService.checkPermission
 *   will 403 every time, regardless of role setup.
 *
 * Activation checklist (do this in the UI before wiring a new endpoint):
 *   1. Modules screen  → create / confirm module with code = "CATEGORY".
 *   2. Permissions screen → create a permission row for each constant below,
 *      linked to that module, and mark it Active.
 *   3. Roles screen → attach the needed permission(s) to Sub-Admin role(s).
 *   4. Only then will AdminCategoryController honour them.
 *
 * Design note — two fine-grained write codes:
 *   - CATEGORY_EDIT   covers rename + active-toggle (low-risk changes).
 *   - CATEGORY_DELETE covers soft-delete (higher-risk; assign sparingly).
 *   Keeping delete separate lets you give a content editor EDIT without
 *   giving them the ability to wipe out a category that still has products.
 */
public final class CategoryPermissionCodes {
    private CategoryPermissionCodes() {}

    public static final String MODULE_CODE = "CATEGORY";

    public static final String CATEGORY_VIEW   = "CATEGOR_VIEW";
    public static final String CATEGORY_CREATE = "CATEGORY_CREATE";
    public static final String CATEGORY_EDIT   = "CATEGORY_EDIT";    // rename + active toggle
    public static final String CATEGORY_DELETE = "CATEGORY_DELETE";  // soft delete
}
