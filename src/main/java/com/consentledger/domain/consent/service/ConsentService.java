package com.consentledger.domain.consent.service;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.domain.agent.service.AgentPolicyValidator;
import com.consentledger.domain.audit.service.AuditLogService;
import com.consentledger.domain.consent.dto.ConsentCreateRequest;
import com.consentledger.domain.consent.dto.ConsentResponse;
import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.entity.ConsentStatus;
import com.consentledger.domain.consent.repository.ConsentRepository;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.dataholder.repository.DataHolderRepository;
import com.consentledger.domain.user.entity.User;
import com.consentledger.domain.user.repository.UserRepository;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final DataHolderRepository dataHolderRepository;
    private final AgentPolicyValidator agentPolicyValidator;
    private final AuditLogService auditLogService;

    @Transactional
    public ConsentResponse create(ConsentCreateRequest request) {
        UUID actorUserId = null;
        UUID actorAgentId = null;
        User user;

        if (SecurityUtils.isAgent()) {
            Agent agent = SecurityUtils.getCurrentAgent()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_API_KEY));
            actorAgentId = agent.getId();

            // Agent가 생성하는 경우 agentId가 자신이어야 함
            if (request.getAgentId() != null && !request.getAgentId().equals(agent.getId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }

            // Agent 정책 검증 (scopes를 method 대신 사용하거나, 범용 검증)
            agentPolicyValidator.validate(agent.getId(), "PUSH");

            // Agent가 동의를 생성할 때는 관련 사용자 정보 필요
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Agents cannot create consents directly. User must create consent.");
        } else {
            actorUserId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
            user = userRepository.findById(actorUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        DataHolder dataHolder = dataHolderRepository.findById(request.getDataHolderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_HOLDER_NOT_FOUND));

        Agent agent = null;
        if (request.getAgentId() != null) {
            agent = agentRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));
        }

        Consent consent = Consent.builder()
                .user(user)
                .agent(agent)
                .dataHolder(dataHolder)
                .scopes(request.getScopes())
                .status(ConsentStatus.ACTIVE)
                .expiresAt(request.getExpiresAt())
                .build();

        Consent saved = consentRepository.save(consent);

        auditLogService.log("CONSENT_CREATED", "CONSENT", saved.getId(),
                actorUserId, null,
                Map.of("dataHolderId", dataHolder.getId().toString(),
                        "scopes", request.getScopes()));

        return ConsentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> listMyConsents() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        return consentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ConsentResponse::from)
                .toList();
    }

    @Transactional
    public ConsentResponse revoke(UUID consentId) {
        UUID actorUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        Consent consent = consentRepository.findByIdAndUserId(consentId, actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSENT_NOT_FOUND));

        if (consent.getStatus() == ConsentStatus.REVOKED) {
            throw new BusinessException(ErrorCode.CONSENT_ALREADY_REVOKED);
        }

        consent.revoke();
        Consent saved = consentRepository.save(consent);

        auditLogService.log("CONSENT_REVOKED", "CONSENT", saved.getId(),
                actorUserId, null,
                Map.of("consentId", consentId.toString()));

        return ConsentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ConsentResponse getById(UUID consentId) {
        UUID actorUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        Consent consent = consentRepository.findByIdAndUserId(consentId, actorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSENT_NOT_FOUND));

        return ConsentResponse.from(consent);
    }
}
