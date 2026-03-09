package com.consentledger.mcp.tools;

import com.consentledger.domain.audit.anomaly.AnomalyDetectionService;
import com.consentledger.domain.audit.anomaly.dto.AnomalyReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

// [CRITICAL-1] Defense-in-depth: enforce ADMIN role at method level
@Slf4j
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AnomalyMcpTools {

    // [Circular dependency fix] @Lazy breaks the cycle:
    // AnomalyMcpTools → AnomalyDetectionService → ChatModel → ToolCallbackProvider → AnomalyMcpTools
    private final AnomalyDetectionService anomalyDetectionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnomalyMcpTools(@Lazy AnomalyDetectionService anomalyDetectionService,
                           ObjectMapper objectMapper) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Analyze audit logs using AI to detect security anomalies. " +
            "Detects: abnormal hours, privilege abuse, data exfiltration, account takeover. " +
            "Returns a report with findings, risk summary, and analysisStatus.")
    public String analyzeAnomalies(
            @ToolParam(description = "Number of days to analyze (1-30). Default is 7.") int days) {
        try {
            AnomalyReport report = anomalyDetectionService.analyze(days);
            return objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            // [CRITICAL-3] Do not leak exception details; log server-side only
            log.error("Anomaly analysis failed: {}", e.getMessage());
            return "{\"error\": \"Anomaly analysis could not be completed. Check server logs.\"}";
        }
    }
}
