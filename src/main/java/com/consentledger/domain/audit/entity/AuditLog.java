package com.consentledger.domain.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant ts;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String objectType;

    private UUID objectId;

    private UUID actorUserId;

    private UUID actorAgentId;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(columnDefinition = "jsonb")
    private String payloadJson;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false, length = 64)
    private String prevHash;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;

    @Column(nullable = false)
    private Integer schemaVersion;
}
