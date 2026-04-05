package com.medai.orchestrator.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.orchestrator.application.advisor.EvaluationAdvisor;
import com.medai.orchestrator.application.advisor.MessageChatMemoryAdvisor;
import com.medai.orchestrator.application.advisor.SimpleLoggerAdvisor;
import com.medai.orchestrator.application.advisor.TokenOptimizerAdvisor;
import com.medai.orchestrator.application.persistence.PlatformJdbcRepository;
import com.medai.orchestrator.application.tool.MedicalToolRegistry;
import com.medai.orchestrator.config.AppProperties;
import com.medai.orchestrator.domain.AgentExecutionContext;
import com.medai.orchestrator.domain.AgentExecutionResult;
import com.medai.orchestrator.domain.ConversationTurn;
import com.medai.orchestrator.domain.LlmResponse;
import com.medai.orchestrator.domain.ModelProvider;
import com.medai.orchestrator.domain.ResponseDetailLevel;
import com.medai.orchestrator.domain.RoutingDecision;
import com.medai.orchestrator.domain.ToolExecutionResult;
import com.medai.orchestrator.domain.ToolLoopResponse;
import com.medai.orchestrator.domain.ValidatedExecution;
import com.medai.shared.agent.AgentType;
import com.medai.shared.api.ChatChunk;
import com.medai.shared.api.ChatRequest;
import com.medai.shared.api.ChatResponse;
import com.medai.shared.api.UserProfile;
import com.medai.shared.evaluation.EvaluationResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The core orchestration service for handling chat interactions.
 * This class coordinates the flow of a chat request from user input to an AI-generated response,
 * including routing, RAG (Retrieval-Augmented Generation), tool execution, LLM calls, and evaluation.
 */
@Service
public class ChatOrchestratorService {

    private final AppProperties properties;
    private final LlmRoutingService routingService;
    private final LlmGateway llmGateway;
    private final VectorDataClient vectorDataClient;
    private final PromptTemplateService promptTemplateService;
    private final MedicalToolRegistry medicalToolRegistry;
    private final PlatformJdbcRepository repository;
    private final TokenOptimizerAdvisor tokenOptimizerAdvisor;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final EvaluationAdvisor evaluationAdvisor;
    private final SimpleLoggerAdvisor simpleLoggerAdvisor;
    private final PlatformMetricsService platformMetricsService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for ChatOrchestratorService.
     * Uses constructor injection for its many dependencies.
     */
    public ChatOrchestratorService(AppProperties properties,
                                   LlmRoutingService routingService,
                                   LlmGateway llmGateway,
                                   VectorDataClient vectorDataClient,
                                   PromptTemplateService promptTemplateService,
                                   MedicalToolRegistry medicalToolRegistry,
                                   PlatformJdbcRepository repository,
                                   TokenOptimizerAdvisor tokenOptimizerAdvisor,
                                   MessageChatMemoryAdvisor memoryAdvisor,
                                   EvaluationAdvisor evaluationAdvisor,
                                   SimpleLoggerAdvisor simpleLoggerAdvisor,
                                   PlatformMetricsService platformMetricsService,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.routingService = routingService;
        this.llmGateway = llmGateway;
        this.vectorDataClient = vectorDataClient;
        this.promptTemplateService = promptTemplateService;
        this.medicalToolRegistry = medicalToolRegistry;
        this.repository = repository;
        this.tokenOptimizerAdvisor = tokenOptimizerAdvisor;
        this.memoryAdvisor = memoryAdvisor;
        this.evaluationAdvisor = evaluationAdvisor;
        this.simpleLoggerAdvisor = simpleLoggerAdvisor;
        this.platformMetricsService = platformMetricsService;
        this.objectMapper = objectMapper;
    }

