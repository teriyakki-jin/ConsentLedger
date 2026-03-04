package com.consentledger.global.security;

import com.consentledger.domain.agent.entity.Agent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UUID> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails.getUserId());
        }
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails.getUserId());
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentAgentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken apiKeyToken) {
            return Optional.of(apiKeyToken.getAgent().getId());
        }
        return Optional.empty();
    }

    public static boolean isAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof ApiKeyAuthenticationToken;
    }

    public static boolean isUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CustomUserDetails;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public static Optional<Agent> getCurrentAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken apiKeyToken) {
            return Optional.of(apiKeyToken.getAgent());
        }
        return Optional.empty();
    }
}
