package com.example.demo.util;

/** Permission codes for the Rental Order module (seeded by rental_orders.sql). */
public final class RentalOrderPermissionCodes {
    private RentalOrderPermissionCodes() {}

    public static final String ORDER_VIEW    = "ORDR_VIEW";
    public static final String ORDER_CONFIRM = "ORDR_CONFIRM";
    public static final String ORDER_REJECT  = "ORDR_REJECT";
    public static final String ORDER_CANCEL  = "ORDR_CANCEL";
    public static final String ORDER_FULFIL  = "ORDR_FULFIL";
    public static final String ORDER_REFUND  = "ORDR_REFUND";
    public static final String ORDER_CONFIG  = "ORDR_CONFIG";
}
