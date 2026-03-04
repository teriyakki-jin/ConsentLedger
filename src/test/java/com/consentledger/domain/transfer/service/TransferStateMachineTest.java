package com.consentledger.domain.transfer.service;

import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferStateMachineTest {

    private final TransferStateMachine stateMachine = new TransferStateMachine();

    private TransferRequest mockWithStatus(TransferStatus status) {
        TransferRequest tr = mock(TransferRequest.class);
        when(tr.getStatus()).thenReturn(status);
        return tr;
    }

    @Test
    @DisplayName("REQUESTED -> APPROVED 전이 허용")
    void requestedToApproved() {
        assertThatNoException().isThrownBy(() -> stateMachine.approve(mockWithStatus(TransferStatus.REQUESTED)));
    }

    @Test
    @DisplayName("REQUESTED -> DENIED 전이 허용")
    void requestedToDenied() {
        assertThatNoException().isThrownBy(() -> stateMachine.deny(mockWithStatus(TransferStatus.REQUESTED)));
    }

    @Test
    @DisplayName("APPROVED -> COMPLETED 전이 허용")
    void approvedToCompleted() {
        assertThatNoException().isThrownBy(() -> stateMachine.complete(mockWithStatus(TransferStatus.APPROVED)));
    }

    @Test
    @DisplayName("APPROVED -> FAILED 전이 허용")
    void approvedToFailed() {
        assertThatNoException().isThrownBy(() -> stateMachine.fail(mockWithStatus(TransferStatus.APPROVED)));
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"DENIED", "COMPLETED", "FAILED"})
    @DisplayName("종결 상태에서 승인 시도는 예외")
    void terminalStatusCannotBeApproved(TransferStatus status) {
        assertThatThrownBy(() -> stateMachine.approve(mockWithStatus(status)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_INVALID_TRANSITION);
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"REQUESTED", "DENIED", "COMPLETED", "FAILED"})
    @DisplayName("APPROVED 이외의 상태에서 완료 시도는 예외")
    void nonApprovedStatusCannotBeCompleted(TransferStatus status) {
        assertThatThrownBy(() -> stateMachine.complete(mockWithStatus(status)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSFER_INVALID_TRANSITION);
    }
}
