package com.consentledger.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class McpToolInvokeResponse {

    private final String toolName;
    private final Instant executedAt;
    private final String output;
}