    /**
     * Orchestrates a single chat request and returns a complete ChatResponse.
     * This method:
     * 1. Routes the message to an agent type.
     * 2. Retrieves the user's profile and conversation history.
     * 3. Saves the user's message to the database.
     * 4. Builds the execution context (including RAG).
     * 5. Executes the primary and/or fallback LLMs.
     * 6. Persists the AI's response and audit logs.
     *
     * @param request The chat request from the user.
     * @return A Mono emitting the final ChatResponse.
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        var startedAt = Instant.now();
        var routingMono = routingService.route(request.message(), request.metadata());
        var userProfileMono = Mono.fromCallable(() -> repository.findUserProfile(request.userId()).orElse(emptyProfile(request.userId())))
            .subscribeOn(Schedulers.boundedElastic());
        var historyMono = Mono.fromCallable(() -> repository.recentMessages(request.sessionId(), properties.getMemory().getWindowSize()))
            .subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(routingMono, userProfileMono, historyMono)
            .flatMap(tuple -> {
                var routingDecision = tuple.getT1();
                var userProfile = tuple.getT2();
                var history = tuple.getT3();
                return Mono.fromRunnable(() -> {
                        repository.ensureSession(request.sessionId(), request.userId());
                        repository.saveMessage(request.sessionId(), request.userId(), "USER", request.message(), estimateTokens(request.message()));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(buildContext(request, routingDecision, userProfile, history))
                    .flatMap(context -> executeWithFallbacks(request, routingDecision, context))
                    .flatMap(validated -> persistAndBuildResponse(request, validated, startedAt));
            });
    }

    /**
     * Orchestrates a chat request and returns a stream of ChatChunks.
     * It wraps the synchronous `chat()` method and simulates a stream by splitting the answer into words.
     * In a production scenario, this would ideally use the LLM's native streaming capabilities.
     *
     * @param request The chat request from the user.
     * @return A Flux emitting ChatChunk objects.
     */
    public Flux<ChatChunk> stream(ChatRequest request) {
        return chat(request)
            .flatMapMany(response -> {
                var words = response.answer().split("\\s+");
                return Flux.fromArray(words)
                    .map(word -> new ChatChunk(
                        response.agent(),
                        word + " ",
                        false,
                        response.confidenceScore(),
                        response.hallucinationDetected(),
                        Instant.now()))
                    .concatWithValues(new ChatChunk(
                        response.agent(),
                        "",
                        true,
                        response.confidenceScore(),
                        response.hallucinationDetected(),
                        Instant.now()));
            });
    }

    /**
     * Constructs the AgentExecutionContext by enriching it with RAG (Retrieval-Augmented Generation) context.
     * It searches the vector database for knowledge relevant to the user's question.
     *
     * @param request          The original chat request.
     * @param routingDecision The decision on which agent type to use.
     * @param userProfile      The profile of the user.
     * @param history          The recent conversation history.
     * @return A Mono emitting the built AgentExecutionContext.
     */
    private Mono<AgentExecutionContext> buildContext(ChatRequest request,
                                                     RoutingDecision routingDecision,
                                                     UserProfile userProfile,
                                                     List<ConversationTurn> history) {
        var estimatedPromptTokens = estimateTokens(request.message() + memoryAdvisor.format(history));
        var responseDetailLevel = tokenOptimizerAdvisor.chooseDetailLevel(estimatedPromptTokens);
        if (routingDecision.agentType() == AgentType.FALLBACK) {
            return Mono.just(new AgentExecutionContext(
                request.userId(),
                request.sessionId(),
                AgentType.FALLBACK,
                userProfile,
                history,
                List.of(),
                responseDetailLevel,
                request.metadata()));
        }
        return vectorDataClient.search(routingDecision.agentType(), request.message(), properties.getVector().getTopK(), true)
            .map(retrieved -> new AgentExecutionContext(
                request.userId(),
                request.sessionId(),
                routingDecision.agentType(),
                userProfile,
                history,
                retrieved,
                responseDetailLevel,
                request.metadata()));
    }

