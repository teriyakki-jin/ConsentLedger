package com.consentledger.domain.audit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditVerifyResponse {

    private final boolean valid;
    private final long totalLogs;
    private final long verifiedCount;
    private final Long firstBrokenLogId;
    private final String reason;

    public static AuditVerifyResponse valid(long totalLogs, long verifiedCount) {
        return AuditVerifyResponse.builder()
                .valid(true)
                .totalLogs(totalLogs)
                .verifiedCount(verifiedCount)
                .build();
    }

    public static AuditVerifyResponse broken(long totalLogs, long verifiedCount, Long firstBrokenLogId, String reason) {
        return AuditVerifyResponse.builder()
                .valid(false)
                .totalLogs(totalLogs)
                .verifiedCount(verifiedCount)
                .firstBrokenLogId(firstBrokenLogId)
                .reason(reason)
                .build();
    }
}
