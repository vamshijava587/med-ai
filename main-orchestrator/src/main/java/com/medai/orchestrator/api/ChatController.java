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

/**
 * REST controller for handling chat-related API requests.
 * This class provides endpoints for both synchronous (Mono) and streaming (Flux) chat interactions.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;

    /**
     * Constructor for ChatController.
     *
     * @param chatOrchestratorService The service responsible for orchestrating the chat logic.
     */
    public ChatController(ChatOrchestratorService chatOrchestratorService) {
        this.chatOrchestratorService = chatOrchestratorService;
    }

    /**
     * Handles a standard chat request and returns a single, complete response.
     *
     * @param request The chat request containing the user's message, session ID, and metadata.
     * @return A Mono emitting a ChatResponse once the LLM and orchestration process is complete.
     */
    @PostMapping
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
       // validatePrincipal(request, jwt);
        return chatOrchestratorService.chat(request);
    }

    /**
     * Handles a chat request and returns a stream of chat chunks as they are generated.
     * This uses Server-Sent Events (SSE) to provide a "typing" effect in the UI.
     *
     * @param request The chat request containing the user's message and session ID.
     * @param jwt     The authenticated user's JWT, used for identity validation.
     * @return A Flux emitting ChatChunk objects as the response is generated.
     */
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatChunk> stream(@Valid @RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
       // validatePrincipal(request, jwt);
        return chatOrchestratorService.stream(request);
    }

    /**
     * Validates that the userId in the chat request matches the 'sub' (subject) claim in the JWT.
     * This prevents users from sending messages on behalf of other users.
     *
     * @param request The chat request to validate.
     * @param jwt     The JWT of the authenticated user.
     * @throws ResponseStatusException if the validation fails (403 Forbidden).
     */
    private void validatePrincipal(ChatRequest request, Jwt jwt) {
        if (jwt == null || !request.userId().equals(jwt.getSubject())) {
            throw new ResponseStatusException(FORBIDDEN, "JWT subject must match userId.");
        }
    }
}
