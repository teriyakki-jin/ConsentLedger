package com.consentledger.domain.audit.anomaly.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AnomalyReport {

    private final Instant analyzedAt;
    private final int periodDays;
    private final int totalLogsAnalyzed;
    private final List<AnomalyFinding> findings;
    private final String riskSummary;
    private final boolean hasCriticalFindings;
    // [HIGH-4] Distinguish AI call failure from "no anomalies found"
    // Values: "SUCCESS", "ANALYSIS_FAILED", "NO_LOGS"
    private final String analysisStatus;
}
