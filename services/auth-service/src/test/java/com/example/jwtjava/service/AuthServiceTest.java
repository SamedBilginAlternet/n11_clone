package com.example.jwtjava.service;

import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
import com.example.jwtjava.exception.UserAlreadyExistsException;
import com.example.jwtjava.repository.UserRepository;
import com.example.jwtjava.saga.SagaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuthenticationManager authenticationManager;
    @Mock SagaEventPublisher sagaEventPublisher;

    @InjectMocks AuthService authService;

    private User stubUser() {
        return User.builder()
                .id(1L).email("user@example.com")
                .password("encoded").fullName("Test User").roles(EnumSet.of(Role.USER))
                .build();
    }

    private RefreshToken stubRefreshToken(User user) {
        return RefreshToken.builder()
                .token("refresh-uuid")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register returns tokens when email is new")
    void register_success() {
        var req = new RegisterRequest("user@example.com", "Secure1!", "Test User");
        User user = stubUser();

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(stubRefreshToken(user));

        var response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
    }

    @Test
    @DisplayName("register throws UserAlreadyExistsException when email is taken")
    void register_emailTaken() {
        var req = new RegisterRequest("taken@example.com", "Secure1!", "Test User");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login returns tokens on valid credentials")
    void login_success() {
        var req = new LoginRequest("user@example.com", "Secure1!");
        User user = stubUser();

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(stubRefreshToken(user));

        var response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login throws when credentials are wrong")
    void login_badCredentials() {
        var req = new LoginRequest("user@example.com", "wrongpass");
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
