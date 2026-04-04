package com.consentledger.domain.audit.demo;

import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin - Demo", description = "데모 시나리오 생성 API (ADMIN 전용)")
@RestController
@RequestMapping("/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    @Operation(
            summary = "AI 이상 탐지 데모 시나리오 생성",
            description = "ACCOUNT_TAKEOVER, DATA_EXFILTRATION, PRIVILEGE_ABUSE, ABNORMAL_HOURS 4가지 패턴의 의심 로그를 생성합니다."
    )
    @PostMapping("/anomaly-scenario")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAnomalyScenario() {
        int count = demoScenarioService.generateAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "generatedLogs", count,
                "message", "데모 시나리오 로그가 생성되었습니다. AI 이상 탐지를 실행해보세요."
        )));
    }
}
