package com.consentledger.mcp.tools;

import com.consentledger.domain.admin.dto.UserSummary;
import com.consentledger.domain.admin.service.AdminService;
import com.consentledger.domain.user.entity.User;
import com.consentledger.fixture.UserFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMcpToolsTest {

    @Mock private AdminService adminService;

    @InjectMocks private AdminMcpTools adminMcpTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(adminMcpTools, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("list_users: 사용자 목록 JSON 반환")
    void listUsers_returnsSerializedUsers() {
        User admin = UserFixture.admin();
        User user = UserFixture.user();
        given(adminService.listUsers()).willReturn(List.of(
                UserSummary.from(admin),
                UserSummary.from(user)
        ));

        String result = adminMcpTools.listUsers();

        verify(adminService).listUsers();
        assertThat(result).contains(admin.getEmail());
        assertThat(result).contains(user.getEmail());
        assertThat(result).contains("\"role\":\"ADMIN\"");
        assertThat(result).contains("\"role\":\"USER\"");
    }

    @Test
    @DisplayName("list_users: 예외 발생 시 에러 JSON 반환")
    void listUsers_whenServiceFails_returnsErrorJson() {
        given(adminService.listUsers()).willThrow(new RuntimeException("DB unavailable"));

        String result = adminMcpTools.listUsers();

        assertThat(result).isEqualTo("{\"error\": \"Failed to retrieve users.\"}");
    }
}
