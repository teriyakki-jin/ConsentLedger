package com.consentledger.domain.audit.service;

import com.consentledger.domain.audit.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final HashChainService hashChainService;

    public void log(String action, String objectType, UUID objectId,
                    UUID actorUserId, UUID actorAgentId, Map<String, Object> payload) {
        log(action, objectType, objectId, actorUserId, actorAgentId, null, "SUCCESS", payload);
    }

    public void log(String action, String objectType, UUID objectId,
                    UUID actorUserId, UUID actorAgentId, String ipAddress,
                    String outcome, Map<String, Object> payload) {
        hashChainService.appendLog(action, objectType, objectId,
                actorUserId, actorAgentId, ipAddress, outcome, payload);
    }
}
