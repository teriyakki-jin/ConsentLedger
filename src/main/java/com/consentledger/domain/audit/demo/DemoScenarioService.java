package com.consentledger.domain.audit.demo;

import com.consentledger.domain.audit.service.HashChainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * ADMIN 전용 데모 시나리오 생성 서비스.
 * AI 이상 탐지 데모를 위해 의심스러운 감사 로그 패턴을 생성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoScenarioService {

    // 시드 데이터 UUID (V9__seed_data.sql)
    private static final UUID ADMIN_USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID NORMAL_USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    private static final UUID AGENT_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID DATAHOLDER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");

    // 의심 행위자 (시나리오 전용 가상 UUID)
    private static final UUID SUSPICIOUS_USER_ID = UUID.fromString("f9000001-0000-0000-0000-000000000099");
    private static final UUID SUSPICIOUS_AGENT_ID = UUID.fromString("f9000002-0000-0000-0000-000000000099");

    private final HashChainService hashChainService;

    /**
     * 4가지 이상 패턴 시나리오를 생성한다.
     * @return 생성된 로그 수
     */
    public int generateAll() {
        int count = 0;
        count += generateAccountTakeover();
        count += generateDataExfiltration();
        count += generatePrivilegeAbuse();
        count += generateAbnormalBulkConsent();
        log.info("Demo anomaly scenario generated: {} logs", count);
        return count;
    }

    /** ACCOUNT_TAKEOVER: 연속 로그인 실패 후 성공 (다른 IP) */
    private int generateAccountTakeover() {
        UUID targetConsent = UUID.randomUUID();

        // 5번 로그인 실패 (의심 IP)
        for (int i = 0; i < 5; i++) {
            hashChainService.appendLog(
                    "LOGIN_FAILURE", "USER", SUSPICIOUS_USER_ID,
                    null, null,
                    "203.0.113." + (10 + i),
                    "FAILURE",
                    Map.of("reason", "bad_credentials", "attempt", i + 1));
        }
        // 로그인 성공 (동일 의심 IP)
        hashChainService.appendLog(
                "LOGIN_SUCCESS", "USER", SUSPICIOUS_USER_ID,
                SUSPICIOUS_USER_ID, null,
                "203.0.113.15",
                "SUCCESS",
                Map.of("note", "login_after_multiple_failures"));

        return 6;
    }

    /** DATA_EXFILTRATION: 짧은 시간 내 대량 전송 실행 */
    private int generateDataExfiltration() {
        for (int i = 0; i < 12; i++) {
            UUID transferId = UUID.randomUUID();
            hashChainService.appendLog(
                    "TRANSFER_EXECUTED", "TRANSFER_REQUEST", transferId,
                    null, SUSPICIOUS_AGENT_ID,
                    "198.51.100.42",
                    "SUCCESS",
                    Map.of("method", "PULL", "dataHolder", DATAHOLDER_ID.toString(),
                            "size_kb", 512 + (i * 128)));
        }
        return 12;
    }

    /** PRIVILEGE_ABUSE: 관리자 권한 남용 — 대량 감사 로그 열람 + 다수 사용자 조회 */
    private int generatePrivilegeAbuse() {
        // 대량 감사 로그 조회
        for (int i = 0; i < 8; i++) {
            hashChainService.appendLog(
                    "AUDIT_LOG_VIEWED", "AUDIT_LOG", null,
                    ADMIN_USER_ID, null,
                    "10.0.0.5",
                    "SUCCESS",
                    Map.of("page", i, "size", 1000, "note", "bulk_export_attempt"));
        }
        // 전체 사용자 목록 반복 조회
        for (int i = 0; i < 5; i++) {
            hashChainService.appendLog(
                    "USER_LIST_VIEWED", "USER", null,
                    ADMIN_USER_ID, null,
                    "10.0.0.5",
                    "SUCCESS",
                    Map.of("page", i));
        }
        return 13;
    }

    /** ABNORMAL_HOURS + 비정상 패턴: 새벽 시간대 동의 일괄 철회 시도 */
    private int generateAbnormalBulkConsent() {
        // 여러 동의를 빠르게 생성 후 즉시 철회 (비정상 패턴)
        for (int i = 0; i < 6; i++) {
            UUID consentId = UUID.randomUUID();
            hashChainService.appendLog(
                    "CONSENT_CREATED", "CONSENT", consentId,
                    SUSPICIOUS_USER_ID, null,
                    "172.16.0." + (100 + i),
                    "SUCCESS",
                    Map.of("dataHolder", DATAHOLDER_ID.toString(), "scopes", "[\"ALL\"]"));
            hashChainService.appendLog(
                    "CONSENT_REVOKED", "CONSENT", consentId,
                    SUSPICIOUS_USER_ID, null,
                    "172.16.0." + (100 + i),
                    "SUCCESS",
                    Map.of("reason", "immediate_revoke_suspicious"));
        }
        return 12;
    }
}
