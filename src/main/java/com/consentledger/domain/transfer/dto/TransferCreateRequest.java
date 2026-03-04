package com.consentledger.domain.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TransferCreateRequest {

    @NotNull(message = "Consent ID is required")
    private UUID consentId;

    @NotBlank(message = "Transfer method is required")
    private String method;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
