package com.consentledger.domain.audit.anomaly;

import com.consentledger.domain.audit.anomaly.dto.AnomalyReport;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Anomaly", description = "AI 이상 감지 API")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class AnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;

    @Operation(
            summary = "AI 이상 감지 분석",
            description = "AI 모델로 최근 N일 감사 로그를 분석하여 이상 패턴을 탐지합니다. (ADMIN 전용)"
    )
    @GetMapping("/audit-logs/analyze")
    public ResponseEntity<ApiResponse<AnomalyReport>> analyzeAnomalies(
            // [HIGH-2] Validate days range at controller boundary
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        AnomalyReport report = anomalyDetectionService.analyze(days);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
