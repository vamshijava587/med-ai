package com.medai.auth.api;

import com.medai.auth.application.JwtService;
import com.medai.auth.config.AuthProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Internal endpoint — called by other microservices to validate tokens.
 *
 * Protected by a shared INTERNAL_API_KEY header so only trusted services
 * (main-orchestrator, api-gateway, vector-data-service) can call it.
 *
 * POST /internal/auth/validate
 *   Header: X-Internal-Api-Key: <INTERNAL_API_KEY>
 *   Body:   { "token": "<jwt>" }
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final JwtService jwtService;
    private final String internalApiKey;

    public InternalAuthController(JwtService jwtService, AuthProperties properties) {
        this.jwtService = jwtService;
        this.internalApiKey = properties.getInternalApiKey();
    }

    @PostMapping("/validate")
    public Mono<AuthApiModels.ValidateResponse> validate(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody AuthApiModels.ValidateRequest request) {

        if (!internalApiKey.equals(apiKey)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Missing or invalid X-Internal-Api-Key header"));
        }

        var result = jwtService.validate(request.token());

        return Mono.just(new AuthApiModels.ValidateResponse(
            result.valid(),
            result.claims(),
            result.expiresAt(),
            result.errorMessage()
        ));
    }
}
