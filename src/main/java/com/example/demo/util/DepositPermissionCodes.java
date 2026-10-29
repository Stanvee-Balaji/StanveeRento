package com.example.demo.util;

/** Insert these 3 codes in your permissions table (see sql/deposit_tables.sql) and assign them to roles. */
public final class DepositPermissionCodes {
    private DepositPermissionCodes() {}
    public static final String DEPOSIT_VIEW   = "DEP_VIEW";
    public static final String DEPOSIT_DEDUCT = "DEP_DEDUCT";
    public static final String DEPOSIT_REFUND = "DEP_REFUND";
}
