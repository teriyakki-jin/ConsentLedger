package com.consentledger.domain.consent.controller;

import com.consentledger.domain.consent.dto.ConsentCreateRequest;
import com.consentledger.domain.consent.dto.ConsentResponse;
import com.consentledger.domain.consent.service.ConsentService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Consents", description = "동의 관리 API")
@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "동의 생성", description = "사용자가 데이터 보유자에 대한 동의를 생성합니다.")
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConsentResponse>> create(@Valid @RequestBody ConsentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(consentService.create(request)));
    }

    @Operation(summary = "동의 목록 조회", description = "현재 로그인한 사용자의 동의 목록을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(consentService.listMyConsents()));
    }

    @Operation(summary = "동의 단건 조회", description = "특정 동의를 조회합니다.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConsentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.getById(id)));
    }

    @Operation(summary = "동의 철회", description = "사용자가 동의를 철회합니다.")
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConsentResponse>> revoke(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.revoke(id)));
    }
}
