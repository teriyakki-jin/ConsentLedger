package com.consentledger.domain.admin.service;

import com.consentledger.mcp.tools.AdminMcpTools;
import com.consentledger.mcp.tools.AnomalyMcpTools;
import com.consentledger.mcp.tools.AuditMcpTools;
import com.consentledger.mcp.tools.ConsentMcpTools;
import com.consentledger.mcp.tools.TransferMcpTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class McpAdminServiceTest {

    @Mock private AuditMcpTools auditMcpTools;
    @Mock private ConsentMcpTools consentMcpTools;
    @Mock private TransferMcpTools transferMcpTools;
    @Mock private AdminMcpTools adminMcpTools;
    @Mock private AnomalyMcpTools anomalyMcpTools;

    @InjectMocks private McpAdminService mcpAdminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mcpAdminService, "serverName", "consentledger-mcp");
        ReflectionTestUtils.setField(mcpAdminService, "serverVersion", "1.0.0");
        ReflectionTestUtils.setField(mcpAdminService, "transport", "SYNC");
        // @PostConstruct is not called by Mockito; invoke manually to build registries
        mcpAdminService.init();
    }

    @Test
    @DisplayName("get_status: 등록된 MCP 툴과 엔드포인트 정보를 반환")
    void getStatus_returnsToolCatalog() {
        var result = mcpAdminService.getStatus();

        assertThat(result.getServerName()).isEqualTo("consentledger-mcp");
        assertThat(result.getSseEndpoint()).isEqualTo("/sse");
        assertThat(result.getMessageEndpointTemplate()).isEqualTo("/mcp/message?sessionId={sessionId}");
        assertThat(result.isAdminProtected()).isTrue();
        assertThat(result.getRegisteredTools()).isEqualTo(6);
        assertThat(result.getTools()).extracting("name")
                .contains(
                        "analyzeAnomalies",
                        "getAuditLogs",
                        "getConsentsByUser",
                        "getTransferRequests",
                        "listUsers",
                        "verifyAuditChain"
                );
    }

    @Test
    @DisplayName("invoke_tool: 파라미터 없는 MCP 툴 실행 결과 반환")
    void invokeTool_noParamTool_returnsOutput() {
        given(adminMcpTools.listUsers()).willReturn("[{\"email\":\"admin@test.com\"}]");

        var result = mcpAdminService.invokeTool("listUsers", Map.of());

        assertThat(result.getToolName()).isEqualTo("listUsers");
        assertThat(result.getOutput()).contains("admin@test.com");
    }

    @Test
    @DisplayName("invoke_tool: 파라미터 있는 MCP 툴 실행 결과 반환")
    void invokeTool_paramTool_returnsOutput() {
        given(consentMcpTools.getConsentsByUser("user-1")).willReturn("[]");

        var result = mcpAdminService.invokeTool("getConsentsByUser", Map.of("userId", "user-1"));

        assertThat(result.getToolName()).isEqualTo("getConsentsByUser");
        assertThat(result.getOutput()).isEqualTo("[]");
    }

    @Test
    @DisplayName("invoke_tool: 지원하지 않는 툴명은 예외")
    void invokeTool_unknownTool_throwsException() {
        assertThatThrownBy(() -> mcpAdminService.invokeTool("unknownTool", Map.of()))
                .hasMessageContaining("Unsupported MCP tool");
    }
}
