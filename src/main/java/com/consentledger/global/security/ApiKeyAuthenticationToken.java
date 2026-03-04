package com.consentledger.global.security;

import com.consentledger.domain.agent.entity.Agent;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Agent agent;

    public ApiKeyAuthenticationToken(Agent agent) {
        super(List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
        this.agent = agent;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return agent;
    }

    public Agent getAgent() {
        return agent;
    }
}
