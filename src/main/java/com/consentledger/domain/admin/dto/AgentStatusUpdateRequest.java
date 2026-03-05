package com.consentledger.domain.admin.dto;

import com.consentledger.domain.agent.entity.AgentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AgentStatusUpdateRequest {
    @NotNull
    private AgentStatus status;
}
