package com.example.jwtjava.controller;

import com.example.jwtjava.saga.SagaEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockbean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ConnectionFactory connectionFactory;
    @MockBean SagaEventPublisher sagaEventPublisher;

    // ── /api/users/me ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/users/me without token → 401")
    void getMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/users/me"));
    }

    @Test
    @DisplayName("GET /api/users/me with valid token → 200 with user data")
    void getMe_withToken_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.roles").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/users/me with malformed token → 401")
    void getMe_malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer this.is.not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ── /api/users/admin ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/users/admin with USER role → 403")
    void adminEndpoint_userRole_returns403() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/api/users/admin")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/users/admin"));
    }

    @Test
    @DisplayName("GET /api/users/admin without token → 401")
    void adminEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@example.com",
                                  "password": "Secure1!",
                                  "fullName": "Test User" }
                                """))
                .andReturn();

        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
