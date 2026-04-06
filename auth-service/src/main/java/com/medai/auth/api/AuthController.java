package com.medai.auth.api;

import com.medai.auth.application.JwtService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Public-facing auth endpoints.
 *
 * POST /auth/token   — Generate a JWT for a given userId + roles.
 * GET  /auth/health  — Simple liveness ping (no auth required).
 *
 * In production you would front this with your own user store / password check.
 * For now it trusts the caller to supply the correct userId (suitable for
 * development, internal tools, and the Postman collection).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public Mono<AuthApiModels.TokenResponse> generateToken(
            @Valid @RequestBody AuthApiModels.TokenRequest request) {

        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank"));
        }

        var roles = (request.roles() == null || request.roles().isEmpty())
            ? List.of("USER")
            : request.roles();

        var issuedAt = Instant.now();
        var token = jwtService.generateToken(request.userId(), roles);

        // Re-validate immediately to extract expiry from the signed token
        var validation = jwtService.validate(token);

        return Mono.just(new AuthApiModels.TokenResponse(
            token,
            request.userId(),
            roles,
            issuedAt,
            validation.expiresAt()
        ));
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("auth-service is running");
    }
}
