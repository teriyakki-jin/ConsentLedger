package com.consentledger.global.security;

import com.consentledger.global.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "secret", SECRET);
        ReflectionTestUtils.setField(jwtConfig, "expirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtConfig, "refreshExpirationMs", EXPIRATION_MS * 7);

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
