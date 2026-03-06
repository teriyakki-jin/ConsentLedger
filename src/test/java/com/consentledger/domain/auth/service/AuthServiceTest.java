package com.consentledger.domain.auth.service;

import com.consentledger.domain.auth.dto.LoginRequest;
import com.consentledger.domain.auth.dto.LoginResponse;
import com.consentledger.domain.user.entity.User;
import com.consentledger.fixture.UserFixture;
import com.consentledger.global.config.JwtConfig;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import com.consentledger.global.security.CustomUserDetails;
import com.consentledger.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtConfig jwtConfig;

    @InjectMocks private AuthService authService;

    private User user;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockAuth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("올바른 자격증명으로 로그인 - 토큰/사용자 정보/만료시간 모두 검증")
    void login_validCredentials_returnsCorrectResponse() {
        given(authenticationManager.authenticate(any())).willReturn(mockAuth);
        given(jwtTokenProvider.generateToken(any())).willReturn("mock-jwt-token");
        given(jwtConfig.getExpirationMs()).willReturn(3600000L);

        LoginResponse response = authService.login(buildRequest(user.getEmail(), "password"));

        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 INVALID_CREDENTIALS")
    void login_invalidPassword_throwsUnauthorized() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                authService.login(buildRequest("user@test.com", "wrongpassword")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private LoginRequest buildRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }
}
