package com.consentledger.mcp.tools;

import com.consentledger.domain.audit.anomaly.AnomalyDetectionService;
import com.consentledger.domain.audit.anomaly.dto.AnomalyFinding;
import com.consentledger.domain.audit.anomaly.dto.AnomalyReport;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnomalyMcpToolsTest {

    @Mock private AnomalyDetectionService anomalyDetectionService;

    @InjectMocks private AnomalyMcpTools anomalyMcpTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(anomalyMcpTools, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("analyze_anomalies: 이상 탐지 리포트 JSON 반환")
    void analyzeAnomalies_returnsSerializedReport() {
        AnomalyFinding finding = AnomalyFinding.builder()
                .patternType("ACCOUNT_TAKEOVER")
                .severity("HIGH")
                .description("Suspicious login pattern")
                .affectedActor("user-1")
                .evidenceLogIds(List.of(1L, 2L))
                .recommendation("Force password reset")
                .build();
        AnomalyReport report = AnomalyReport.builder()
                .analyzedAt(Instant.parse("2026-03-09T01:00:00Z"))
                .periodDays(7)
                .totalLogsAnalyzed(25)
                .findings(List.of(finding))
                .riskSummary("Detected 1 finding.")
                .hasCriticalFindings(false)
                .analysisStatus("SUCCESS")
                .build();
        given(anomalyDetectionService.analyze(7)).willReturn(report);

        String result = anomalyMcpTools.analyzeAnomalies(7);

        verify(anomalyDetectionService).analyze(7);
        assertThat(result).contains("\"analysisStatus\":\"SUCCESS\"");
        assertThat(result).contains("ACCOUNT_TAKEOVER");
        assertThat(result).contains("Detected 1 finding.");
    }

    @Test
    @DisplayName("analyze_anomalies: 서비스 예외 시 에러 JSON 반환")
    void analyzeAnomalies_whenServiceFails_returnsErrorJson() {
        given(anomalyDetectionService.analyze(7)).willThrow(new RuntimeException("LLM unavailable"));

        String result = anomalyMcpTools.analyzeAnomalies(7);

        assertThat(result).isEqualTo("{\"error\": \"Anomaly analysis could not be completed. Check server logs.\"}");
    }
}
