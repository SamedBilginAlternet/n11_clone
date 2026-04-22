package com.example.jwtjava.controller;

import com.example.jwtjava.saga.SagaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        SagaEventPublisher sagaEventPublisher() {
            return Mockito.mock(SagaEventPublisher.class);
        }
    }

    @Autowired MockMvc mockMvc;

    private static final String EMAIL    = "user@example.com";
    private static final String PASSWORD = "Secure1!";
    private static final String NAME     = "Test User";
    private static final String COOKIE   = "refresh_token";

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register → 201 with accessToken + refresh cookie")
    void register_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, PASSWORD, NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists(COOKIE))
                .andExpect(cookie().httpOnly(COOKIE, true));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email → 409 problem+json")
    void register_duplicateEmail_returns409() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, PASSWORD, NAME)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/auth/register with weak password → 400 with fields")
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, "weak", NAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid email → 400 with fields")
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", PASSWORD, NAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").isNotEmpty());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login → 200 with accessToken + refresh cookie")
    void login_success() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists(COOKIE));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password → 401")
    void login_wrongPassword_returns401() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "WrongPass1!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login with unknown email → 401")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/refresh with cookie → 200 with new tokens")
    void refresh_success() throws Exception {
        Cookie refreshCookie = registerUser();

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists(COOKIE));
    }

    @Test
    @DisplayName("POST /api/auth/refresh with used token → 401 (rotation enforced)")
    void refresh_reuse_returns401() throws Exception {
        Cookie refreshCookie = registerUser();

        // First use — succeeds
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie));

        // Second use — old token was revoked
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh with bogus cookie → 400")
    void refresh_bogusToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(COOKIE, "this-is-not-a-real-token")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh without cookie → 400")
    void refresh_noCookie_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/logout → 204 + clears cookie")
    void logout_success() throws Exception {
        Cookie refreshCookie = registerUser();

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));
    }

    @Test
    @DisplayName("POST /api/auth/logout invalidates the refresh token")
    void logout_thenRefresh_returns401() throws Exception {
        Cookie refreshCookie = registerUser();

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Cookie registerUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, PASSWORD, NAME)))
                .andReturn();
        return result.getResponse().getCookie(COOKIE);
    }

    private String registerJson(String email, String password, String name) {
        return """
                { "email": "%s", "password": "%s", "fullName": "%s" }
                """.formatted(email, password, name);
    }

    private String loginJson(String email, String password) {
        return """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);
    }
}