    /**
     * Executes the agent logic with a robust fallback mechanism.
     * It tries:
     * 1. Primary Model (Ollama).
     * 2. If primary fails confidence thresholds, it tries the Secondary Model (OpenAI) IF enabled.
     * 3. If everything fails, it uses a Fallback Agent or a static safety message.
     *
     * @param request          The original chat request.
     * @param routingDecision The agent routing decision.
     * @param context          The execution context with RAG data.
     * @return A Mono emitting the validated execution result.
     */
    private Mono<ValidatedExecution> executeWithFallbacks(ChatRequest request,
                                                          RoutingDecision routingDecision,
                                                          AgentExecutionContext context) {
        if (routingDecision.fallbackPreferred() || routingDecision.agentType() == AgentType.FALLBACK) {
            return executeAndEvaluate(AgentType.FALLBACK, request, context, preferredFallbackProvider());
        }

        return executeAndEvaluate(context.agentType(), request, context, ModelProvider.OLLAMA)
            .flatMap(primary -> {
                if (!primary.evaluation().shouldFallback(properties.getEvaluation().getConfidenceThreshold())) {
                    return Mono.just(primary);
                }
                if (!llmGateway.isAvailable(ModelProvider.OPENAI)) {
                    return Mono.just(primary);
                }
                return executeAndEvaluate(context.agentType(), request, context, ModelProvider.OPENAI)
                    .flatMap(openAiAttempt -> {
                        if (!openAiAttempt.evaluation().shouldFallback(properties.getEvaluation().getConfidenceThreshold())) {
                            return Mono.just(openAiAttempt);
                        }
                        return executeAndEvaluate(AgentType.FALLBACK, request, context, preferredFallbackProvider())
                            .onErrorResume(unused -> Mono.just(buildStaticFallback(request.message())));
                    });
            })
            .onErrorResume(primaryFailure -> {
                if (llmGateway.isAvailable(ModelProvider.OPENAI)) {
                    return executeAndEvaluate(context.agentType(), request, context, ModelProvider.OPENAI)
                        .onErrorResume(openAiFailure -> Mono.just(buildStaticFallback(request.message())));
                }
                return Mono.just(buildStaticFallback(request.message()));
            });
    }

    /**
     * Executes an agent and then evaluates the result using the EvaluationAdvisor.
     *
     * @param agentType The type of agent being executed.
     * @param request   The chat request.
     * @param originalContext The execution context.
     * @param provider  The LLM provider to use.
     * @return A Mono emitting a ValidatedExecution (result + evaluation).
     */
    private Mono<ValidatedExecution> executeAndEvaluate(AgentType agentType,
                                                        ChatRequest request,
                                                        AgentExecutionContext originalContext,
                                                        ModelProvider provider) {
        var context = new AgentExecutionContext(
            originalContext.userId(),
            originalContext.sessionId(),
            agentType,
            originalContext.userProfile(),
            originalContext.history(),
            agentType == AgentType.FALLBACK ? List.of() : originalContext.retrievedContext(),
            originalContext.responseDetailLevel(),
            originalContext.requestMetadata());
        return executeAgent(agentType, request, context, provider)
            .flatMap(execution -> evaluationAdvisor.evaluate(execution, context)
                .map(evaluation -> new ValidatedExecution(execution, evaluation)));
    }

