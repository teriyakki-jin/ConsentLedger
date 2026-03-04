package com.consentledger.domain.audit.dto;

import com.consentledger.domain.audit.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private final Long id;
    private final Instant ts;
    private final String action;
    private final String objectType;
    private final UUID objectId;
    private final UUID actorUserId;
    private final UUID actorAgentId;
    private final String outcome;
    private final String payloadHash;
    private final String prevHash;
    private final String hash;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .ts(log.getTs())
                .action(log.getAction())
                .objectType(log.getObjectType())
                .objectId(log.getObjectId())
                .actorUserId(log.getActorUserId())
                .actorAgentId(log.getActorAgentId())
                .outcome(log.getOutcome())
                .payloadHash(log.getPayloadHash())
                .prevHash(log.getPrevHash())
                .hash(log.getHash())
                .build();
    }
}
