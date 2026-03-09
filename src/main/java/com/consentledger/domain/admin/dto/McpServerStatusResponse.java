package com.consentledger.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class McpServerStatusResponse {

    private final String serverName;
    private final String serverVersion;
    private final String transport;
    private final String sseEndpoint;
    private final String messageEndpointTemplate;
    private final boolean adminProtected;
    private final int registeredTools;
    private final List<McpToolSummary> tools;
}
