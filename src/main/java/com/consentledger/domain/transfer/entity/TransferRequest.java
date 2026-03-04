package com.consentledger.domain.transfer.entity;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.user.entity.User;
import com.consentledger.domain.agent.entity.Agent;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id", nullable = false)
    private Consent consent;

    @Column(nullable = false, length = 50)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private User requesterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_agent_id")
    private Agent requesterAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public void approve(User approver) {
        this.status = TransferStatus.APPROVED;
        this.approvedByUser = approver;
    }

    public void deny() {
        this.status = TransferStatus.DENIED;
    }

    public void complete() {
        this.status = TransferStatus.COMPLETED;
    }

    public void fail() {
        this.status = TransferStatus.FAILED;
    }
}