    /**
     * Initiates the iterative agent execution loop.
     */
    private Mono<AgentExecutionResult> executeAgent(AgentType agentType,
                                                    ChatRequest request,
                                                    AgentExecutionContext context,
                                                    ModelProvider provider) {
        return executeAgentLoop(agentType, request, context, provider, 0, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Executes the agent loop which handles tool calls.
     * If the LLM requests a tool call, this method executes it and passes the output back into the next iteration.
     *
     * @param agentType    The agent type.
     * @param request      The chat request.
     * @param context      The execution context.
     * @param provider     The LLM provider.
     * @param iteration    The current loop iteration (to prevent infinite loops).
     * @param toolHistory  History of tool calls and results in this request.
     * @param toolNames    Names of tools used.
     * @return A Mono emitting the final AgentExecutionResult.
     */
    private Mono<AgentExecutionResult> executeAgentLoop(AgentType agentType,
                                                        ChatRequest request,
                                                        AgentExecutionContext context,
                                                        ModelProvider provider,
                                                        int iteration,
                                                        List<ToolExecutionResult> toolHistory,
                                                        List<String> toolNames) {
        var definition = resolveAgentDefinition(agentType);
        var systemPrompt = buildSystemPrompt(agentType, definition, context.responseDetailLevel());
        var userPrompt = buildUserPrompt(request, context, toolHistory);
        return llmGateway.complete(provider, systemPrompt, userPrompt)
            .flatMap(llmResponse -> {
                var toolLoopResponse = parseToolLoopResponse(llmResponse);
                if (!"TOOL".equalsIgnoreCase(toolLoopResponse.action())
                    || toolLoopResponse.toolCalls() == null
                    || toolLoopResponse.toolCalls().isEmpty()
                    || iteration >= properties.getTokens().getMaxToolIterations()) {
                    return Mono.just(new AgentExecutionResult(
                        agentType,
                        provider,
                        Optional.ofNullable(toolLoopResponse.answer()).filter(answer -> !answer.isBlank()).orElse(llmResponse.content()),
                        List.copyOf(toolNames),
                        List.copyOf(toolHistory),
                        llmResponse.inputTokens(),
                        llmResponse.outputTokens(),
                        llmResponse.estimatedCost(),
                        llmResponse.content()));
                }
                return medicalToolRegistry.executeToolCalls(toolLoopResponse.toolCalls(), context)
                    .flatMap(results -> {
                        var updatedHistory = new ArrayList<>(toolHistory);
                        updatedHistory.addAll(results);
                        var updatedToolNames = new ArrayList<>(toolNames);
                        updatedToolNames.addAll(results.stream().map(ToolExecutionResult::tool).toList());
                        return executeAgentLoop(agentType, request, context, provider, iteration + 1, updatedHistory, updatedToolNames);
                    });
            });
    }

    /**
     * Constructs the multi-part system prompt by concatenating base prompts, guardrails, and tool instructions.
     */
    private String buildSystemPrompt(AgentType agentType,
                                     AppProperties.AgentDefinition definition,
                                     ResponseDetailLevel detailLevel) {
        var basePrompt = promptTemplateService.loadTemplate(definition.getSystemPrompt());
        var guardrails = promptTemplateService.loadTemplate(definition.getGuardrailsPrompt());
        var toolInstructions = promptTemplateService.loadTemplate(definition.getToolInstructionsPrompt());
        var contract = promptTemplateService.loadTemplate("prompts/common/tool-loop-contract.st");
        return String.join("\n\n",
            basePrompt,
            "Response detail level: " + detailLevel.name(),
            guardrails,
            toolInstructions,
            contract,
            "Agent name: " + agentType.name());
    }

    /**
     * Constructs the user prompt by rendering a template with user profile, history, and RAG context.
     */
    private String buildUserPrompt(ChatRequest request,
                                   AgentExecutionContext context,
                                   List<ToolExecutionResult> toolHistory) {
        return promptTemplateService.render("prompts/common/agent-user.st", Map.of(
            "question", request.message(),
            "userProfile", formatUserProfile(context.userProfile()),
            "conversationHistory", memoryAdvisor.format(context.history()),
            "ragContext", formatRagContext(context),
            "toolHistory", formatToolHistory(toolHistory),
            "metadata", context.requestMetadata()
        ));
    }

    /**
     * Attempts to parse the JSON response from the LLM into a ToolLoopResponse.
     * If parsing fails (e.g., LLM returned plain text), it treats it as a FINAL answer.
     */
    private ToolLoopResponse parseToolLoopResponse(LlmResponse response) {
        try {
            return objectMapper.readValue(response.content(), ToolLoopResponse.class);
        }
        catch (Exception exception) {
            return new ToolLoopResponse("FINAL", response.content(), "Raw answer returned.", List.of());
        }
    }

    /**
     * Persists the conversation and audit data to the database and builds the final ChatResponse object.
     */
    private Mono<ChatResponse> persistAndBuildResponse(ChatRequest request,
                                                       ValidatedExecution validatedExecution,
                                                       Instant startedAt) {
        return Mono.fromRunnable(() -> {
                var execution = validatedExecution.execution();
                var evaluation = validatedExecution.evaluation();
                var latency = Duration.between(startedAt, Instant.now());
                repository.saveMessage(request.sessionId(), request.userId(), "ASSISTANT", execution.answer(), execution.outputTokens());
                repository.saveToolCalls(request.sessionId(), request.userId(), execution.agentType(), execution.toolExecutions());
                repository.saveEvaluation(request.sessionId(), request.userId(), execution.agentType(), evaluation);
                repository.saveAuditLog(
                    request.sessionId(),
                    request.userId(),
                    request.message(),
                    execution.agentType(),
                    execution.toolsUsed(),
                    execution.inputTokens() + execution.outputTokens(),
                    evaluation.confidenceScore(),
                    evaluation.hallucinationDetected(),
                    latency.toMillis());
                platformMetricsService.record(execution, evaluation);
                simpleLoggerAdvisor.logExecution(request.sessionId(), execution, evaluation, latency);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(new ChatResponse(
                request.sessionId(),
                validatedExecution.execution().agentType(),
                validatedExecution.execution().answer(),
                validatedExecution.evaluation().confidenceScore(),
                validatedExecution.evaluation().hallucinationDetected(),
                validatedExecution.execution().inputTokens(),
                validatedExecution.execution().outputTokens(),
                validatedExecution.execution().toolsUsed(),
                Duration.between(startedAt, Instant.now()),
                Instant.now()));
    }

    /**
     * Builds a static safety message used when LLM providers are failing.
     */
    private ValidatedExecution buildStaticFallback(String userQuestion) {
        var answer = """
            I cannot provide a fully grounded answer right now. Please use this as general information only, avoid making medication changes on your own, and contact a licensed clinician or emergency services if symptoms are severe or worsening.
            """.trim();
        return new ValidatedExecution(
            new AgentExecutionResult(
                AgentType.FALLBACK,
                preferredFallbackProvider(),
                answer,
                List.of(),
                List.of(),
                estimateTokens(userQuestion),
                estimateTokens(answer),
                0.0,
                "Static fallback after provider failure."),
            new EvaluationResult(
                false,
                0.40,
                0.45,
                "Static safety fallback used after orchestration failure.",
                List.of("Fallback agent response was generated without a validated grounded answer.")));
    }

    /**
     * Resolves the configured agent definition (prompts, etc.) for a given AgentType.
     */
    private AppProperties.AgentDefinition resolveAgentDefinition(AgentType agentType) {
        var definition = properties.getAgents().get(agentType.name().toLowerCase());
        if (definition == null) {
            throw new IllegalStateException("Missing agent configuration for " + agentType.name());
        }
        return definition;
    }

    /**
     * Formats the user profile data for inclusion in the prompt.
     */
    private String formatUserProfile(UserProfile userProfile) {
        return """
            User ID: %s
            Name: %s
            Allergies: %s
            Medications: %s
            Insurance: %s / %s
            Notes: %s
            """.formatted(
            userProfile.userId(),
            Optional.ofNullable(userProfile.fullName()).orElse("Unknown"),
            userProfile.allergies(),
            userProfile.medications(),
            Optional.ofNullable(userProfile.insuranceProvider()).orElse("Unknown"),
            Optional.ofNullable(userProfile.insurancePlan()).orElse("Unknown"),
            Optional.ofNullable(userProfile.notes()).orElse("None"));
    }

    /**
     * Formats the RAG context data for inclusion in the prompt.
     */
    private String formatRagContext(AgentExecutionContext context) {
        if (context.retrievedContext().isEmpty()) {
            return "No retrieved knowledge context.";
        }
        return context.retrievedContext().stream()
            .map(item -> item.rank() + ". " + item.content())
            .collect(Collectors.joining("\n"));
    }

    /**
     * Formats the tool call history for inclusion in the prompt.
     */
    private String formatToolHistory(List<ToolExecutionResult> toolHistory) {
        if (toolHistory.isEmpty()) {
            return "No tool calls have been executed yet.";
        }
        return toolHistory.stream()
            .map(result -> result.tool() + "(" + result.input() + ") => " + result.output())
            .collect(Collectors.joining("\n"));
    }

    /**
     * Provides a rough estimation of token count based on string length.
     * Assumes ~4 characters per token.
     */
    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 4.0d));
    }

    /**
     * Selects the preferred fallback LLM provider.
     */
    private ModelProvider preferredFallbackProvider() {
        return llmGateway.isAvailable(ModelProvider.OPENAI) ? ModelProvider.OPENAI : ModelProvider.OLLAMA;
    }

    /**
     * Creates an empty user profile object.
     */
    private UserProfile emptyProfile(String userId) {
        return new UserProfile(userId, "Unknown", List.of(), List.of(), null, null, null);
    }
}
