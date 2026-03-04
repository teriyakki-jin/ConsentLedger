package com.consentledger.domain.transfer.dto;

import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TransferResponse {

    private final UUID id;
    private final UUID consentId;
    private final String method;
    private final TransferStatus status;
    private final UUID requesterUserId;
    private final UUID requesterAgentId;
    private final UUID approvedByUserId;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static TransferResponse from(TransferRequest tr) {
        return TransferResponse.builder()
                .id(tr.getId())
                .consentId(tr.getConsent().getId())
                .method(tr.getMethod())
                .status(tr.getStatus())
                .requesterUserId(tr.getRequesterUser() != null ? tr.getRequesterUser().getId() : null)
                .requesterAgentId(tr.getRequesterAgent() != null ? tr.getRequesterAgent().getId() : null)
                .approvedByUserId(tr.getApprovedByUser() != null ? tr.getApprovedByUser().getId() : null)
                .idempotencyKey(tr.getIdempotencyKey())
                .createdAt(tr.getCreatedAt())
                .updatedAt(tr.getUpdatedAt())
                .build();
    }
}
