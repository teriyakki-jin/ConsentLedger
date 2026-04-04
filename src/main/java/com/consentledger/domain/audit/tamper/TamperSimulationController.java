package com.consentledger.domain.audit.tamper;

import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin - Demo", description = "데모 시나리오 생성 API (ADMIN 전용)")
@RestController
@RequestMapping("/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TamperSimulationController {

    private final TamperSimulationService tamperSimulationService;

    @Operation(
            summary = "해시 체인 변조 시뮬레이션",
            description = "지정한 감사 로그의 payload를 변조하고 체인 검증을 실행합니다. 데모 전용."
    )
    @PostMapping("/tamper/{logId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> tamper(@PathVariable long logId) {
        TamperSimulationService.TamperResult result = tamperSimulationService.tamperAndVerify(logId);

        Map<String, Object> data = Map.of(
                "tamperedLogId", result.tamperedLogId(),
                "chainValid", result.verifyResult().isValid(),
                "totalChecked", result.verifyResult().getVerifiedCount(),
                "firstBrokenId", result.verifyResult().getFirstBrokenLogId() != null
                        ? result.verifyResult().getFirstBrokenLogId() : -1,
                "message", result.verifyResult().isValid()
                        ? "체인이 유효합니다 (예상치 못한 결과)"
                        : "해시 체인이 깨졌습니다! ID " + result.verifyResult().getFirstBrokenLogId() + "에서 변조 감지"
        );

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @Operation(summary = "변조 상태 조회", description = "현재 변조된 로그 수를 조회합니다.")
    @GetMapping("/tamper/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.ok(tamperSimulationService.getStatus()));
    }
}
