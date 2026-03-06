package com.consentledger.fixture;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.entity.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public class AgentFixture {

    public static Agent active() {
        return Agent.builder()
                .id(UUID.randomUUID())
                .name("Test Agent")
                .status(AgentStatus.ACTIVE)
                .clientId("test-client-" + UUID.randomUUID().toString().substring(0, 8))
                .apiKeyHash("dummyhash64charlongxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .lastRotatedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Agent suspended() {
        return Agent.builder()
                .id(UUID.randomUUID())
                .name("Suspended Agent")
                .status(AgentStatus.SUSPENDED)
                .clientId("suspended-client")
                .apiKeyHash("dummyhash64charlongxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .lastRotatedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
