package com.consentledger.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UUID userId;
    private final String email;
    private final String role;
}
