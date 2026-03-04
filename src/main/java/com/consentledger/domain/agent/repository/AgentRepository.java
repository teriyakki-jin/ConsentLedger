package com.consentledger.domain.agent.repository;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.entity.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByClientId(String clientId);

    Optional<Agent> findByApiKeyHashAndStatus(String apiKeyHash, AgentStatus status);
}
