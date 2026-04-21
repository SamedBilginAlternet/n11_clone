package com.example.jwtjava.service;

import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.entity.User;
import com.example.jwtjava.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /**
     * Creates a new refresh token for the user.
     * If the user already has a refresh token, it is replaced.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing token if present
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiry))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates the refresh token: must exist, not expired, not revoked.
     * Throws RuntimeException on any failure so the controller can catch it.
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token bulunamadı."));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token iptal edilmiş.");
        }

        if (refreshToken.isExpired()) {
            // Auto-revoke expired token
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RuntimeException("Refresh token süresi dolmuş.");
        }

        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revokeByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }
}
