package com.example.demo.util;

/**
 * Permission codes for the Subscription Manager module.
 *
 * IMPORTANT: these are NOT hardcoded/enforced by the backend — they are
 * plain string keys. The actual permission only "exists" once a matching
 * row is created in the `permissions` table (code column) via the Super
 * Admin UI (Modules & Permissions screen), and only "grants" access once
 * that permission is checked on a Role (Add/Edit Role screen).
 *
 * These constants MUST be spelled exactly (case-insensitive) the same as
 * the `code` value you create in the UI, or checkPermission(...) will
 * never find a match and every call will 403 regardless of role setup.
 *
 * Current UI state (as of your last screenshots):
 *   modules.code    = "SUB"        (name: "SUBSCRIPTION MANGER")
 *   permissions.code = "SUB_VIEW"   (action: VIEW)
 *   permissions.code = "SUB_CREATE" (action: CREATE)
 *   -- SUB_EDIT / SUB_DELETE not created yet in the UI --
 *
 * If you add more permissions (edit/delete/manage features/manage limits)
 * via the UI, add the matching constant here too, and use it in the
 * controller. One file, one edit, instead of hunting through 7 methods.
 */
public final class PlanPermissionCodes {

    private PlanPermissionCodes() {}

//    public static final String MODULE_CODE = "SUBB";

//    public static final String PLAN_VIEW = "SUB_VIEW";
//    public static final String PLAN_CREATE = "SUB_CREATE";
//    public static final String PLAN_EDIT = "SUB_EDIT";              // create this permission in the UI before use
//    public static final String PLAN_MANAGE_FEATURES = "SUB_MANAGE_FEATURES"; // create this permission in the UI before use
//    public static final String PLAN_MANAGE_LIMITS = "SUB_MANAGE_LIMITS";     // create this permission in the UI before use
//    public static final String PLAN_STATUS_CHANGE = "SUB_STATUS_CHANGE";     // create this permission in the UI before use
//    public static final String PLAN_DELETE = "SUB_DELETE";       
//    
    
    
    public static final String MODULE_CODE = "SUB";
    public static final String PLAN_VIEW = "SUB_VIEW";
    public static final String PLAN_CREATE = "SUB_CREATE";
    public static final String PLAN_EDIT = "SUB_EDIT";       
    public static final String PLAN_DELETE = "SUB_DELETE";   
    
}
