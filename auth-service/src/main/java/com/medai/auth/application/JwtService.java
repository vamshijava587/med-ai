package com.medai.auth.application;

import com.medai.auth.config.AuthProperties;
import com.medai.auth.domain.TokenValidationResult;
import com.medai.shared.security.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long tokenTtlSeconds;

    public JwtService(AuthProperties properties) {
        var secret = properties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters. Set auth.jwt.secret or JWT_SECRET env variable.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenTtlSeconds = properties.getJwt().getTokenTtlSeconds();
    }

    /**
     * Issue a signed HS256 JWT for the given subject (userId).
     */
    public String generateToken(String subject, List<String> roles) {
        var now = Instant.now();
        var expiry = now.plusSeconds(tokenTtlSeconds);
        return Jwts.builder()
            .subject(subject)
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Validate a token and return its claims, or a failure result.
     */
    public TokenValidationResult validate(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidationResult.invalid("Token is missing or blank");
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            var subject = claims.getSubject();
            @SuppressWarnings("unchecked")
            var roles = (List<String>) claims.getOrDefault("roles", List.of());
            var expiresAt = claims.getExpiration().toInstant();

            return TokenValidationResult.valid(new JwtClaims(subject, roles), expiresAt);
        } catch (ExpiredJwtException ex) {
            return TokenValidationResult.invalid("Token has expired");
        } catch (JwtException ex) {
            return TokenValidationResult.invalid("Token signature or format is invalid");
        } catch (Exception ex) {
            return TokenValidationResult.invalid("Token validation failed: " + ex.getMessage());
        }
    }
}
