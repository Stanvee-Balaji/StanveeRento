package com.example.demo.util;

/**
 * Permission codes for the Inventory Management (dashboard / reports) module.
 *
 * Same activation rules as ProductPermissionCodes / CalendarPermissionCodes:
 *   1. Modules screen     -> create / confirm module with code = MODULE_CODE and mark Active.
 *   2. Permissions screen -> create one row per constant below, linked to that module, Active.
 *   3. Roles screen       -> attach the needed codes to Sub-Admin roles.
 *   4. AuthorizationService.checkPermission(...) re-checks the live chain on every call.
 *
 * NOTE: this module is READ-ONLY. Blocking / unblocking units stays under the
 * existing CALENDAR_BLOCK / CALENDAR_UNBLOCK codes; editing units stays under
 * PRODUCT_MANAGE_INVENTORY. Nothing is duplicated here.
 */
public final class InventoryPermissionCodes {
    private InventoryPermissionCodes() {}

    public static final String MODULE_CODE = "INVENTORY";

    public static final String INVENTORY_VIEW   = "INVENTORY_VIEW";    // dashboard, lists, details
    public static final String INVENTORY_EXPORT = "INVENTORY_EXPORT";  // CSV export
}
