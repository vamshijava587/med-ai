package com.medai.auth.api;

import com.medai.shared.security.JwtClaims;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

// ── Token generation ─────────────────────────────────────────────────────────

public final class AuthApiModels {

    private AuthApiModels() {}

    /** POST /auth/token */
    public record TokenRequest(
        @NotBlank String userId,
        List<String> roles          // optional, defaults to ["USER"]
    ) {}

    /** Response for POST /auth/token */
    public record TokenResponse(
        String token,
        String userId,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
    ) {}

    // ── Token validation ──────────────────────────────────────────────────────

    /** POST /internal/auth/validate  (called by other microservices) */
    public record ValidateRequest(
        @NotBlank String token
    ) {}

    /** Response for POST /internal/auth/validate */
    public record ValidateResponse(
        boolean valid,
        JwtClaims claims,       // non-null only when valid=true
        Instant expiresAt,      // non-null only when valid=true
        String errorMessage     // non-null only when valid=false
    ) {}
}
