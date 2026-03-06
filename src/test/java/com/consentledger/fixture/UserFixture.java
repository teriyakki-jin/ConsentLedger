package com.consentledger.fixture;

import com.consentledger.domain.user.entity.Role;
import com.consentledger.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public class UserFixture {

    public static User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static User admin() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static User userWithId(UUID id) {
        return User.builder()
                .id(id)
                .email("user-" + id.toString().substring(0, 8) + "@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
