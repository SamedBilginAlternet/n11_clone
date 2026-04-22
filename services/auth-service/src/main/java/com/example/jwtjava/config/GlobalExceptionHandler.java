package com.example.jwtjava.config;

import com.example.jwtjava.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthException ex, HttpServletRequest req) {
        return respond(ex.getStatus(), ex.getMessage(), req);
    }

    // ── Spring Security ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı.", req);
    }

    // ── Spring MVC ───────────────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı veya eksik.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED,
                "'" + ex.getMethod() + "' metodu bu endpoint için desteklenmiyor.", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "Endpoint bulunamadı: " + req.getRequestURI(), req);
    }

    /** Validation failures — adds per-field errors as an extension property. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Geçersiz değer",
                        (a, b) -> a
                ));

        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Doğrulama hatası", req);
        pd.setProperty("fields", fields);
        return problemResponse(HttpStatus.BAD_REQUEST, pd);
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu.", req);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, String detail, HttpServletRequest req) {
        return problemResponse(status, build(status, detail, req));
    }

    private ProblemDetail build(HttpStatus status, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    private ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail pd) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
