package com.consentledger.domain.transfer.controller;

import com.consentledger.domain.transfer.dto.TransferCreateRequest;
import com.consentledger.domain.transfer.dto.TransferResponse;
import com.consentledger.domain.transfer.service.TransferRequestService;
import com.consentledger.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Transfer Requests", description = "전송 요청 API")
@RestController
@RequestMapping("/transfer-requests")
@RequiredArgsConstructor
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    @Operation(summary = "전송 요청 생성", description = "사용자 또는 대리인이 전송 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> create(@Valid @RequestBody TransferCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(transferRequestService.create(request)));
    }

    @Operation(summary = "전송 요청 목록 조회", description = "본인과 관련된 전송 요청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransferResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(transferRequestService.listMine()));
    }

    @Operation(summary = "전송 요청 승인", description = "사용자가 전송 요청을 승인합니다.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<TransferResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transferRequestService.approve(id)));
    }

    @Operation(summary = "전송 실행", description = "승인된 전송 요청을 실행합니다.")
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<TransferResponse>> execute(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transferRequestService.execute(id)));
    }
}
