package com.consentledger.domain.consent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class ConsentCreateRequest {

    @NotNull(message = "Data holder ID is required")
    private UUID dataHolderId;

    private UUID agentId;

    @NotEmpty(message = "At least one scope is required")
    private List<String> scopes;

    private Instant expiresAt;
}
