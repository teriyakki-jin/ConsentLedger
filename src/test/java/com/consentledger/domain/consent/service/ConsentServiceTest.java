package com.consentledger.domain.consent.service;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.domain.agent.service.AgentPolicyValidator;
import com.consentledger.domain.audit.service.AuditLogService;
import com.consentledger.domain.consent.dto.ConsentCreateRequest;
import com.consentledger.domain.consent.dto.ConsentResponse;
import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.repository.ConsentRepository;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.dataholder.repository.DataHolderRepository;
import com.consentledger.domain.user.entity.User;
import com.consentledger.domain.user.repository.UserRepository;
import com.consentledger.fixture.AgentFixture;
import com.consentledger.fixture.ConsentFixture;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentRepository agentRepository;
    @Mock private DataHolderRepository dataHolderRepository;
    @Mock private AgentPolicyValidator agentPolicyValidator;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ConsentService consentService;

    private User user;
    private DataHolder dataHolder;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        dataHolder = ConsentFixture.dataHolder();
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

    private ConsentCreateRequest buildRequest(UUID dataHolderId, UUID agentId, List<String> scopes) {
        ConsentCreateRequest req = new ConsentCreateRequest();
        ReflectionTestUtils.setField(req, "dataHolderId", dataHolderId);
        ReflectionTestUtils.setField(req, "agentId", agentId);
        ReflectionTestUtils.setField(req, "scopes", scopes);
        return req;
    }

    // ── create() tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자로 동의 생성 성공 - 감사 로그 기록 검증")
    void create_byUser_success() {
        setUserContext(user);
        Consent consent = ConsentFixture.active(user, dataHolder);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(dataHolderRepository.findById(dataHolder.getId())).willReturn(Optional.of(dataHolder));
        given(consentRepository.save(any(Consent.class))).willReturn(consent);

        ConsentResponse response = consentService.create(
                buildRequest(dataHolder.getId(), null, List.of("READ")));

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(user.getId());
        verify(auditLogService).log(
                eq("CONSENT_CREATED"), eq("CONSENT"), any(),
                eq(user.getId()), isNull(), any());
    }

    @Test
    @DisplayName("Agent는 직접 동의 생성 불가 - ACCESS_DENIED")
    void create_byAgent_throwsAccessDenied() {
        Agent agent = AgentFixture.active();
        setAgentContext(agent);

        assertThatThrownBy(() ->
                consentService.create(buildRequest(dataHolder.getId(), null, List.of("READ"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("없는 데이터홀더로 동의 생성 시 DATA_HOLDER_NOT_FOUND")
    void create_dataHolderNotFound_throws() {
        setUserContext(user);
        UUID unknownId = UUID.randomUUID();

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(dataHolderRepository.findById(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                consentService.create(buildRequest(unknownId, null, List.of("READ"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_HOLDER_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 agentId 지정 시 AGENT_NOT_FOUND")
    void create_agentNotFound_throws() {
        setUserContext(user);
        UUID unknownAgentId = UUID.randomUUID();

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(dataHolderRepository.findById(dataHolder.getId())).willReturn(Optional.of(dataHolder));
        given(agentRepository.findById(unknownAgentId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                consentService.create(buildRequest(dataHolder.getId(), unknownAgentId, List.of("READ"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AGENT_NOT_FOUND);
    }

    // ── listMyConsents() tests ───────────────────────────────────────────────

    @Test
    @DisplayName("사용자 동의 목록 조회 - 소유한 동의만 반환")
    void listMyConsents_asUser_returnsOwned() {
        setUserContext(user);
        Consent consent = ConsentFixture.active(user, dataHolder);

        given(consentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .willReturn(List.of(consent));

        List<ConsentResponse> result = consentService.listMyConsents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("인증 없이 동의 목록 조회 시 INVALID_TOKEN")
    void listMyConsents_noAuth_throwsInvalidToken() {
        assertThatThrownBy(() -> consentService.listMyConsents())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    // ── getById() tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("본인 동의 조회 성공")
    void getById_ownConsent_success() {
        setUserContext(user);
        Consent consent = ConsentFixture.active(user, dataHolder);

        given(consentRepository.findByIdAndUserId(consent.getId(), user.getId()))
                .willReturn(Optional.of(consent));

        ConsentResponse response = consentService.getById(consent.getId());

        assertThat(response.getId()).isEqualTo(consent.getId());
    }

    @Test
    @DisplayName("타인 동의 조회 시 CONSENT_NOT_FOUND")
    void getById_otherUserConsent_throws() {
        setUserContext(user);

        given(consentRepository.findByIdAndUserId(any(UUID.class), eq(user.getId())))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> consentService.getById(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONSENT_NOT_FOUND);
    }

    // ── revoke() tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("활성 동의 철회 성공")
    void revoke_activeConsent_success() {
        setUserContext(user);
        Consent consent = ConsentFixture.active(user, dataHolder);

        given(consentRepository.findByIdAndUserId(consent.getId(), user.getId()))
                .willReturn(Optional.of(consent));
        given(consentRepository.save(any(Consent.class))).willReturn(consent);

        ConsentResponse response = consentService.revoke(consent.getId());

        assertThat(response).isNotNull();
        verify(auditLogService).log(
                eq("CONSENT_REVOKED"), eq("CONSENT"), any(),
                eq(user.getId()), isNull(), any());
    }

    @Test
    @DisplayName("이미 철회된 동의 재철회 시 CONSENT_ALREADY_REVOKED")
    void revoke_alreadyRevoked_throws() {
        setUserContext(user);
        Consent revokedConsent = ConsentFixture.revoked(user, dataHolder);

        given(consentRepository.findByIdAndUserId(revokedConsent.getId(), user.getId()))
                .willReturn(Optional.of(revokedConsent));

        assertThatThrownBy(() -> consentService.revoke(revokedConsent.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONSENT_ALREADY_REVOKED);
    }

    @Test
    @DisplayName("없는 동의 철회 시 CONSENT_NOT_FOUND")
    void revoke_consentNotFound_throws() {
        setUserContext(user);
        UUID unknownId = UUID.randomUUID();

        given(consentRepository.findByIdAndUserId(unknownId, user.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> consentService.revoke(unknownId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONSENT_NOT_FOUND);
    }
}
