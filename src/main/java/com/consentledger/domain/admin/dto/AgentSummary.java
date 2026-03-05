package com.consentledger.domain.admin.dto;

import com.consentledger.domain.agent.entity.Agent;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AgentSummary {
    private UUID id;
    private String name;
    private String status;
    private String clientId;
    private Instant lastRotatedAt;
    private Instant createdAt;

    public static AgentSummary from(Agent agent) {
        return AgentSummary.builder()
                .id(agent.getId())
                .name(agent.getName())
                .status(agent.getStatus().name())
                .clientId(agent.getClientId())
                .lastRotatedAt(agent.getLastRotatedAt())
                .createdAt(agent.getCreatedAt())
                .build();
    }
}
