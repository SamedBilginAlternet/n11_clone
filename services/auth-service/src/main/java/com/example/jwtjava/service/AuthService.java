package com.example.jwtjava.service;

import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
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

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final SagaEventPublisher sagaEventPublisher;

    public record TokenPair(String accessToken, String refreshToken) {}

    public TokenPair register(RegisterRequest request) {
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

        sagaEventPublisher.publishUserRegistered(
                UserRegisteredEvent.of(user.getId(), user.getEmail(), user.getFullName())
        );

        return issueTokens(user);
    }

    public TokenPair login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", request.email()));

        return issueTokens(user);
    }

    public TokenPair refresh(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

        User user = storedToken.getUser();

        refreshTokenService.revokeByToken(storedToken.getToken());

        return issueTokens(user);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenService.revokeByToken(refreshTokenValue);
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new TokenPair(accessToken, refreshToken.getToken());
    }
}
