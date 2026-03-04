package com.consentledger.global.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final List<FieldError> errors;
    private final Instant timestamp = Instant.now();

    private ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
