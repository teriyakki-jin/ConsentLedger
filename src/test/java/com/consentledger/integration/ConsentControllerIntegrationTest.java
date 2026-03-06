package com.consentledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class ConsentControllerIntegrationTest extends BaseControllerIntegrationTest {

    private static final String USER_EMAIL = "user@consentledger.com";
    private static final String USER_PASSWORD = "user1234";

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
    }

    @Test
    @DisplayName("POST /consents - 201 Created")
    void createConsent_returns201() throws Exception {
        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dataHolderId", DATA_HOLDER_ID,
                                "scopes", List.of("READ")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /consents - 200 OK")
    void listConsents_returns200() throws Exception {
        mockMvc.perform(get("/consents")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /consents/{id} - 200 OK for own consent")
    void getConsentById_ownConsent_returns200() throws Exception {
        String consentId = createConsentAndGetId(userToken);

        mockMvc.perform(get("/consents/" + consentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(consentId));
    }

    @Test
    @DisplayName("POST /consents/{id}/revoke - 200 OK")
    void revokeConsent_returns200() throws Exception {
        String consentId = createConsentAndGetId(userToken);

        mockMvc.perform(post("/consents/" + consentId + "/revoke")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REVOKED"));
    }

    @Test
    @DisplayName("POST /consents - 400 Bad Request (missing scopes)")
    void createConsent_missingScopes_returns400() throws Exception {
        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dataHolderId", DATA_HOLDER_ID
                                // scopes omitted
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /consents - 403 Forbidden (no token)")
    void listConsents_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/consents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /consents/{id} - 404 Not Found (unknown ID)")
    void getConsentById_notFound_returns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();

        mockMvc.perform(get("/consents/" + unknownId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /consents/{id}/revoke - 409 Conflict (already revoked)")
    void revokeConsent_alreadyRevoked_returns409() throws Exception {
        String consentId = createConsentAndGetId(userToken);

        mockMvc.perform(post("/consents/" + consentId + "/revoke")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/consents/" + consentId + "/revoke")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict());
    }
}
