package com.consentledger.mcp.tools;

import com.consentledger.domain.transfer.dto.TransferResponse;
import com.consentledger.domain.transfer.repository.TransferRequestRepository;
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
public class TransferMcpTools {

    private final TransferRequestRepository transferRequestRepository;
    private final ObjectMapper objectMapper;

    @Tool(description = "Get all transfer requests in the system. Returns a list of transfer request summaries.")
    public String getTransferRequests() {
        try {
            List<TransferResponse> transfers = transferRequestRepository.findAll()
                    .stream()
                    .map(TransferResponse::from)
                    .toList();
            return objectMapper.writeValueAsString(transfers);
        } catch (Exception e) {
            log.error("Failed to get transfer requests: {}", e.getMessage());
            return "{\"error\": \"Failed to retrieve transfer requests.\"}";
        }
    }
}
