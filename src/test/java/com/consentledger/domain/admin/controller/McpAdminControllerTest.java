package com.consentledger.domain.admin.controller;

import com.consentledger.domain.admin.dto.McpServerStatusResponse;
import com.consentledger.domain.admin.dto.McpToolInvokeResponse;
import com.consentledger.domain.admin.dto.McpToolSummary;
import com.consentledger.domain.admin.service.McpAdminService;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.global.config.SecurityConfig;
import com.consentledger.global.security.CustomUserDetailsService;
import com.consentledger.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpAdminController.class)
@Import(SecurityConfig.class)
class McpAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private McpAdminService mcpAdminService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private AgentRepository agentRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/mcp - 관리자는 서버 상태 조회 가능")
    void getStatus_asAdmin_returns200() throws Exception {
        McpToolSummary tool = McpToolSummary.builder()
                .name("listUsers")
                .description("List all users")
                .sourceClass("AdminMcpTools")
                .build();
        McpServerStatusResponse response = McpServerStatusResponse.builder()
                .serverName("consentledger-mcp")
                .serverVersion("1.0.0")
                .transport("SYNC")
                .sseEndpoint("/sse")
                .messageEndpointTemplate("/mcp/message?sessionId={sessionId}")
                .adminProtected(true)
                .registeredTools(1)
                .tools(List.of(tool))
                .build();
        given(mcpAdminService.getStatus()).willReturn(response);

        mockMvc.perform(get("/admin/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverName").value("consentledger-mcp"))
                .andExpect(jsonPath("$.data.registeredTools").value(1))
                .andExpect(jsonPath("$.data.tools[0].name").value("listUsers"));
    }

    @Test
    @DisplayName("GET /admin/mcp - 미인증 요청은 401")
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/mcp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin/mcp - USER 역할은 403")
    void getStatus_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/mcp"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp/invoke - 유효한 툴 실행 성공")
    void invokeTool_validTool_returns200() throws Exception {
        McpToolInvokeResponse invokeResponse = McpToolInvokeResponse.builder()
                .toolName("listUsers")
                .executedAt(Instant.now())
                .output("[{\"email\":\"admin@test.com\"}]")
                .build();
        given(mcpAdminService.invokeTool(eq("listUsers"), any())).willReturn(invokeResponse);

        Map<String, Object> body = Map.of("toolName", "listUsers");

        mockMvc.perform(post("/admin/mcp/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolName").value("listUsers"))
                .andExpect(jsonPath("$.data.output").value("[{\"email\":\"admin@test.com\"}]"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp/invoke - toolName 누락 시 400")
    void invokeTool_missingToolName_returns400() throws Exception {
        mockMvc.perform(post("/admin/mcp/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/mcp/invoke - 미인증 요청은 401")
    void invokeTool_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("toolName", "listUsers");

        mockMvc.perform(post("/admin/mcp/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
