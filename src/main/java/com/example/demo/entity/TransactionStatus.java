package com.example.demo.entity;

/**
 * Lifecycle states for a Transaction row.
 *
 *   INITIATED        → request received, about to call Airpay's token API
 *   TOKEN_FAILED      → Airpay rejected the token request, or token API errored
 *   AWAITING_PAYMENT  → token obtained, buyer redirected to Airpay's iframe
 *   SUCCESS           → callback received, transaction_status = 200, hash valid
 *   FAILED            → callback received, transaction_status != 200/402
 *   CANCELLED         → callback received, transaction_status = 402 (buyer cancelled)
 *   HASH_MISMATCH     → callback received but ap_securehash did not match — treat as untrusted
 *   ERROR             → unexpected error during initiate or callback processing
 */
public enum TransactionStatus {
    INITIATED,
    TOKEN_FAILED,
    AWAITING_PAYMENT,
    SUCCESS,
    FAILED,
    CANCELLED,
    HASH_MISMATCH,
    ERROR
}