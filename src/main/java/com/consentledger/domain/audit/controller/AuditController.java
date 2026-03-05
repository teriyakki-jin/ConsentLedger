package com.consentledger.domain.audit.controller;

import com.consentledger.domain.audit.dto.AuditLogResponse;
import com.consentledger.domain.audit.dto.AuditVerifyResponse;
import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.consentledger.domain.audit.service.AuditReportService;
import com.consentledger.domain.audit.service.HashChainService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;
import java.util.List;

@Tag(name = "Admin - Audit", description = "관리자 감사 로그 API")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final HashChainService hashChainService;
    private final AuditReportService auditReportService;

    @Operation(summary = "감사 로그 조회", description = "감사 로그를 필터링하여 조회합니다. (ADMIN 전용)")
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (objectType != null) predicates.add(cb.equal(root.get("objectType"), objectType));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("ts"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("ts"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ts"));
        Page<AuditLogResponse> result = auditLogRepository.findAll(spec, pageable)
                .map(AuditLogResponse::from);

        return ResponseEntity.ok(ApiResponse.ok(
                result.getContent(),
                new ApiResponse.Meta(result.getTotalElements(), page, size)));
    }

    @Operation(summary = "감사 로그 무결성 검증", description = "전체 감사 로그 해시 체인 무결성을 검증합니다. (ADMIN 전용)")
    @GetMapping("/audit-logs/verify")
    public ResponseEntity<ApiResponse<AuditVerifyResponse>> verifyChain() {
        return ResponseEntity.ok(ApiResponse.ok(hashChainService.verifyChain()));
    }

    @Operation(summary = "감사 리포트 PDF 다운로드", description = "기간별 감사 로그를 PDF로 다운로드합니다. (ADMIN 전용)")
    @GetMapping("/reports/audit")
    public ResponseEntity<byte[]> downloadAuditReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        byte[] pdf = auditReportService.generatePdfReport(from, to);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"audit-report.pdf\"")
                .body(pdf);
    }
}
