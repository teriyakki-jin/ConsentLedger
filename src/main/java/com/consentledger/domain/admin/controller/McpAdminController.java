package com.consentledger.domain.admin.controller;

import com.consentledger.domain.admin.dto.McpServerStatusResponse;
import com.consentledger.domain.admin.dto.McpToolInvokeRequest;
import com.consentledger.domain.admin.dto.McpToolInvokeResponse;
import com.consentledger.domain.admin.service.McpAdminService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - MCP", description = "관리자 MCP 상태 API")
@RestController
@RequestMapping("/admin/mcp")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class McpAdminController {

    private final McpAdminService mcpAdminService;

    @Operation(summary = "MCP 서버 상태 및 등록 툴 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<McpServerStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(mcpAdminService.getStatus()));
    }

    @Operation(summary = "MCP 툴 수동 실행")
    @PostMapping("/invoke")
    public ResponseEntity<ApiResponse<McpToolInvokeResponse>> invokeTool(
            @Valid @RequestBody McpToolInvokeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                mcpAdminService.invokeTool(request.getToolName(), request.getParams())
        ));
    }
}
