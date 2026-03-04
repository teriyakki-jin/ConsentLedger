package com.consentledger.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "Invalid or expired token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_003", "Access denied"),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "AUTH_004", "Invalid or missing API key"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "Email already exists"),

    // Agent
    AGENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AGENT_001", "Agent not found"),
    AGENT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "AGENT_002", "Agent is not active"),
    AGENT_POLICY_VIOLATION(HttpStatus.FORBIDDEN, "AGENT_003", "Agent is not authorized for this operation"),

    // DataHolder
    DATA_HOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "DH_001", "Data holder not found"),
    DATA_HOLDER_METHOD_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY, "DH_002", "Transfer method not supported by data holder"),

    // Consent
    CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONSENT_001", "Consent not found"),
    CONSENT_NOT_ACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "CONSENT_002", "Consent is not active or has expired"),
    CONSENT_ALREADY_REVOKED(HttpStatus.CONFLICT, "CONSENT_003", "Consent is already revoked"),
    CONSENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CONSENT_004", "You do not have permission to access this consent"),

    // Transfer
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSFER_001", "Transfer request not found"),
    TRANSFER_INVALID_TRANSITION(HttpStatus.CONFLICT, "TRANSFER_002", "Invalid state transition"),
    TRANSFER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TRANSFER_003", "You do not have permission to access this transfer request"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "TRANSFER_004", "Duplicate idempotency key with different parameters"),

    // Audit
    AUDIT_CHAIN_BROKEN(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIT_001", "Audit log chain integrity violated"),

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON_001", "Validation failed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "Internal server error");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
