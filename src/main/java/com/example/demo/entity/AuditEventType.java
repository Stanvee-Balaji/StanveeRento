package com.example.demo.entity;

public enum AuditEventType {
    TOKEN_REQUEST, TOKEN_RESPONSE, PAYMENT_INIT_RESPONSE,
    CALLBACK_PROCESSED,      // Airpay IPN resolved (transaction level)
    SUBSCRIPTION_EVENT,      // purchase lifecycle (created / activated / failed / expired ...)
    ADMIN_ACTION,            // manual confirm / reject / revoke / refund (before + after in payload)
    ERROR
}
