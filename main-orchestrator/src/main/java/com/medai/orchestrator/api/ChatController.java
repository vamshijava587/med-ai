package com.medai.orchestrator.api;

import com.medai.orchestrator.application.ChatOrchestratorService;
import com.medai.shared.api.ChatChunk;
import com.medai.shared.api.ChatRequest;
import com.medai.shared.api.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;

    public ChatController(ChatOrchestratorService chatOrchestratorService) {
        this.chatOrchestratorService = chatOrchestratorService;
    }

    @PostMapping
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        validatePrincipal(request, jwt);
        return chatOrchestratorService.chat(request);
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatChunk> stream(@Valid @RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        validatePrincipal(request, jwt);
        return chatOrchestratorService.stream(request);
    }

    private void validatePrincipal(ChatRequest request, Jwt jwt) {
        if (jwt == null || !request.userId().equals(jwt.getSubject())) {
            throw new ResponseStatusException(FORBIDDEN, "JWT subject must match userId.");
        }
    }
}
