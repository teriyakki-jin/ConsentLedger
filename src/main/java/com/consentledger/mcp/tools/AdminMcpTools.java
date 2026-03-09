package com.consentledger.mcp.tools;

import com.consentledger.domain.admin.dto.UserSummary;
import com.consentledger.domain.admin.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

// [CRITICAL-1] Defense-in-depth: enforce ADMIN role at method level
@Slf4j
@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMcpTools {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    @Tool(description = "List all users registered in the ConsentLedger system.")
    public String listUsers() {
        try {
            List<UserSummary> users = adminService.listUsers();
            return objectMapper.writeValueAsString(users);
        } catch (Exception e) {
            log.error("Failed to list users: {}", e.getMessage());
            return "{\"error\": \"Failed to retrieve users.\"}";
        }
    }
}
