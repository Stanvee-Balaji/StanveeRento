package com.example.demo.util;

/**
 * Permission codes for the Rental Calendar & Availability module.
 * Same activation rules as ProductPermissionCodes:
 *   1. Modules screen: module code = MODULE_CODE must exist & be active
 *      (seeded by the migration).
 *   2. Permissions screen: rows with these codes must exist & be active
 *      (seeded by the migration).
 *   3. Roles screen: attach to whichever Sub-Admin roles need calendar access.
 *   4. Only then do these codes actually grant anything —
 *      AuthorizationService.checkPermission(...) re-checks live every call.
 */
public final class CalendarPermissionCodes {
    private CalendarPermissionCodes() {}

    public static final String MODULE_CODE = "RENTAL_CALENDAR";

    public static final String CALENDAR_VIEW     = "CALENDAR_VIEW";
    public static final String CALENDAR_BLOCK    = "CALENDAR_BLOCK";
    public static final String CALENDAR_UNBLOCK  = "CALENDAR_UNBLOCK";
    public static final String CALENDAR_CONFIG   = "CALENDAR_CONFIG";
}
