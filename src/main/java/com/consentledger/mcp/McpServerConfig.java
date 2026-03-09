package com.consentledger.mcp;

import com.consentledger.mcp.tools.AdminMcpTools;
import com.consentledger.mcp.tools.AnomalyMcpTools;
import com.consentledger.mcp.tools.AuditMcpTools;
import com.consentledger.mcp.tools.ConsentMcpTools;
import com.consentledger.mcp.tools.TransferMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class McpServerConfig {

    private final AuditMcpTools auditMcpTools;
    private final ConsentMcpTools consentMcpTools;
    private final TransferMcpTools transferMcpTools;
    private final AdminMcpTools adminMcpTools;
    private final AnomalyMcpTools anomalyMcpTools;

    /**
     * Registers all MCP tool classes with the Spring AI MCP server.
     * The auto-configuration picks up ToolCallbackProvider beans and
     * registers their tool callbacks with the MCP server.
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(auditMcpTools, consentMcpTools, transferMcpTools, adminMcpTools, anomalyMcpTools)
                .build();
    }
}
