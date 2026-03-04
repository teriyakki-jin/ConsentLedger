package com.consentledger.domain.agent.repository;

import com.consentledger.domain.agent.entity.AgentAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AgentAgreementRepository extends JpaRepository<AgentAgreement, UUID> {

    @Query("""
        SELECT aa FROM AgentAgreement aa
        WHERE aa.agent.id = :agentId
          AND aa.validFrom <= :now
          AND (aa.validUntil IS NULL OR aa.validUntil > :now)
        """)
    List<AgentAgreement> findValidAgreements(@Param("agentId") UUID agentId, @Param("now") Instant now);
}
