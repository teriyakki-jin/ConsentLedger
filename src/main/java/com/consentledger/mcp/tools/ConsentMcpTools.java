package com.consentledger.mcp.tools;

import com.consentledger.domain.consent.dto.ConsentResponse;
import com.consentledger.domain.consent.repository.ConsentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// [CRITICAL-1] Defense-in-depth: enforce ADMIN role at method level
@Slf4j
@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConsentMcpTools {

    private final ConsentRepository consentRepository;
    private final ObjectMapper objectMapper;

    @Tool(description = "Get all consents for a specific user by their UUID.")
    public String getConsentsByUser(
            @ToolParam(description = "The UUID of the user to query consents for.") String userId) {
        try {
            UUID uid = UUID.fromString(userId);
            List<ConsentResponse> consents = consentRepository.findByUserIdOrderByCreatedAtDesc(uid)
                    .stream()
                    .map(ConsentResponse::from)
                    .toList();
            return objectMapper.writeValueAsString(consents);
        } catch (IllegalArgumentException e) {
            return "{\"error\": \"Invalid userId format. Must be a valid UUID.\"}";
        } catch (Exception e) {
            log.error("Failed to get consents for user: {}", e.getMessage());
            return "{\"error\": \"Failed to retrieve consents.\"}";
        }
    }
}
