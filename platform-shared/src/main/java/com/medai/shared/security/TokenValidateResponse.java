package com.medai.shared.security;

import java.time.Instant;
import java.util.List;

/**
 * Shared response model returned by auth-service POST /internal/auth/validate.
 * Used by all services that call the auth-service to validate tokens.
 */
public record TokenValidateResponse(
    boolean valid,
    JwtClaims claims,
    Instant expiresAt,
    String errorMessage
) {}
