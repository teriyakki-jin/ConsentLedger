package com.consentledger.domain.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status;

    @Column(nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(nullable = false, length = 64)
    private String apiKeyHash;

    @Column(nullable = false)
    private Instant lastRotatedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public void rotateApiKey(String newApiKeyHash) {
        this.apiKeyHash = newApiKeyHash;
        this.lastRotatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == AgentStatus.ACTIVE;
    }
}
