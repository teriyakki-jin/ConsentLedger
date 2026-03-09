package com.consentledger.domain.audit.anomaly;

import com.consentledger.domain.audit.anomaly.dto.AnomalyFinding;
import com.consentledger.domain.audit.anomaly.dto.AnomalyReport;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.fixture.AuditLogFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ChatModel chatModel;

    @InjectMocks private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(anomalyDetectionService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(anomalyDetectionService, "maxLogsPerAnalysis", 500);
        ReflectionTestUtils.setField(anomalyDetectionService, "maxDays", 30);
    }

    @Test
    @DisplayName("정상 로그 분석 - AI 모델 응답으로 findings 반환, analysisStatus=SUCCESS")
    void analyze_withLogs_returnsFindingsFromModel() {
        AuditLog log = AuditLogFixture.firstLog();
        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(Instant.class), any(Instant.class)))
                .willReturn(List.of(log));

        String modelJson = """
                [{"patternType":"ABNORMAL_HOURS","severity":"HIGH","description":"Action at midnight",
                  "affectedActor":"user-1","evidenceLogIds":[1],"recommendation":"Review access"}]
                """;
        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        given(chatResponse.getResult().getOutput().getText()).willReturn(modelJson);
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);

        AnomalyReport report = anomalyDetectionService.analyze(7);

        assertThat(report.getTotalLogsAnalyzed()).isEqualTo(1);
        assertThat(report.getFindings()).hasSize(1);
        assertThat(report.getFindings().get(0).getPatternType()).isEqualTo("ABNORMAL_HOURS");
        assertThat(report.getFindings().get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(report.isHasCriticalFindings()).isFalse();
        assertThat(report.getAnalysisStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("빈 로그 - AI 모델 호출 없이 analysisStatus=NO_LOGS 반환")
    void analyze_emptyLogs_returnsNoLogsStatus() {
        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(Instant.class), any(Instant.class)))
                .willReturn(List.of());

        AnomalyReport report = anomalyDetectionService.analyze(7);

        assertThat(report.getTotalLogsAnalyzed()).isEqualTo(0);
        assertThat(report.getFindings()).isEmpty();
        assertThat(report.isHasCriticalFindings()).isFalse();
        assertThat(report.getAnalysisStatus()).isEqualTo("NO_LOGS");
        assertThat(report.getRiskSummary()).contains("No logs found");
    }

    @Test
    @DisplayName("AI 모델 API 실패 시 analysisStatus=ANALYSIS_FAILED (빈 findings와 구분)")
    void analyze_modelApiFails_returnsAnalysisFailedStatus() {
        AuditLog log = AuditLogFixture.firstLog();
        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(Instant.class), any(Instant.class)))
                .willReturn(List.of(log));
        given(chatModel.call(any(Prompt.class))).willThrow(new RuntimeException("API rate limit exceeded"));

        AnomalyReport report = anomalyDetectionService.analyze(7);

        assertThat(report.getAnalysisStatus()).isEqualTo("ANALYSIS_FAILED");
        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getRiskSummary()).contains("could not be completed");
    }

    @Test
    @DisplayName("로그 상한 초과 시 maxLogsPerAnalysis 적용, totalLogsAnalyzed=3")
    void analyze_logsExceedMax_capsAtMaxLogsPerAnalysis() {
        ReflectionTestUtils.setField(anomalyDetectionService, "maxLogsPerAnalysis", 3);

        AuditLog first = AuditLogFixture.firstLog();
        AuditLog second = AuditLogFixture.nextLog(first, 2L);
        AuditLog third = AuditLogFixture.nextLog(second, 3L);
        AuditLog fourth = AuditLogFixture.nextLog(third, 4L);
        AuditLog fifth = AuditLogFixture.nextLog(fourth, 5L);

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(Instant.class), any(Instant.class)))
                .willReturn(List.of(first, second, third, fourth, fifth));

        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        given(chatResponse.getResult().getOutput().getText()).willReturn("[]");
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);

        AnomalyReport report = anomalyDetectionService.analyze(7);

        assertThat(report.getTotalLogsAnalyzed()).isEqualTo(3);
        assertThat(report.getAnalysisStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("CRITICAL severity 포함 시 hasCriticalFindings=true, analysisStatus=SUCCESS")
    void analyze_criticalFinding_setsCriticalFlagAndSuccessStatus() {
        AuditLog log = AuditLogFixture.firstLog();
        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(Instant.class), any(Instant.class)))
                .willReturn(List.of(log));

        String modelJson = """
                [{"patternType":"ACCOUNT_TAKEOVER","severity":"CRITICAL","description":"Suspicious login",
                  "affectedActor":"user-99","evidenceLogIds":[1],"recommendation":"Block immediately"}]
                """;
        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        given(chatResponse.getResult().getOutput().getText()).willReturn(modelJson);
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);

        AnomalyReport report = anomalyDetectionService.analyze(7);

        assertThat(report.isHasCriticalFindings()).isTrue();
        assertThat(report.getAnalysisStatus()).isEqualTo("SUCCESS");
        assertThat(report.getFindings()).extracting(AnomalyFinding::getSeverity)
                .containsExactly("CRITICAL");
    }
}
