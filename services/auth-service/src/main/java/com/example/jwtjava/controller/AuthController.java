package com.example.jwtjava.controller;

import com.example.jwtjava.dto.AuthResponse;
import com.example.jwtjava.dto.LoginRequest;
import com.example.jwtjava.dto.RefreshRequest;
import com.example.jwtjava.dto.RegisterRequest;
import com.example.jwtjava.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kayıt, giriş, token yenileme ve çıkış işlemleri")
@SecurityRequirements   // auth endpoints don't require a token
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Yeni kullanıcı kaydı",
               description = "E-posta, şifre ve ad ile yeni hesap oluşturur. Access + refresh token döner.")
    @ApiResponse(responseCode = "201", description = "Kullanıcı oluşturuldu")
    @ApiResponse(responseCode = "409", description = "E-posta zaten kullanımda")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi",
               description = "Geçerli kimlik bilgileriyle access + refresh token alır.")
    @ApiResponse(responseCode = "200", description = "Giriş başarılı")
    @ApiResponse(responseCode = "401", description = "Kimlik bilgileri hatalı")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Token yenileme",
               description = "Geçerli refresh token ile yeni access token alır. Eski refresh token iptal edilir (rotation).")
    @ApiResponse(responseCode = "200", description = "Token yenilendi")
    @ApiResponse(responseCode = "401", description = "Refresh token geçersiz veya süresi dolmuş")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Çıkış", description = "Refresh token'ı iptal eder.")
    @ApiResponse(responseCode = "204", description = "Çıkış başarılı")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
