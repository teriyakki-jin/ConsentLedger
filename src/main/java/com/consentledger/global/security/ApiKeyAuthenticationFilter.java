package com.consentledger.global.security;

import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.entity.AgentStatus;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.global.util.HashUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final AgentRepository agentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHash = HashUtils.sha256(apiKey);
        Optional<Agent> agentOpt = agentRepository.findByApiKeyHashAndStatus(apiKeyHash, AgentStatus.ACTIVE);

        if (agentOpt.isEmpty()) {
            log.debug("Invalid API key provided");
            filterChain.doFilter(request, response);
            return;
        }

        Agent agent = agentOpt.get();
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(agent);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
