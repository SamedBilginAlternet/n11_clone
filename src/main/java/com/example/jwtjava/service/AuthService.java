package com.example.jwtjava.service;

import com.example.jwtjava.dto.AuthResponse;
import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RefreshRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
import com.example.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda.");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(RefreshRequest request) {
        // Validate refresh token (throws if invalid/expired/revoked)
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        // Token rotation: revoke old token, issue a fresh one
        refreshTokenService.revokeByToken(storedToken.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken.getToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revokeByToken(request.refreshToken());
    }
}
