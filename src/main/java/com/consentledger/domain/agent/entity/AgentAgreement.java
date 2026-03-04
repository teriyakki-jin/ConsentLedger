package com.consentledger.domain.agent.entity;

import com.consentledger.infra.persistence.converter.JsonbConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agent_agreements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AgentAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allowedMethods;

    @Column(nullable = false)
    private Instant validFrom;

    private Instant validUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isValid() {
        Instant now = Instant.now();
        return !now.isBefore(validFrom) && (validUntil == null || now.isBefore(validUntil));
    }
}
