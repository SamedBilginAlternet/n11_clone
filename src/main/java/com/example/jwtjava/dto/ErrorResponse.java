package com.example.jwtjava.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Single, consistent error envelope returned for every error in the API.
 *
 * <pre>
 * {
 *   "status":    400,
 *   "error":     "Doğrulama hatası",
 *   "path":      "/api/auth/register",
 *   "timestamp": "2026-04-21T14:00:00Z",
 *   "fields":    { "email": "geçerli bir e-posta girin" }   // only for validation errors
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String path,
        String timestamp,
        Map<String, String> fields   // null unless this is a validation error
) {
    /** Factory for simple errors. */
    public static ErrorResponse of(int status, String error, String path) {
        return new ErrorResponse(status, error, path, Instant.now().toString(), null);
    }

    /** Factory for validation errors that include field-level details. */
    public static ErrorResponse ofValidation(String path, Map<String, String> fields) {
        return new ErrorResponse(400, "Doğrulama hatası", path, Instant.now().toString(), fields);
    }
}
