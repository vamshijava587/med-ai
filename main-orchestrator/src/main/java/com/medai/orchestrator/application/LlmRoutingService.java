package com.medai.orchestrator.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.orchestrator.domain.ModelProvider;
import com.medai.orchestrator.domain.RoutingDecision;
import com.medai.shared.agent.AgentType;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LlmRoutingService {

    private final PromptTemplateService promptTemplateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmRoutingService(PromptTemplateService promptTemplateService,
                             LlmGateway llmGateway,
                             ObjectMapper objectMapper) {
        this.promptTemplateService = promptTemplateService;
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    public Mono<RoutingDecision> route(String message, Map<String, Object> metadata) {
        var availableAgents = java.util.Arrays.stream(AgentType.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));
        var system = promptTemplateService.render("prompts/routing/router-system.st", Map.of());
        var user = promptTemplateService.render("prompts/routing/router-user.st", Map.of(
            "message", message,
            "metadata", metadata,
            "availableAgents", availableAgents
        ));
        return llmGateway.complete(ModelProvider.OLLAMA, system, user)
            .map(response -> parse(response.content()))
            .onErrorResume(primaryFailure -> llmGateway.complete(ModelProvider.OPENAI, system, user)
                .map(response -> parse(response.content())))
            .onErrorReturn(new RoutingDecision(AgentType.FALLBACK, true, 0.10, "Routing failed, defaulting to fallback agent."));
    }

    private RoutingDecision parse(String rawContent) {
        try {
            var payload = objectMapper.readValue(rawContent, RoutingPayload.class);
            return new RoutingDecision(
                AgentType.fromString(payload.agent()),
                payload.fallbackPreferred(),
                payload.confidence(),
                payload.rationale());
        }
        catch (Exception exception) {
            return new RoutingDecision(AgentType.FALLBACK, true, 0.10, "Routing output was not parseable.");
        }
    }

    private record RoutingPayload(String agent, boolean fallbackPreferred, double confidence, String rationale) {
    }
}
