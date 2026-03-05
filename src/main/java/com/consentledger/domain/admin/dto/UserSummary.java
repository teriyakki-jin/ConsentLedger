package com.consentledger.domain.admin.dto;

import com.consentledger.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserSummary {
    private UUID id;
    private String email;
    private String role;
    private Instant createdAt;

    public static UserSummary from(User user) {
        return UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
