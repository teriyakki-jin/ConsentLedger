package com.consentledger.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class AuditControllerIntegrationTest extends BaseControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@consentledger.com";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final String USER_EMAIL = "user@consentledger.com";
    private static final String USER_PASSWORD = "user1234";

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
    }

    @Test
    @DisplayName("GET /admin/audit-logs - 200 OK (admin)")
    void getAuditLogs_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /admin/audit-logs - 403 Forbidden (regular user)")
    void getAuditLogs_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/audit-logs/verify - 200 OK (admin)")
    void verifyAuditChain_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/admin/audit-logs/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").isBoolean());
    }

    @Test
    @DisplayName("GET /admin/reports/audit - 200 OK (admin, returns PDF)")
    void downloadAuditReport_asAdmin_returnsPdf() throws Exception {
        String from = Instant.now().minus(7, ChronoUnit.DAYS).toString();
        String to = Instant.now().toString();

        mockMvc.perform(get("/admin/reports/audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("GET /admin/audit-logs?action=CONSENT_CREATED - 200 OK with filter")
    void getAuditLogs_withActionFilter_returns200() throws Exception {
        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("action", "CONSENT_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /admin/reports/audit - 403 Forbidden (regular user)")
    void downloadAuditReport_asUser_returns403() throws Exception {
        String from = Instant.now().minus(7, ChronoUnit.DAYS).toString();
        String to = Instant.now().toString();

        mockMvc.perform(get("/admin/reports/audit")
                        .header("Authorization", "Bearer " + userToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isForbidden());
    }
}
