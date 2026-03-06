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
import com.consentledger.fixture.AgentFixture;
import com.consentledger.fixture.ConsentFixture;
import com.consentledger.fixture.TransferRequestFixture;
import com.consentledger.fixture.UserFixture;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.global.security.ApiKeyAuthenticationToken;
import com.consentledger.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferRequestServiceTest {

    @Mock private TransferRequestRepository transferRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentPolicyValidator agentPolicyValidator;
    @Mock private TransferStateMachine stateMachine;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private TransferRequestService service;

    private User user;
    private DataHolder dataHolder;
    private Consent consent;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        dataHolder = ConsentFixture.dataHolder();
        consent = ConsentFixture.active(user, dataHolder);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setUserContext(User u) {
        CustomUserDetails userDetails = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));
    }

    private void setAgentContext(Agent agent) {
        SecurityContextHolder.getContext().setAuthentication(
                new ApiKeyAuthenticationToken(agent));
    }

    private TransferCreateRequest buildRequest(UUID consentId, String method, String idempotencyKey) {
        TransferCreateRequest req = new TransferCreateRequest();
        ReflectionTestUtils.setField(req, "consentId", consentId);
        ReflectionTestUtils.setField(req, "method", method);
        ReflectionTestUtils.setField(req, "idempotencyKey", idempotencyKey);
        return req;
    }

    // ── create() tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("새 멱등성 키로 전송 요청 생성 성공")
    void create_newIdempotencyKey_createsRequest() {
        setUserContext(user);
        String idempotencyKey = "key-" + UUID.randomUUID();
        TransferRequest transferRequest = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByIdempotencyKey(idempotencyKey)).willReturn(Optional.empty());
        given(consentRepository.findActiveById(consent.getId())).willReturn(Optional.of(consent));
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(transferRepository.save(any(TransferRequest.class))).willReturn(transferRequest);

        TransferResponse response = service.create(buildRequest(consent.getId(), "PULL", idempotencyKey));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.REQUESTED);
        verify(auditLogService).log(eq("TRANSFER_REQUESTED"), eq("TRANSFER_REQUEST"),
                any(), eq(user.getId()), any(), any());
    }

    @Test
    @DisplayName("기존 멱등성 키로 요청 시 기존 레코드 반환")
    void create_existingIdempotencyKey_returnsExisting() {
        setUserContext(user);
        String idempotencyKey = "existing-key";
        TransferRequest existing = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByIdempotencyKey(idempotencyKey))
                .willReturn(Optional.of(existing));

        TransferResponse response = service.create(buildRequest(consent.getId(), "PULL", idempotencyKey));

        assertThat(response.getIdempotencyKey()).isEqualTo(existing.getIdempotencyKey());
    }

    @Test
    @DisplayName("비활성 동의로 전송 요청 생성 시 CONSENT_NOT_ACTIVE")
    void create_inactiveConsent_throwsConsentNotActive() {
        setUserContext(user);
        String idempotencyKey = "key-" + UUID.randomUUID();

        given(transferRepository.findByIdempotencyKey(idempotencyKey)).willReturn(Optional.empty());
        given(consentRepository.findActiveById(consent.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.create(buildRequest(consent.getId(), "PULL", idempotencyKey)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONSENT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("지원하지 않는 메서드로 요청 시 DATA_HOLDER_METHOD_NOT_SUPPORTED")
    void create_unsupportedMethod_throws() {
        setUserContext(user);
        String idempotencyKey = "key-" + UUID.randomUUID();

        given(transferRepository.findByIdempotencyKey(idempotencyKey)).willReturn(Optional.empty());
        given(consentRepository.findActiveById(consent.getId())).willReturn(Optional.of(consent));

        assertThatThrownBy(() ->
                service.create(buildRequest(consent.getId(), "UNSUPPORTED_METHOD", idempotencyKey)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_HOLDER_METHOD_NOT_SUPPORTED);
    }

    @Test
    @DisplayName("동의 소유자가 아닌 사용자의 요청 시 TRANSFER_ACCESS_DENIED")
    void create_byNonOwnerUser_throwsAccessDenied() {
        User otherUser = UserFixture.user();
        setUserContext(otherUser);
        String idempotencyKey = "key-" + UUID.randomUUID();

        given(transferRepository.findByIdempotencyKey(idempotencyKey)).willReturn(Optional.empty());
        given(consentRepository.findActiveById(consent.getId())).willReturn(Optional.of(consent));

        assertThatThrownBy(() ->
                service.create(buildRequest(consent.getId(), "PULL", idempotencyKey)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_ACCESS_DENIED);
    }

    // ── listMine() tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자 전송 요청 목록 조회")
    void listMine_asUser_returnsBothSides() {
        setUserContext(user);
        TransferRequest transferRequest = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByUserId(user.getId())).willReturn(List.of(transferRequest));

        List<TransferResponse> result = service.listMine();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Agent 전송 요청 목록 조회")
    void listMine_asAgent_returnsAgentRequests() {
        Agent agent = AgentFixture.active();
        setAgentContext(agent);
        TransferRequest transferRequest = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByAgentId(agent.getId())).willReturn(List.of(transferRequest));

        List<TransferResponse> result = service.listMine();

        assertThat(result).hasSize(1);
    }

    // ── approve() tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("동의 소유자의 전송 요청 승인 성공")
    void approve_success_setsApprover() {
        setUserContext(user);
        TransferRequest transferRequest = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByIdForUpdate(transferRequest.getId()))
                .willReturn(Optional.of(transferRequest));
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(transferRepository.save(any(TransferRequest.class))).willReturn(transferRequest);

        TransferResponse response = service.approve(transferRequest.getId());

        assertThat(response).isNotNull();
        verify(stateMachine).approve(transferRequest);
    }

    @Test
    @DisplayName("비관련 사용자의 승인 시 TRANSFER_ACCESS_DENIED")
    void approve_unauthorizedUser_throws() {
        User otherUser = UserFixture.user();
        setUserContext(otherUser);
        TransferRequest transferRequest = TransferRequestFixture.requested(consent, user);

        given(transferRepository.findByIdForUpdate(transferRequest.getId()))
                .willReturn(Optional.of(transferRequest));

        assertThatThrownBy(() -> service.approve(transferRequest.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("이미 승인된 요청 재승인 시 TRANSFER_INVALID_TRANSITION")
    void approve_alreadyApproved_throwsStateMachine() {
        setUserContext(user);
        TransferRequest approvedRequest = TransferRequestFixture.approved(consent, user, user);

        given(transferRepository.findByIdForUpdate(approvedRequest.getId()))
                .willReturn(Optional.of(approvedRequest));
        org.mockito.BDDMockito.willThrow(new BusinessException(ErrorCode.TRANSFER_INVALID_TRANSITION))
                .given(stateMachine).approve(approvedRequest);

        assertThatThrownBy(() -> service.approve(approvedRequest.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("존재하지 않는 요청 승인 시 TRANSFER_NOT_FOUND")
    void approve_notFound_throws() {
        setUserContext(user);
        UUID unknownId = UUID.randomUUID();

        given(transferRepository.findByIdForUpdate(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(unknownId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_NOT_FOUND);
    }

    // ── execute() tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("동의 소유자가 전송 요청 실행 성공")
    void execute_byConsentOwner_success() {
        setUserContext(user);
        TransferRequest approvedRequest = TransferRequestFixture.approved(consent, user, user);

        given(transferRepository.findByIdForUpdate(approvedRequest.getId()))
                .willReturn(Optional.of(approvedRequest));
        given(transferRepository.save(any(TransferRequest.class))).willReturn(approvedRequest);

        TransferResponse response = service.execute(approvedRequest.getId());

        assertThat(response).isNotNull();
        verify(stateMachine).complete(approvedRequest);
    }

    @Test
    @DisplayName("비관련 사용자의 실행 시 TRANSFER_ACCESS_DENIED")
    void execute_byUnauthorized_throws() {
        User otherUser = UserFixture.user();
        setUserContext(otherUser);
        TransferRequest approvedRequest = TransferRequestFixture.approved(consent, user, user);

        given(transferRepository.findByIdForUpdate(approvedRequest.getId()))
                .willReturn(Optional.of(approvedRequest));

        assertThatThrownBy(() -> service.execute(approvedRequest.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("요청 Agent가 전송 요청 실행 성공")
    void execute_byRequesterAgent_success() {
        Agent agent = AgentFixture.active();
        setAgentContext(agent);

        TransferRequest approvedRequest = TransferRequest.builder()
                .id(UUID.randomUUID())
                .consent(consent)
                .method("PULL")
                .status(TransferStatus.APPROVED)
                .requesterAgent(agent)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        given(transferRepository.findByIdForUpdate(approvedRequest.getId()))
                .willReturn(Optional.of(approvedRequest));
        given(transferRepository.save(any(TransferRequest.class))).willReturn(approvedRequest);

        TransferResponse response = service.execute(approvedRequest.getId());

        assertThat(response).isNotNull();
        verify(stateMachine).complete(approvedRequest);
    }

    @Test
    @DisplayName("인증 없이 실행 시 ACCESS_DENIED")
    void execute_noAuth_throwsAccessDenied() {
        assertThatThrownBy(() -> service.execute(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
