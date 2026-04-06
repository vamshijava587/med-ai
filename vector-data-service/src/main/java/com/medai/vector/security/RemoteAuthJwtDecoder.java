package com.medai.vector.security;

import com.medai.shared.security.TokenValidateResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * ReactiveJwtDecoder implementation that delegates token validation
 * to the central auth-service instead of verifying locally.
 *
 * Spring Security calls decode(token) for every authenticated request.
 * We forward the token to POST /internal/auth/validate on auth-service,
 * then wrap the result as a Spring Security Jwt object.
 */
@Component
public class RemoteAuthJwtDecoder implements ReactiveJwtDecoder {

    private final WebClient authWebClient;
    private final String internalApiKey;

    public RemoteAuthJwtDecoder(
            WebClient.Builder builder,
            @Value("${auth.service.url:http://localhost:8083}") String authServiceUrl,
            @Value("${auth.service.internal-api-key:medai-internal-secret}") String internalApiKey) {
        this.authWebClient = builder.baseUrl(authServiceUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return authWebClient.post()
            .uri("/internal/auth/validate")
            .header("X-Internal-Api-Key", internalApiKey)
            .bodyValue(Map.of("token", token))
            .retrieve()
            .bodyToMono(TokenValidateResponse.class)
            .flatMap(response -> {
                if (!response.valid()) {
                    return Mono.error(new JwtException("Token invalid: " + response.errorMessage()));
                }
                var claims = response.claims();
                var now = Instant.now();
                var expiresAt = response.expiresAt() != null ? response.expiresAt() : now.plusSeconds(3600);

                var jwt = Jwt.withTokenValue(token)
                    .header("alg", "HS256")
                    .subject(claims.subject())
                    .claim("roles", claims.roles() != null ? claims.roles() : List.of())
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .build();

                return Mono.just(jwt);
            })
            .onErrorMap(ex -> !(ex instanceof JwtException),
                ex -> new JwtException("Auth-service validation failed: " + ex.getMessage()));
    }
}
