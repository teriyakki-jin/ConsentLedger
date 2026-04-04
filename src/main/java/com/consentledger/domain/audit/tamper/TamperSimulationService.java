package com.consentledger.domain.audit.tamper;

import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.service.HashChainService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * ADMIN 전용 데모용 해시 체인 변조 시뮬레이터.
 * audit_logs_immutable 트리거를 일시 비활성화하고 특정 로그 payload를 변조한 뒤,
 * 체인 검증을 실행하여 어느 블록에서 깨지는지 시각적으로 보여준다.
 *
 * <b>프로덕션에서는 절대 사용하면 안 됨.</b>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TamperSimulationService {

    @PersistenceContext
    private EntityManager em;

    private final HashChainService hashChainService;

    /**
     * 지정한 logId의 payload_json을 변조하고 체인 검증 결과를 반환한다.
     */
    @Transactional
    public TamperResult tamperAndVerify(long logId) {
        // 대상 로그 존재 확인
        Long target = (Long) em.createNativeQuery(
                "SELECT id FROM audit_logs WHERE id = :id")
                .setParameter("id", logId)
                .getResultStream()
                .findFirst()
                .map(r -> ((Number) r).longValue())
                .orElse(null);

        if (target == null) {
            throw new IllegalArgumentException("로그 ID " + logId + "를 찾을 수 없습니다.");
        }

        // 1. 트리거 비활성화 (데모 전용)
        em.createNativeQuery("ALTER TABLE audit_logs DISABLE TRIGGER audit_logs_immutable").executeUpdate();

        try {
            // 2. payload_json 변조
            em.createNativeQuery(
                    "UPDATE audit_logs SET payload_json = :tampered WHERE id = :id")
                    .setParameter("tampered", "{\"TAMPERED\":true,\"original\":\"data has been manipulated\"}")
                    .setParameter("id", logId)
                    .executeUpdate();

            log.warn("[DEMO] Audit log {} has been tampered for simulation", logId);
        } finally {
            // 3. 트리거 반드시 재활성화
            em.createNativeQuery("ALTER TABLE audit_logs ENABLE TRIGGER audit_logs_immutable").executeUpdate();
        }

        // 4. 체인 검증 실행
        AuditVerifyResponse verifyResult = hashChainService.verifyChain();

        return new TamperResult(logId, verifyResult);
    }

    /**
     * 변조된 로그를 원상복구한다 (데모 재사용을 위해).
     * payload_hash 기반으로 원본 복구는 불가하므로, 해당 로그를 원래 hash와 일치하도록
     * 실제로는 demo 전용 seed payload로 복구한다.
     * 간단한 방법으로 reset은 "전체 audit_logs에서 tampered 표시된 것만 삭제"가 아닌
     * payload_json을 "{}" 로 되돌리는 수준으로 처리 (hash는 여전히 불일치 — 변조 상태 유지).
     *
     * 실제 복구는 DB 재시작 + Flyway re-seed가 정석이나,
     * 데모용으로는 "복구 불가"를 보여주는 것 자체가 포인트.
     */
    public Map<String, Object> getStatus() {
        Long tamperedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM audit_logs WHERE payload_json LIKE '%TAMPERED%'")
                .getSingleResult();

        Long totalCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM audit_logs")
                .getSingleResult();

        return Map.of(
                "totalLogs", totalCount,
                "tamperedLogs", tamperedCount,
                "isTampered", tamperedCount > 0
        );
    }

    public record TamperResult(long tamperedLogId, AuditVerifyResponse verifyResult) {}
}
