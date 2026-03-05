package com.consentledger.domain.audit.service;

import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.entity.AuditChainAnchor;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditChainAnchorRepository;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.global.util.HashUtils;
import com.consentledger.global.util.JsonUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HashChainService {

    private static final int VERIFY_BATCH_SIZE = 1000;

    @Value("${app.audit.genesis-hash}")
    private String genesisHash;

    private final AuditChainAnchorRepository anchorRepository;
    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public AuditLog appendLog(
            String action,
            String objectType,
            UUID objectId,
            UUID actorUserId,
            UUID actorAgentId,
            String ipAddress,
            String outcome,
            Map<String, Object> payload) {

        String payloadJson = payload != null ? JsonUtils.toCanonicalJson(payload) : "{}";
        String payloadHash = HashUtils.sha256(payloadJson);

        AuditChainAnchor anchor = anchorRepository.findAnchorForUpdate()
                .orElseThrow(() -> new IllegalStateException("Audit chain anchor not found"));

        String prevHash = anchor.getLastHash();
        // PostgreSQL은 마이크로초(6자리) 정밀도 → 절삭하여 DB 왕복 후에도 일관성 보장
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Map<String, Object> recordCore = buildRecordCore(
                action,
                actorAgentId != null ? actorAgentId.toString() : null,
                actorUserId != null ? actorUserId.toString() : null,
                ipAddress,
                objectId != null ? objectId.toString() : null,
                objectType,
                outcome,
                payloadHash,
                1,
                now.toString());

        String recordCoreJson = JsonUtils.toCanonicalJson(recordCore);
        String hash = HashUtils.sha256(prevHash + recordCoreJson);

        AuditLog logEntry = AuditLog.builder()
                .ts(now)
                .action(action)
                .objectType(objectType)
                .objectId(objectId)
                .actorUserId(actorUserId)
                .actorAgentId(actorAgentId)
                .ipAddress(ipAddress)
                .outcome(outcome)
                .payloadJson(payloadJson)
                .payloadHash(payloadHash)
                .prevHash(prevHash)
                .hash(hash)
                .schemaVersion(1)
                .build();

        AuditLog saved = auditLogRepository.save(logEntry);
        anchor.update(saved.getId(), hash);

        return saved;
    }

    @Transactional(readOnly = true)
    public AuditVerifyResponse verifyChain() {
        long verifiedCount = 0;
        String expectedPrevHash = genesisHash;
        long afterId = 0L;

        while (true) {
            List<AuditLog> batch = auditLogRepository.findBatchAfter(afterId, VERIFY_BATCH_SIZE);
            if (batch.isEmpty()) break;

            for (AuditLog auditLog : batch) {
                if (!auditLog.getPrevHash().equals(expectedPrevHash)) {
                    long total = auditLogRepository.count();
                    return AuditVerifyResponse.broken(total, verifiedCount, auditLog.getId(), "prev_hash mismatch");
                }

                String recomputedPayloadHash = HashUtils.sha256(
                        auditLog.getPayloadJson() != null ? auditLog.getPayloadJson() : "{}");
                if (!auditLog.getPayloadHash().equals(recomputedPayloadHash)) {
                    long total = auditLogRepository.count();
                    return AuditVerifyResponse.broken(total, verifiedCount, auditLog.getId(), "payload_hash mismatch");
                }

                Map<String, Object> recordCore = buildRecordCore(
                        auditLog.getAction(),
                        auditLog.getActorAgentId() != null ? auditLog.getActorAgentId().toString() : null,
                        auditLog.getActorUserId() != null ? auditLog.getActorUserId().toString() : null,
                        auditLog.getIpAddress(),
                        auditLog.getObjectId() != null ? auditLog.getObjectId().toString() : null,
                        auditLog.getObjectType(),
                        auditLog.getOutcome(),
                        auditLog.getPayloadHash(),
                        auditLog.getSchemaVersion(),
                        auditLog.getTs().toString());

                String recordCoreJson = JsonUtils.toCanonicalJson(recordCore);
                String recomputedHash = HashUtils.sha256(expectedPrevHash + recordCoreJson);

                if (!auditLog.getHash().equals(recomputedHash)) {
                    long total = auditLogRepository.count();
                    return AuditVerifyResponse.broken(total, verifiedCount, auditLog.getId(), "hash mismatch");
                }

                expectedPrevHash = auditLog.getHash();
                verifiedCount++;
                afterId = auditLog.getId();
            }

            // JPA 1차 캐시 정리로 OOM 방지
            entityManager.clear();

            if (batch.size() < VERIFY_BATCH_SIZE) break;
        }

        long total = auditLogRepository.count();
        return AuditVerifyResponse.valid(total, verifiedCount);
    }

    /**
     * record_core 맵을 일관된 순서로 구성한다.
     * appendLog와 verifyChain 양쪽에서 동일한 메서드를 사용해야 해시가 일치한다.
     */
    private Map<String, Object> buildRecordCore(
            String action, String actorAgentId, String actorUserId,
            String ipAddress, String objectId, String objectType,
            String outcome, String payloadHash, int schemaVersion, String ts) {

        Map<String, Object> core = new LinkedHashMap<>();
        core.put("action", action);
        core.put("actor_agent_id", actorAgentId);
        core.put("actor_user_id", actorUserId);
        core.put("ip_address", ipAddress);
        core.put("object_id", objectId);
        core.put("object_type", objectType);
        core.put("outcome", outcome);
        core.put("payload_hash", payloadHash);
        core.put("schema_version", schemaVersion);
        core.put("ts", ts);
        return core;
    }
}
