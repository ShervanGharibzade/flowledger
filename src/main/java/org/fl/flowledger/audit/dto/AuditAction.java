package org.fl.flowledger.audit.dto;

public enum AuditAction {
    LOGIN,
    LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    WALLET_CREATED,
    WALLET_UPDATED,
    TRANSFER_CREATED,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED
}