package com.consentledger.domain.admin.service;

import com.consentledger.domain.admin.dto.AgentStatusUpdateRequest;
import com.consentledger.domain.admin.dto.AgentSummary;
import com.consentledger.domain.admin.dto.UserSummary;
import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.domain.user.repository.UserRepository;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(UserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSummary> listAgents() {
        return agentRepository.findAll().stream()
                .map(AgentSummary::from)
                .toList();
    }

    @Transactional
    public AgentSummary updateAgentStatus(UUID agentId, AgentStatusUpdateRequest request) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));
        agent.changeStatus(request.getStatus());
        return AgentSummary.from(agent);
    }
}
