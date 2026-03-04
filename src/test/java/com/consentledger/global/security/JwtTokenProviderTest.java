package com.consentledger.global.security;

import com.consentledger.global.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        // Reflection would be needed in real test; using a direct approach
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    @DisplayName("유효한 토큰 생성 및 검증 성공")
    void generateAndValidateToken() {
        UserDetails userDetails = new User("test@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtTokenProvider.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserEmailFromToken(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패")
    void invalidTokenReturnsFalse() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void emptyTokenReturnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }
}
