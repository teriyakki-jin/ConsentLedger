package com.consentledger.domain.audit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_chain_anchor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditChainAnchor {

    @Id
    private Integer id = 1;

    private Long lastLogId;

    @Column(nullable = false, length = 64)
    private String lastHash;

    @Column(nullable = false)
    private Instant updatedAt;

    public void update(Long newLogId, String newHash) {
        this.lastLogId = newLogId;
        this.lastHash = newHash;
        this.updatedAt = Instant.now();
    }
}
