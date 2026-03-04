package com.consentledger.domain.consent.dto;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.entity.ConsentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ConsentResponse {

    private final UUID id;
    private final UUID userId;
    private final UUID agentId;
    private final UUID dataHolderId;
    private final String dataHolderName;
    private final List<String> scopes;
    private final ConsentStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ConsentResponse from(Consent consent) {
        return ConsentResponse.builder()
                .id(consent.getId())
                .userId(consent.getUser().getId())
                .agentId(consent.getAgent() != null ? consent.getAgent().getId() : null)
                .dataHolderId(consent.getDataHolder().getId())
                .dataHolderName(consent.getDataHolder().getName())
                .scopes(consent.getScopes())
                .status(consent.getEffectiveStatus())
                .expiresAt(consent.getExpiresAt())
                .createdAt(consent.getCreatedAt())
                .updatedAt(consent.getUpdatedAt())
                .build();
    }
}
