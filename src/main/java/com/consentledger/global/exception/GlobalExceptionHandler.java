package com.consentledger.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, fieldErrors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_TOKEN.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_TOKEN));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
