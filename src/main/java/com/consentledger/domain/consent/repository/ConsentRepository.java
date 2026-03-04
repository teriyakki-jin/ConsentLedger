package com.consentledger.domain.consent.repository;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.entity.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Consent> findByAgentIdOrderByCreatedAtDesc(UUID agentId);

    Optional<Consent> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT c FROM Consent c
        WHERE c.id = :id
          AND c.status = 'ACTIVE'
        """)
    Optional<Consent> findActiveById(@Param("id") UUID id);
}
