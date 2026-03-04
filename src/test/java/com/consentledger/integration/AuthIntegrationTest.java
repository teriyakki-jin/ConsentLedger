package com.consentledger.integration;

import com.consentledger.domain.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("관리자 계정으로 로그인 성공")
    void loginWithAdminCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        // Use reflection to set fields since @Getter only
        var emailField = LoginRequest.class.getDeclaredField("email");
        var passwordField = LoginRequest.class.getDeclaredField("password");
        emailField.setAccessible(true);
        passwordField.setAccessible(true);
        emailField.set(request, "admin@consentledger.com");
        passwordField.set(request, "admin1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void loginWithWrongPasswordFails() throws Exception {
        LoginRequest request = new LoginRequest();
        var emailField = LoginRequest.class.getDeclaredField("email");
        var passwordField = LoginRequest.class.getDeclaredField("password");
        emailField.setAccessible(true);
        passwordField.setAccessible(true);
        emailField.set(request, "admin@consentledger.com");
        passwordField.set(request, "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("이메일 없이 로그인 시 400 에러")
    void loginWithoutEmailFails() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"admin1234\"}"))
                .andExpect(status().isBadRequest());
    }
}
