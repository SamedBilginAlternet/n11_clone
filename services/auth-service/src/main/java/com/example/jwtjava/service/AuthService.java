package com.example.jwtjava.service;

import com.example.jwtjava.dto.AuthResponse;
import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RefreshRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
import java.util.EnumSet;
import com.example.jwtjava.exception.ResourceNotFoundException;
import com.example.jwtjava.exception.UserAlreadyExistsException;
import com.example.jwtjava.repository.UserRepository;
import com.example.jwtjava.saga.SagaEventPublisher;
import com.example.jwtjava.saga.UserRegisteredEvent;
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
    private final SagaEventPublisher sagaEventPublisher;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(EnumSet.of(Role.USER))
                .build();

        userRepository.save(user);

        // Kick off the registration saga — basket-service will create an empty
        // basket for this user, and compensate back to us if it fails.
        sagaEventPublisher.publishUserRegistered(
                UserRegisteredEvent.of(user.getId(), user.getEmail(), user.getFullName())
        );

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", request.email()));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        // Token rotation: revoke old, issue new
        refreshTokenService.revokeByToken(storedToken.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken.getToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revokeByToken(request.refreshToken());
    }
}
