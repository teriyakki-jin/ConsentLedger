package com.consentledger.fixture;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.entity.TransferStatus;
import com.consentledger.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public class TransferRequestFixture {

    public static TransferRequest requested(Consent consent, User requesterUser) {
        return TransferRequest.builder()
                .id(UUID.randomUUID())
                .consent(consent)
                .method("PULL")
                .status(TransferStatus.REQUESTED)
                .requesterUser(requesterUser)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static TransferRequest approved(Consent consent, User requesterUser, User approvedByUser) {
        return TransferRequest.builder()
                .id(UUID.randomUUID())
                .consent(consent)
                .method("PULL")
                .status(TransferStatus.APPROVED)
                .requesterUser(requesterUser)
                .approvedByUser(approvedByUser)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
