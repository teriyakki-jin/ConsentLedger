package com.consentledger.domain.audit.service;

import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.fixture.AuditLogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HashChainService hashChainService;

    @InjectMocks private AuditReportService auditReportService;

    @Test
    @DisplayName("PDF 생성 성공 - byte 배열 반환")
    void generatePdf_returnsByteArray() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(from, to)).willReturn(List.of());

        byte[] result = auditReportService.generatePdfReport(from, to);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("PDF 생성 결과는 비어 있지 않아야 한다")
    void generatePdf_notEmpty() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(from, to)).willReturn(List.of());

        byte[] result = auditReportService.generatePdfReport(from, to);

        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("기간 필터링 - 올바른 범위를 repository에 전달한다")
    void generatePdf_dateRangeFiltering() {
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-12-31T23:59:59Z");

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(from, to)).willReturn(List.of());

        auditReportService.generatePdfReport(from, to);

        verify(auditLogRepository).findByTsBetweenOrderByTsAsc(from, to);
    }

    @Test
    @DisplayName("빈 로그로도 PDF 생성 성공")
    void generatePdf_emptyLogs_stillGenerates() {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now();

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(any(), any())).willReturn(List.of());

        byte[] result = auditReportService.generatePdfReport(from, to);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("감사 로그가 있을 때 PDF 생성 성공")
    void generatePdf_withLogs_includesData() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        AuditLog log = AuditLogFixture.firstLog();

        given(auditLogRepository.findByTsBetweenOrderByTsAsc(from, to)).willReturn(List.of(log));

        byte[] result = auditReportService.generatePdfReport(from, to);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }
}
