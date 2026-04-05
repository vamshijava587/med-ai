package com.medai.orchestrator.application.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.orchestrator.application.LlmGateway;
import com.medai.orchestrator.application.PromptTemplateService;
import com.medai.orchestrator.domain.AgentExecutionContext;
import com.medai.orchestrator.domain.AgentExecutionResult;
import com.medai.orchestrator.domain.ModelProvider;
import com.medai.shared.evaluation.EvaluationResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EvaluationAdvisor {

    private final PromptTemplateService promptTemplateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public EvaluationAdvisor(PromptTemplateService promptTemplateService,
                             LlmGateway llmGateway,
                             ObjectMapper objectMapper) {
        this.promptTemplateService = promptTemplateService;
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    public Mono<EvaluationResult> evaluate(AgentExecutionResult execution, AgentExecutionContext context) {
        var ragContext = context.retrievedContext().stream()
            .map(item -> item.rank() + ". " + item.content())
            .collect(Collectors.joining("\n"));
        var hallucinationSystem = promptTemplateService.render("prompts/evaluation/hallucination-system.st", Map.of());
        var hallucinationUser = promptTemplateService.render("prompts/evaluation/hallucination-user.st", Map.of(
            "agent", execution.agentType().name(),
            "answer", execution.answer(),
            "ragContext", ragContext,
            "toolsUsed", execution.toolsUsed()
        ));
        var evaluationSystem = promptTemplateService.render("prompts/evaluation/evaluation-system.st", Map.of());
        var evaluationUser = promptTemplateService.render("prompts/evaluation/evaluation-user.st", Map.of(
            "agent", execution.agentType().name(),
            "answer", execution.answer(),
            "ragContext", ragContext,
            "toolsUsed", execution.toolsUsed()
        ));

        return Mono.zip(
                runHallucinationPrompt(hallucinationSystem, hallucinationUser),
                runScoringPrompt(evaluationSystem, evaluationUser))
            .map(tuple -> new EvaluationResult(
                tuple.getT1().hallucinationDetected(),
                tuple.getT2().confidenceScore(),
                tuple.getT2().relevanceScore(),
                tuple.getT2().rationale(),
                combineNotes(tuple.getT1().notes(), tuple.getT2().notes())));
    }

    private Mono<HallucinationPayload> runHallucinationPrompt(String system, String user) {
        return llmGateway.complete(selectProvider(), system, user)
            .flatMap(response -> Mono.fromCallable(() -> objectMapper.readValue(response.content(), HallucinationPayload.class)))
            .onErrorReturn(new HallucinationPayload(false, List.of("Hallucination analysis fell back to optimistic default.")));
    }

    private Mono<ScoringPayload> runScoringPrompt(String system, String user) {
        return llmGateway.complete(selectProvider(), system, user)
            .flatMap(response -> Mono.fromCallable(() -> objectMapper.readValue(response.content(), ScoringPayload.class)))
            .onErrorReturn(new ScoringPayload(0.55, 0.55, "Evaluation model unavailable; conservative defaults applied.", List.of("Evaluation fallback used.")));
    }

    private ModelProvider selectProvider() {
        return llmGateway.isAvailable(ModelProvider.OPENAI) ? ModelProvider.OPENAI : ModelProvider.OLLAMA;
    }

    private List<String> combineNotes(List<String> first, List<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private record HallucinationPayload(boolean hallucinationDetected, List<String> notes) {
    }

    private record ScoringPayload(double confidenceScore, double relevanceScore, String rationale, List<String> notes) {
    }
}
