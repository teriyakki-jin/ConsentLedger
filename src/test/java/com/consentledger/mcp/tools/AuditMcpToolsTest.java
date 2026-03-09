package com.consentledger.mcp.tools;

import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.domain.audit.service.HashChainService;
import com.consentledger.fixture.AuditLogFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditMcpToolsTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HashChainService hashChainService;

    @InjectMocks private AuditMcpTools auditMcpTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(auditMcpTools, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("get_audit_logs: 필터 적용 후 로그 목록 JSON 반환")
    void getAuditLogs_withFilters_returnsSerializedLogs() {
        AuditLog log = AuditLogFixture.firstLog();
        given(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(log)));

        String result = auditMcpTools.getAuditLogs(null, null, "TEST_ACTION", "SUCCESS", 0);

        assertThat(result).isNotBlank();
        assertThat(result).contains("TEST_ACTION");
        assertThat(result).contains("SUCCESS");
    }

    @Test
    @DisplayName("get_audit_logs: 로그 없을 때 빈 배열 반환")
    void getAuditLogs_noResults_returnsEmptyArray() {
        given(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        String result = auditMcpTools.getAuditLogs(null, null, null, null, 0);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    @DisplayName("get_audit_logs: 다수 로그 포함 시 모두 직렬화")
    void getAuditLogs_multipleLogs_returnsAllSerialized() {
        AuditLog first = AuditLogFixture.firstLog();
        AuditLog second = AuditLogFixture.nextLog(first, 2L);
        given(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(first, second)));

        String result = auditMcpTools.getAuditLogs(null, null, null, null, 0);

        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
    }

    @Test
    @DisplayName("verify_audit_chain: HashChainService에 위임하고 결과 반환")
    void verifyAuditChain_delegatesToHashChainService() {
        AuditVerifyResponse verifyResponse = AuditVerifyResponse.valid(10L, 10L);
        given(hashChainService.verifyChain()).willReturn(verifyResponse);

        String result = auditMcpTools.verifyAuditChain();

        verify(hashChainService).verifyChain();
        assertThat(result).contains("\"valid\":true");
        assertThat(result).contains("\"totalLogs\":10");
    }

    @Test
    @DisplayName("verify_audit_chain: 체인 손상 시 broken 결과 반환")
    void verifyAuditChain_brokenChain_returnsBrokenResult() {
        AuditVerifyResponse brokenResponse = AuditVerifyResponse.broken(10L, 5L, 6L, "prev_hash mismatch");
        given(hashChainService.verifyChain()).willReturn(brokenResponse);

        String result = auditMcpTools.verifyAuditChain();

        assertThat(result).contains("\"valid\":false");
        assertThat(result).contains("prev_hash mismatch");
    }
}
