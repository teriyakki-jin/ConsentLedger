package com.consentledger.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class McpToolInvokeRequest {

    @NotBlank
    private String toolName;

    private Map<String, Object> params;
}
