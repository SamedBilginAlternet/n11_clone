package com.example.jwtjava.seed;

import com.example.jwtjava.entity.Role;
import com.example.jwtjava.entity.User;
import com.example.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Seeds three demo accounts on startup (idempotent — skips if the email is
 * already present). Kept as a CommandLineRunner rather than a Flyway seed
 * because BCrypt hashes include a random salt; computing them at runtime via
 * the application's own PasswordEncoder avoids pinning a precomputed hash
 * into SQL that would drift if the encoder's cost ever changed.
 *
 * Accounts:
 *   admin@n11demo.com    / Admin123!   — ROLE_ADMIN, ROLE_USER
 *   user@n11demo.com     / User123!    — ROLE_USER (happy-path checkout demo)
 *   failuser@n11demo.com / User123!    — ROLE_USER (name contains "fail" → payment
 *                                         declines, triggers checkout-saga compensation)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensure("admin@n11demo.com", "Admin Kullanıcı", "Admin123!",
                EnumSet.of(Role.ADMIN, Role.USER));
        ensure("user@n11demo.com", "Demo Kullanıcı", "User123!",
                EnumSet.of(Role.USER));
        ensure("failuser@n11demo.com", "Fail Demo", "User123!",
                EnumSet.of(Role.USER));
    }

    private void ensure(String email, String fullName, String rawPassword, java.util.Set<Role> roles) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .roles(roles)
                .build();
        userRepository.save(user);
        log.info("Seeded demo user {} (roles={})", email, roles);
    }
}
