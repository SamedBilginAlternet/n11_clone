package com.example.jwtjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JwtjavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwtjavaApplication.class, args);
    }
}
