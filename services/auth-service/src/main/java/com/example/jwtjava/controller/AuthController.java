package com.example.jwtjava.controller;

import com.example.jwtjava.dto.AuthResponse;
import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.entity.RefreshToken;
import com.example.jwtjava.exception.TokenException;
import com.example.jwtjava.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kayıt, giriş, token yenileme ve çıkış işlemleri")
@SecurityRequirements
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiryMs;

    @PostMapping("/register")
    @Operation(summary = "Yeni kullanıcı kaydı")
    @ApiResponse(responseCode = "201", description = "Kullanıcı oluşturuldu")
    @ApiResponse(responseCode = "409", description = "E-posta zaten kullanımda")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthService.TokenPair pair = authService.register(request);
        addRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(pair.accessToken()));
    }

    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi")
    @ApiResponse(responseCode = "200", description = "Giriş başarılı")
    @ApiResponse(responseCode = "401", description = "Kimlik bilgileri hatalı")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthService.TokenPair pair = authService.login(request);
        addRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(pair.accessToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Token yenileme")
    @ApiResponse(responseCode = "200", description = "Token yenilendi")
    @ApiResponse(responseCode = "401", description = "Refresh token geçersiz veya süresi dolmuş")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                 HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        AuthService.TokenPair pair = authService.refresh(refreshToken);
        addRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(new AuthResponse(pair.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Çıkış")
    @ApiResponse(responseCode = "204", description = "Çıkış başarılı")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true in production with HTTPS
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new TokenException("Refresh token bulunamadı.", HttpStatus.BAD_REQUEST);
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new TokenException("Refresh token bulunamadı.", HttpStatus.BAD_REQUEST));
    }
}
