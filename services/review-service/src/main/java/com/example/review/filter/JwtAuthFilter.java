package com.example.review.filter;

import com.example.review.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads both subject (email) and the optional fullName claim. When fullName is
 * absent we derive a display-friendly prefix from the email so reviews still
 * render with a name instead of a bare address.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
        try {
            Claims claims = jwtService.extractClaims(h.substring(7));
            String email = claims.getSubject();
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(email, null, List.of()));
            }
        } catch (JwtException e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz token.");
            return;
        }
        chain.doFilter(req, res);
    }
}
