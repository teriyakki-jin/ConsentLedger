package com.consentledger.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class McpToolSummary {

    private final String name;
    private final String description;
    private final String sourceClass;
}
