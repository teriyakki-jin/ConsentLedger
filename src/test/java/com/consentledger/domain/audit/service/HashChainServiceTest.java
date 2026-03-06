package com.consentledger.domain.audit.service;

import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.entity.AuditChainAnchor;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditChainAnchorRepository;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.fixture.AuditLogFixture;
import com.consentledger.global.util.HashUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashChainServiceTest {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Mock
    private AuditChainAnchorRepository anchorRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private HashChainService hashChainService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hashChainService, "genesisHash", GENESIS_HASH);
        // @PersistenceContext fields are not injected by Mockito automatically
        ReflectionTestUtils.setField(hashChainService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("해시는 SHA-256 형식(64자 hex)이어야 한다")
    void hashShouldBe64CharHex() {
        String hash = HashUtils.sha256("test-input");
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("동일한 입력은 항상 동일한 해시를 생성한다")
    void sameInputProducesSameHash() {
        String hash1 = HashUtils.sha256("test-input");
        String hash2 = HashUtils.sha256("test-input");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 입력은 다른 해시를 생성한다")
    void differentInputProducesDifferentHash() {
        String hash1 = HashUtils.sha256("input1");
        String hash2 = HashUtils.sha256("input2");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("appendLog 호출 시 anchor가 잠기고 해시 체인이 연결된다")
    void appendLogBuildsChain() {
        AuditChainAnchor mockAnchor = mock(AuditChainAnchor.class);
        when(mockAnchor.getLastHash()).thenReturn(GENESIS_HASH);

        given(anchorRepository.findAnchorForUpdate()).willReturn(Optional.of(mockAnchor));
        given(auditLogRepository.save(any(AuditLog.class))).willAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            ReflectionTestUtils.setField(log, "id", 1L);
            return log;
        });

        AuditLog result = hashChainService.appendLog(
                "TEST_ACTION", "TEST_OBJECT", UUID.randomUUID(),
                UUID.randomUUID(), null, null, "SUCCESS",
                Map.of("key", "value"));

        assertThat(result.getPrevHash()).isEqualTo(GENESIS_HASH);
        assertThat(result.getHash()).hasSize(64);
        assertThat(result.getHash()).isNotEqualTo(GENESIS_HASH);
    }

    // ── verifyChain() tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("모든 체인이 유효한 경우 valid=true 반환")
    void verifyChain_allValid_returnsTrue() {
        AuditLog log1 = AuditLogFixture.firstLog();
        AuditLog log2 = AuditLogFixture.nextLog(log1, 2L);

        given(auditLogRepository.findBatchAfter(0L, 1000)).willReturn(List.of(log1, log2));
        given(auditLogRepository.count()).willReturn(2L);

        AuditVerifyResponse response = hashChainService.verifyChain();

        assertThat(response.isValid()).isTrue();
        assertThat(response.getVerifiedCount()).isEqualTo(2);
        assertThat(response.getTotalLogs()).isEqualTo(2);
    }

    @Test
    @DisplayName("payload_hash 불일치 시 valid=false, 첫 번째 손상 로그 ID 반환")
    void verifyChain_payloadHashMismatch_returnsFirstBroken() {
        AuditLog validLog = AuditLogFixture.firstLog();
        AuditLog brokenLog = AuditLogFixture.withBrokenPayloadHash(
                AuditLogFixture.nextLog(validLog, 2L));

        given(auditLogRepository.findBatchAfter(0L, 1000)).willReturn(List.of(validLog, brokenLog));
        given(auditLogRepository.count()).willReturn(2L);

        AuditVerifyResponse response = hashChainService.verifyChain();

        assertThat(response.isValid()).isFalse();
        assertThat(response.getFirstBrokenLogId()).isEqualTo(2L);
        assertThat(response.getReason()).contains("payload_hash");
    }

    @Test
    @DisplayName("prev_hash 불일치 시 valid=false, 첫 번째 손상 로그 ID 반환")
    void verifyChain_prevHashMismatch_returnsFirstBroken() {
        AuditLog brokenLog = AuditLogFixture.withBrokenPrevHash(AuditLogFixture.firstLog());

        given(auditLogRepository.findBatchAfter(0L, 1000)).willReturn(List.of(brokenLog));
        given(auditLogRepository.count()).willReturn(1L);

        AuditVerifyResponse response = hashChainService.verifyChain();

        assertThat(response.isValid()).isFalse();
        assertThat(response.getFirstBrokenLogId()).isEqualTo(1L);
        assertThat(response.getReason()).contains("prev_hash");
    }

    @Test
    @DisplayName("감사 로그가 없을 때 valid=true 반환")
    void verifyChain_emptyLog_returnsValid() {
        given(auditLogRepository.findBatchAfter(0L, 1000)).willReturn(List.of());
        given(auditLogRepository.count()).willReturn(0L);

        AuditVerifyResponse response = hashChainService.verifyChain();

        assertThat(response.isValid()).isTrue();
        assertThat(response.getVerifiedCount()).isEqualTo(0);
    }
}
