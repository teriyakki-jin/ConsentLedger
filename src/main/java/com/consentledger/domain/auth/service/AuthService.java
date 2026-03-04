package com.consentledger.domain.auth.service;

import com.consentledger.domain.auth.dto.LoginRequest;
import com.consentledger.domain.auth.dto.LoginResponse;
import com.consentledger.global.config.JwtConfig;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.global.security.CustomUserDetails;
import com.consentledger.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(userDetails);

            return LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtConfig.getExpirationMs() / 1000)
                    .userId(userDetails.getUserId())
                    .email(userDetails.getEmail())
                    .role(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                    .build();

        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
