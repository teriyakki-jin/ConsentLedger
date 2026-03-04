package com.consentledger.domain.transfer.service;

import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TransferStateMachine {

    private static final Map<TransferStatus, Set<TransferStatus>> ALLOWED_TRANSITIONS = Map.of(
            TransferStatus.REQUESTED, Set.of(TransferStatus.APPROVED, TransferStatus.DENIED),
            TransferStatus.APPROVED, Set.of(TransferStatus.COMPLETED, TransferStatus.FAILED),
            TransferStatus.DENIED, Set.of(),
            TransferStatus.COMPLETED, Set.of(),
            TransferStatus.FAILED, Set.of()
    );

    public void validateTransition(TransferStatus current, TransferStatus target) {
        Set<TransferStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_TRANSITION,
                    String.format("Cannot transition from %s to %s", current, target));
        }
    }

    public void approve(TransferRequest request) {
        validateTransition(request.getStatus(), TransferStatus.APPROVED);
    }

    public void deny(TransferRequest request) {
        validateTransition(request.getStatus(), TransferStatus.DENIED);
    }

    public void complete(TransferRequest request) {
        validateTransition(request.getStatus(), TransferStatus.COMPLETED);
    }

    public void fail(TransferRequest request) {
        validateTransition(request.getStatus(), TransferStatus.FAILED);
    }
}
