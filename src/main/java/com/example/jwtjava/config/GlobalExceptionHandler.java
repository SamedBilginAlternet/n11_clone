package com.example.jwtjava.config;

import com.example.jwtjava.dto.ErrorResponse;
import com.example.jwtjava.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException ex, HttpServletRequest req) {
        return respond(ex.getStatus(), ex.getMessage(), req.getRequestURI());
    }

    // ── Spring Security ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı.", req.getRequestURI());
    }

    // ── Spring MVC ───────────────────────────────────────────────────────────

    /** Malformed or missing request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı veya eksik.", req.getRequestURI());
    }

    /** Wrong HTTP method (e.g. GET on a POST-only endpoint). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        String msg = "'" + ex.getMethod() + "' metodu bu endpoint için desteklenmiyor.";
        return respond(HttpStatus.METHOD_NOT_ALLOWED, msg, req.getRequestURI());
    }

    /** Unknown path — Spring 6.1+ throws this instead of NoHandlerFoundException. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "Endpoint bulunamadı: " + req.getRequestURI(), req.getRequestURI());
    }

    /** @Valid / @Validated failures — includes per-field details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Geçersiz değer",
                        (a, b) -> a   // keep first message if a field has multiple errors
                ));

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.ofValidation(req.getRequestURI(), fields));
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    /** Logs the real cause internally but never exposes it to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu.", req.getRequestURI());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message, path));
    }
}
