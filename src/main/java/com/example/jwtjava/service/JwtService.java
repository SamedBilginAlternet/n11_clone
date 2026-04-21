package com.example.jwtjava.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    // ------------------------------------------------------------------
    // Token generation
    // ------------------------------------------------------------------

    public String generateAccessToken(UserDetails user) {
        // Roles are stored as plain strings so they can be read back without
        // any extra deserialization logic.
        // e.g. "roles": ["ROLE_USER", "ROLE_ADMIN"]
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildToken(Map.of("roles", roles), user, accessTokenExpiry);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails user, long expiry) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getSigningKey())
                .compact();
    }

    // ------------------------------------------------------------------
    // Token validation
    // ------------------------------------------------------------------

    public boolean isTokenValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    // ------------------------------------------------------------------
    // Claims extraction
    // ------------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Reads roles directly from the token — no database call needed.
     * Returns a list of GrantedAuthority ready to set on the SecurityContext.
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        List<?> roles = extractClaim(token, claims -> claims.get("roles", List.class));
        if (roles == null) return List.of();
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.toString()))
                .toList();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
