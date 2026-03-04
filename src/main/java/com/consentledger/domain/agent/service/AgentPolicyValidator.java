package com.consentledger.domain.agent.service;

import com.consentledger.domain.agent.entity.AgentAgreement;
import com.consentledger.domain.agent.entity.AgentStatus;
import com.consentledger.domain.agent.repository.AgentAgreementRepository;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentPolicyValidator {

    private final AgentRepository agentRepository;
    private final AgentAgreementRepository agentAgreementRepository;

    public void validate(UUID agentId, String method) {
        agentRepository.findById(agentId)
                .filter(a -> a.getStatus() == AgentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_ACTIVE));

        List<AgentAgreement> validAgreements = agentAgreementRepository.findValidAgreements(agentId, Instant.now());

        boolean authorized = validAgreements.stream()
                .anyMatch(agreement -> agreement.getAllowedMethods() != null
                        && agreement.getAllowedMethods().contains(method));

        if (!authorized) {
            throw new BusinessException(ErrorCode.AGENT_POLICY_VIOLATION,
                    "Agent is not authorized for method: " + method);
        }
    }
}
