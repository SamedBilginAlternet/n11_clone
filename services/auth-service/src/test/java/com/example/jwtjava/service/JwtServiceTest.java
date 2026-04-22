package com.example.jwtjava.service;

import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "3f6a2b8c1d4e5f7a9b0c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 900_000L);
    }

    private User testUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded")
                .fullName("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Generates a non-blank access token")
    void generateAccessToken_returnsToken() {
        String token = jwtService.generateAccessToken(testUser());
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Extracts correct username from token")
    void extractUsername_returnsEmail() {
        User user = testUser();
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Token is valid for the user it was generated for")
    void isTokenValid_trueForSameUser() {
        User user = testUser();
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Token is invalid for a different user")
    void isTokenValid_falseForDifferentUser() {
        User owner = testUser();
        User other = User.builder()
                .id(2L).email("other@example.com")
                .password("encoded").fullName("Other").role(Role.USER).build();

        String token = jwtService.generateAccessToken(owner);
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("Expired token fails validation")
    void isTokenValid_falseForExpiredToken() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secretKey", SECRET);
        ReflectionTestUtils.setField(shortLivedService, "accessTokenExpiry", -1L); // already expired

        User user = testUser();
        String token = shortLivedService.generateAccessToken(user);
        assertThatThrownBy(() -> shortLivedService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
