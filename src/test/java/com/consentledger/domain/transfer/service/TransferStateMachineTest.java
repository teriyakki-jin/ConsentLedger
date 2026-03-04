package com.consentledger.domain.transfer.service;

import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TransferStateMachineTest {

    private final TransferStateMachine stateMachine = new TransferStateMachine();

    private TransferRequest requestWithStatus(TransferStatus status) {
        TransferRequest tr = new TransferRequest();
        ReflectionTestUtils.setField(tr, "status", status);
        return tr;
    }

    @Test
    @DisplayName("REQUESTED -> APPROVED 전이 허용")
    void requestedToApproved() {
        TransferRequest tr = requestWithStatus(TransferStatus.REQUESTED);
        assertThatNoException().isThrownBy(() -> stateMachine.approve(tr));
    }

    @Test
    @DisplayName("REQUESTED -> DENIED 전이 허용")
    void requestedToDenied() {
        TransferRequest tr = requestWithStatus(TransferStatus.REQUESTED);
        assertThatNoException().isThrownBy(() -> stateMachine.deny(tr));
    }

    @Test
    @DisplayName("APPROVED -> COMPLETED 전이 허용")
    void approvedToCompleted() {
        TransferRequest tr = requestWithStatus(TransferStatus.APPROVED);
        assertThatNoException().isThrownBy(() -> stateMachine.complete(tr));
    }

    @Test
    @DisplayName("APPROVED -> FAILED 전이 허용")
    void approvedToFailed() {
        TransferRequest tr = requestWithStatus(TransferStatus.APPROVED);
        assertThatNoException().isThrownBy(() -> stateMachine.fail(tr));
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"DENIED", "COMPLETED", "FAILED"})
    @DisplayName("종결 상태에서 승인 시도는 예외")
    void terminalStatusCannotBeApproved(TransferStatus status) {
        TransferRequest tr = requestWithStatus(status);
        assertThatThrownBy(() -> stateMachine.approve(tr))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_INVALID_TRANSITION);
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"REQUESTED", "DENIED", "COMPLETED", "FAILED"})
    @DisplayName("APPROVED 이외의 상태에서 완료 시도는 예외")
    void nonApprovedStatusCannotBeCompleted(TransferStatus status) {
        TransferRequest tr = requestWithStatus(status);
        assertThatThrownBy(() -> stateMachine.complete(tr))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_INVALID_TRANSITION);
    }
}
