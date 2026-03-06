package com.consentledger.fixture;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.entity.ConsentStatus;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ConsentFixture {

    public static Consent active(User user, DataHolder dataHolder) {
        return Consent.builder()
                .id(UUID.randomUUID())
                .user(user)
                .dataHolder(dataHolder)
                .scopes(List.of("READ", "WRITE"))
                .status(ConsentStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Consent revoked(User user, DataHolder dataHolder) {
        return Consent.builder()
                .id(UUID.randomUUID())
                .user(user)
                .dataHolder(dataHolder)
                .scopes(List.of("READ"))
                .status(ConsentStatus.REVOKED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static DataHolder dataHolder() {
        return DataHolder.builder()
                .id(UUID.randomUUID())
                .institutionCode("BANK01")
                .name("Test Bank")
                .supportedMethods(List.of("PULL", "PUSH"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
