package com.example.jwtjava.controller;

import com.example.jwtjava.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Kullanıcı profil ve yetki endpoint'leri")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Mevcut kullanıcı", description = "Bearer token ile kimliği doğrulanmış kullanıcının bilgilerini döner.")
    @ApiResponse(responseCode = "200", description = "Kullanıcı bilgileri")
    @ApiResponse(responseCode = "401", description = "Token eksik veya geçersiz")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "roles", user.getRoles()
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin paneli", description = "Yalnızca ADMIN rolüne sahip kullanıcılar erişebilir.")
    @ApiResponse(responseCode = "200", description = "Erişim verildi")
    @ApiResponse(responseCode = "403", description = "Yetki yetersiz")
    public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of("message", "Admin paneline hoş geldiniz!"));
    }
}
