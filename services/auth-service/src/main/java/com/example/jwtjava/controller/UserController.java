package com.example.jwtjava.controller;

import com.example.jwtjava.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Kullanıcı profil ve yetki endpoint'leri")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Mevcut kullanıcı", description = "Bearer token ile kimliği doğrulanmış kullanıcının bilgilerini döner.")
    @ApiResponse(responseCode = "200", description = "Kullanıcı bilgileri")
    @ApiResponse(responseCode = "401", description = "Token eksik veya geçersiz")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "roles", user.getRoles()
                )))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
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
