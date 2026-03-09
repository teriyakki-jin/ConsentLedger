package com.consentledger.mcp.tools;

import com.consentledger.domain.audit.dto.AuditLogResponse;
import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.domain.audit.service.HashChainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// [CRITICAL-1] Defense-in-depth: enforce ADMIN role at method level
@Slf4j
@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditMcpTools {

    private final AuditLogRepository auditLogRepository;
    private final HashChainService hashChainService;
    private final ObjectMapper objectMapper;

    @Tool(description = "Get audit logs with optional filters. Returns paginated audit log entries.")
    public String getAuditLogs(
            @ToolParam(description = "Start time in ISO-8601 format (e.g. 2025-01-01T00:00:00Z). Optional.") String from,
            @ToolParam(description = "End time in ISO-8601 format (e.g. 2025-01-31T23:59:59Z). Optional.") String to,
            @ToolParam(description = "Action filter (e.g. CONSENT_CREATED, TRANSFER_APPROVED). Optional.") String action,
            @ToolParam(description = "Outcome filter (e.g. SUCCESS, FAILURE). Optional.") String outcome,
            @ToolParam(description = "Page number (0-based, default 0).") int page) {

        // [HIGH-3] Validate and sanitize inputs
        int safePage = Math.max(0, page);
        Instant fromInstant;
        Instant toInstant;
        try {
            fromInstant = from != null && !from.isBlank() ? Instant.parse(from) : null;
            toInstant = to != null && !to.isBlank() ? Instant.parse(to) : null;
        } catch (DateTimeParseException e) {
            return "{\"error\": \"Invalid date format. Use ISO-8601 (e.g. 2025-01-01T00:00:00Z).\"}";
        }

        try {
            Specification<AuditLog> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (action != null && !action.isBlank()) predicates.add(cb.equal(root.get("action"), action));
                if (outcome != null && !outcome.isBlank()) predicates.add(cb.equal(root.get("outcome"), outcome));
                if (fromInstant != null) predicates.add(cb.greaterThanOrEqualTo(root.get("ts"), fromInstant));
                if (toInstant != null) predicates.add(cb.lessThanOrEqualTo(root.get("ts"), toInstant));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            PageRequest pageable = PageRequest.of(safePage, 50, Sort.by(Sort.Direction.DESC, "ts"));
            List<AuditLogResponse> results = auditLogRepository.findAll(spec, pageable)
                    .map(AuditLogResponse::from)
                    .getContent();

            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error("Failed to retrieve audit logs: {}", e.getMessage());
            return "{\"error\": \"Failed to retrieve audit logs.\"}";
        }
    }

    @Tool(description = "Verify the integrity of the audit log hash chain. Returns validation result.")
    public String verifyAuditChain() {
        try {
            AuditVerifyResponse result = hashChainService.verifyChain();
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to verify audit chain: {}", e.getMessage());
            return "{\"error\": \"Failed to verify audit chain.\"}";
        }
    }
}
