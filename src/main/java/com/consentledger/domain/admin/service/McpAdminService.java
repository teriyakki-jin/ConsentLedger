package com.consentledger.domain.admin.service;

import com.consentledger.domain.admin.dto.McpServerStatusResponse;
import com.consentledger.domain.admin.dto.McpToolInvokeResponse;
import com.consentledger.domain.admin.dto.McpToolSummary;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.mcp.tools.AdminMcpTools;
import com.consentledger.mcp.tools.AnomalyMcpTools;
import com.consentledger.mcp.tools.AuditMcpTools;
import com.consentledger.mcp.tools.ConsentMcpTools;
import com.consentledger.mcp.tools.TransferMcpTools;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class McpAdminService {

    private final AuditMcpTools auditMcpTools;
    private final ConsentMcpTools consentMcpTools;
    private final TransferMcpTools transferMcpTools;
    private final AdminMcpTools adminMcpTools;
    private final AnomalyMcpTools anomalyMcpTools;

    @Value("${spring.ai.mcp.server.name:consentledger-mcp}")
    private String serverName;

    @Value("${spring.ai.mcp.server.version:1.0.0}")
    private String serverVersion;

    @Value("${spring.ai.mcp.server.type:SYNC}")
    private String transport;

    @FunctionalInterface
    interface ToolInvoker {
        String invoke(Map<String, Object> params);
    }

    private List<Object> toolBeans;
    private Map<String, ToolInvoker> invokerRegistry;

    /**
     * Builds tool bean list (for status reflection) and invoker registry (for invocation).
     * To add a new MCP tool:
     *   1. Inject the new tool bean above
     *   2. Add it to toolBeans list
     *   3. Register its methods in invokerRegistry below
     */
    @PostConstruct
    void init() {
        toolBeans = List.of(auditMcpTools, consentMcpTools, transferMcpTools, adminMcpTools, anomalyMcpTools);

        Map<String, ToolInvoker> registry = new HashMap<>();
        registry.put("listUsers",           p -> adminMcpTools.listUsers());
        registry.put("getTransferRequests", p -> transferMcpTools.getTransferRequests());
        registry.put("verifyAuditChain",    p -> auditMcpTools.verifyAuditChain());
        registry.put("analyzeAnomalies",    p -> anomalyMcpTools.analyzeAnomalies(getIntParam(p, "days", 7)));
        registry.put("getConsentsByUser",   p -> consentMcpTools.getConsentsByUser(getRequiredStringParam(p, "userId")));
        registry.put("getAuditLogs",        p -> auditMcpTools.getAuditLogs(
                getOptionalStringParam(p, "from"),
                getOptionalStringParam(p, "to"),
                getOptionalStringParam(p, "action"),
                getOptionalStringParam(p, "outcome"),
                getIntParam(p, "page", 0)));
        invokerRegistry = Map.copyOf(registry);
    }

    @Transactional(readOnly = true)
    public McpServerStatusResponse getStatus() {
        List<McpToolSummary> tools = toolBeans.stream()
                .flatMap(this::extractToolSummaries)
                .sorted(Comparator.comparing(McpToolSummary::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return McpServerStatusResponse.builder()
                .serverName(serverName)
                .serverVersion(serverVersion)
                .transport(transport)
                .sseEndpoint("/sse")
                .messageEndpointTemplate("/mcp/message?sessionId={sessionId}")
                .adminProtected(true)
                .registeredTools(tools.size())
                .tools(tools)
                .build();
    }

    public McpToolInvokeResponse invokeTool(String toolName, Map<String, Object> params) {
        ToolInvoker invoker = invokerRegistry.get(toolName);
        if (invoker == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported MCP tool: " + toolName);
        }

        Map<String, Object> safeParams = params != null ? params : Map.of();
        String output = invoker.invoke(safeParams);

        return McpToolInvokeResponse.builder()
                .toolName(toolName)
                .executedAt(Instant.now())
                .output(output)
                .build();
    }

    private Stream<McpToolSummary> extractToolSummaries(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return Stream.of(targetClass.getMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(method -> buildToolSummary(targetClass, method));
    }

    private McpToolSummary buildToolSummary(Class<?> targetClass, Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        return McpToolSummary.builder()
                .name(method.getName())
                .description(tool.description())
                .sourceClass(targetClass.getSimpleName())
                .build();
    }

    private String getRequiredStringParam(Map<String, Object> params, String key) {
        String value = getOptionalStringParam(params, key);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Missing required param: " + key);
        }
        return value;
    }

    private String getOptionalStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid integer param: " + key);
        }
    }
}
