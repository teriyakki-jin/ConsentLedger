package com.consentledger.fixture;

import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.global.util.HashUtils;
import com.consentledger.global.util.JsonUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditLogFixture {

    public static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Creates a valid first audit log chained to genesisHash.
     */
    public static AuditLog firstLog() {
        String payloadJson = "{}";
        String payloadHash = HashUtils.sha256(payloadJson);
        Instant ts = Instant.now().truncatedTo(ChronoUnit.MICROS);

        String hash = computeHash(GENESIS_HASH, "TEST_ACTION", null, null,
                null, null, "TEST_OBJECT", "SUCCESS", payloadHash, 1, ts.toString());

        return AuditLog.builder()
                .id(1L)
                .ts(ts)
                .action("TEST_ACTION")
                .objectType("TEST_OBJECT")
                .outcome("SUCCESS")
                .payloadJson(payloadJson)
                .payloadHash(payloadHash)
                .prevHash(GENESIS_HASH)
                .hash(hash)
                .schemaVersion(1)
                .build();
    }

    /**
     * Creates a valid log chained after the previous log.
     */
    public static AuditLog nextLog(AuditLog previous, Long id) {
        String payloadJson = "{}";
        String payloadHash = HashUtils.sha256(payloadJson);
        Instant ts = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String prevHash = previous.getHash();

        String hash = computeHash(prevHash, "TEST_ACTION", null, null,
                null, null, "TEST_OBJECT", "SUCCESS", payloadHash, 1, ts.toString());

        return AuditLog.builder()
                .id(id)
                .ts(ts)
                .action("TEST_ACTION")
                .objectType("TEST_OBJECT")
                .outcome("SUCCESS")
                .payloadJson(payloadJson)
                .payloadHash(payloadHash)
                .prevHash(prevHash)
                .hash(hash)
                .schemaVersion(1)
                .build();
    }

    /**
     * Creates a log with a corrupted payloadHash.
     */
    public static AuditLog withBrokenPayloadHash(AuditLog log) {
        return AuditLog.builder()
                .id(log.getId())
                .ts(log.getTs())
                .action(log.getAction())
                .objectType(log.getObjectType())
                .outcome(log.getOutcome())
                .payloadJson(log.getPayloadJson())
                .payloadHash("0000000000000000000000000000000000000000000000000000000000000000")
                .prevHash(log.getPrevHash())
                .hash(log.getHash())
                .schemaVersion(log.getSchemaVersion())
                .build();
    }

    /**
     * Creates a log with a wrong prevHash (pointing to random hash).
     */
    public static AuditLog withBrokenPrevHash(AuditLog log) {
        return AuditLog.builder()
                .id(log.getId())
                .ts(log.getTs())
                .action(log.getAction())
                .objectType(log.getObjectType())
                .outcome(log.getOutcome())
                .payloadJson(log.getPayloadJson())
                .payloadHash(log.getPayloadHash())
                .prevHash("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")
                .hash(log.getHash())
                .schemaVersion(log.getSchemaVersion())
                .build();
    }

    private static String computeHash(
            String prevHash, String action, String actorAgentId, String actorUserId,
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

        String recordCoreJson = JsonUtils.toCanonicalJson(core);
        return HashUtils.sha256(prevHash + recordCoreJson);
    }
}
