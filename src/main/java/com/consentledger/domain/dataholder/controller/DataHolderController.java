package com.consentledger.domain.dataholder.controller;

import com.consentledger.domain.dataholder.dto.DataHolderSummary;
import com.consentledger.domain.dataholder.service.DataHolderService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Data Holders", description = "데이터 보유자 조회 API")
@RestController
@RequestMapping("/data-holders")
@RequiredArgsConstructor
public class DataHolderController {

    private final DataHolderService dataHolderService;

    @Operation(summary = "데이터 보유자 목록 조회")
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DataHolderSummary>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(dataHolderService.listAll()));
    }
}
