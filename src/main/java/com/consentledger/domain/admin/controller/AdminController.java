package com.consentledger.domain.admin.controller;

import com.consentledger.domain.admin.dto.AgentStatusUpdateRequest;
import com.consentledger.domain.admin.dto.AgentSummary;
import com.consentledger.domain.admin.dto.UserSummary;
import com.consentledger.domain.admin.service.AdminService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "사용자 목록 조회")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummary>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers()));
    }

    @Operation(summary = "에이전트 목록 조회")
    @GetMapping("/agents")
    public ResponseEntity<ApiResponse<List<AgentSummary>>> listAgents() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAgents()));
    }

    @Operation(summary = "에이전트 상태 변경")
    @PatchMapping("/agents/{id}/status")
    public ResponseEntity<ApiResponse<AgentSummary>> updateAgentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AgentStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateAgentStatus(id, request)));
    }
}
