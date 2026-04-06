package com.medai.auth.domain;

import com.medai.shared.security.JwtClaims;
import java.time.Instant;

public record TokenValidationResult(
    boolean valid,
    JwtClaims claims,      // non-null only when valid=true
    Instant expiresAt,     // non-null only when valid=true
    String errorMessage    // non-null only when valid=false
) {

    public static TokenValidationResult valid(JwtClaims claims, Instant expiresAt) {
        return new TokenValidationResult(true, claims, expiresAt, null);
    }

    public static TokenValidationResult invalid(String errorMessage) {
        return new TokenValidationResult(false, null, null, errorMessage);
    }
}
