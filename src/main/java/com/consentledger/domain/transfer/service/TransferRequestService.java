package com.consentledger.domain.transfer.service;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.service.AgentPolicyValidator;
import com.consentledger.domain.audit.service.AuditLogService;
import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.repository.ConsentRepository;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.transfer.dto.TransferCreateRequest;
import com.consentledger.domain.transfer.dto.TransferResponse;
import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import com.consentledger.domain.transfer.repository.TransferRequestRepository;
import com.consentledger.domain.user.entity.User;
import com.consentledger.domain.user.repository.UserRepository;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferRequestService {

    private final TransferRequestRepository transferRepository;
    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AgentPolicyValidator agentPolicyValidator;
    private final TransferStateMachine stateMachine;
    private final AuditLogService auditLogService;

    @Transactional
    public TransferResponse create(TransferCreateRequest request) {
        // Idempotency 키 중복 확인
        Optional<TransferRequest> existing = transferRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return TransferResponse.from(existing.get());
        }

        Consent consent = consentRepository.findActiveById(request.getConsentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSENT_NOT_ACTIVE));

        if (!consent.isActive()) {
            throw new BusinessException(ErrorCode.CONSENT_NOT_ACTIVE);
        }

        DataHolder dataHolder = consent.getDataHolder();
        if (!dataHolder.supportsMethod(request.getMethod())) {
            throw new BusinessException(ErrorCode.DATA_HOLDER_METHOD_NOT_SUPPORTED,
                    "Data holder does not support method: " + request.getMethod());
        }

        UUID actorUserId = null;
        UUID actorAgentId = null;
        User requesterUser = null;
        Agent requesterAgent = null;

        if (SecurityUtils.isAgent()) {
            Agent agent = SecurityUtils.getCurrentAgent()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_API_KEY));
            agentPolicyValidator.validate(agent.getId(), request.getMethod());
            actorAgentId = agent.getId();
            requesterAgent = agent;
        } else {
            UUID userId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
            if (!consent.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.TRANSFER_ACCESS_DENIED);
            }
            actorUserId = userId;
            requesterUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        try {
            TransferRequest transferRequest = TransferRequest.builder()
                    .consent(consent)
                    .method(request.getMethod())
                    .status(TransferStatus.REQUESTED)
                    .requesterUser(requesterUser)
                    .requesterAgent(requesterAgent)
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            TransferRequest saved = transferRepository.save(transferRequest);

            auditLogService.log("TRANSFER_REQUESTED", "TRANSFER_REQUEST", saved.getId(),
                    actorUserId, actorAgentId,
                    Map.of("consentId", request.getConsentId().toString(),
                            "method", request.getMethod()));

            return TransferResponse.from(saved);

        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 unique 위반 시 새 트랜잭션에서 기존 레코드 반환
            return findByIdempotencyKeyInNewTransaction(request.getIdempotencyKey());
        }
    }

    /**
     * DataIntegrityViolationException 후 poisoned 트랜잭션을 피하기 위해
     * REQUIRES_NEW로 새 트랜잭션에서 조회한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public TransferResponse findByIdempotencyKeyInNewTransaction(String idempotencyKey) {
        return transferRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransferResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listMine() {
        if (SecurityUtils.isAgent()) {
            UUID agentId = SecurityUtils.getCurrentAgentId()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_API_KEY));
            return transferRepository.findByAgentId(agentId).stream()
                    .map(TransferResponse::from).toList();
        } else {
            UUID userId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
            return transferRepository.findByUserId(userId).stream()
                    .map(TransferResponse::from).toList();
        }
    }

    @Transactional
    public TransferResponse approve(UUID transferId) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Only authenticated users can approve transfers"));

        // 비관적 락으로 동시 승인 방지
        TransferRequest transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        if (!transfer.getConsent().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.TRANSFER_ACCESS_DENIED);
        }

        stateMachine.approve(transfer);

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        transfer.approve(approver);
        TransferRequest saved = transferRepository.save(transfer);

        auditLogService.log("TRANSFER_APPROVED", "TRANSFER_REQUEST", saved.getId(),
                userId, null,
                Map.of("transferId", transferId.toString()));

        return TransferResponse.from(saved);
    }

    @Transactional
    public TransferResponse execute(UUID transferId) {
        UUID actorUserId = SecurityUtils.getCurrentUserId().orElse(null);
        UUID actorAgentId = SecurityUtils.getCurrentAgentId().orElse(null);

        // 인증 정보가 없으면 조기 실패
        if (actorUserId == null && actorAgentId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Authentication required");
        }

        // 비관적 락으로 동시 실행 방지
        TransferRequest transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        boolean isOwner = actorUserId != null && transfer.getConsent().getUser().getId().equals(actorUserId);
        boolean isRequester = actorAgentId != null && transfer.getRequesterAgent() != null
                && transfer.getRequesterAgent().getId().equals(actorAgentId);

        if (!isOwner && !isRequester) {
            throw new BusinessException(ErrorCode.TRANSFER_ACCESS_DENIED);
        }

        stateMachine.complete(transfer);

        transfer.complete();
        TransferRequest saved = transferRepository.save(transfer);

        auditLogService.log("TRANSFER_EXECUTED", "TRANSFER_REQUEST", saved.getId(),
                actorUserId, actorAgentId,
                Map.of("transferId", transferId.toString(), "outcome", "SUCCESS"));

        return TransferResponse.from(saved);
    }
}
