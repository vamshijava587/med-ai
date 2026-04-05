package com.medai.orchestrator.application;

import com.medai.orchestrator.config.AppProperties;
import com.medai.orchestrator.domain.LlmResponse;
import com.medai.orchestrator.domain.ModelProvider;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Acts as a bridge between the application and various Large Language Model (LLM) providers.
 * It uses Spring AI's ChatModel abstraction to interact with models like Ollama or OpenAI.
 */
@Service
public class LlmGateway {

    private final ApplicationContext applicationContext;
    private final AppProperties properties;

    /**
     * Constructor for LlmGateway.
     *
     * @param applicationContext Used to dynamically resolve ChatModel beans from the Spring context.
     * @param properties        Application properties, including pricing information.
     */
    public LlmGateway(ApplicationContext applicationContext, AppProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    /**
     * Sends a prompt (system + user message) to the specified LLM provider and returns the response.
     *
     * @param provider     The LLM provider to use (e.g., OLLAMA, OPENAI).
     * @param systemPrompt The instruction set for the LLM.
     * @param userPrompt   The actual user query or data.
     * @return A Mono emitting an LlmResponse containing the generated text, token usage, and cost.
     */
    public Mono<LlmResponse> complete(ModelProvider provider, String systemPrompt, String userPrompt) {
        if (!isAvailable(provider)) {
            return Mono.error(new IllegalStateException("Provider " + provider + " is not enabled or available."));
        }
        return Mono.fromCallable(() -> {
                var model = resolveModel(provider);
                var prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
                var response = model.call(prompt);
                var content = response.getResult().getOutput().getText();
                var inputTokens = estimateTokens(systemPrompt + "\n" + userPrompt);
                var outputTokens = estimateTokens(content);
                return new LlmResponse(
                    provider,
                    content,
                    inputTokens,
                    outputTokens,
                    estimateCost(provider, inputTokens, outputTokens));
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Checks if a specific LLM provider is available and enabled.
     *
     * @param provider The provider to check.
     * @return true if the provider's ChatModel bean exists and (for OpenAI) if it's explicitly enabled in properties.
     */
    public boolean isAvailable(ModelProvider provider) {
        return switch (provider) {
            case OLLAMA -> applicationContext.containsBean("ollamaChatModel");
            case OPENAI -> properties.getProviders().getOpenAi().isEnabled() && applicationContext.containsBean("openAiChatModel");
        };
    }

    /**
     * Dynamically retrieves the appropriate ChatModel bean from the Spring application context.
     *
     * @param provider The provider for which to resolve the model.
     * @return The ChatModel instance.
     * @throws IllegalStateException if the bean is not found.
     */
    private ChatModel resolveModel(ModelProvider provider) {
        var beanName = switch (provider) {
            case OLLAMA -> "ollamaChatModel";
            case OPENAI -> "openAiChatModel";
        };
        if (!applicationContext.containsBean(beanName)) {
            throw new IllegalStateException("Missing chat model bean: " + beanName);
        }
        return applicationContext.getBean(beanName, ChatModel.class);
    }

    /**
     * Provides a rough estimation of token count based on string length.
     * This is a simple heuristic and not a precise tokenizer.
     */
    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 4.0d));
    }

    /**
     * Estimates the cost of an LLM call based on token usage and configured pricing per provider.
     *
     * @param provider     The LLM provider.
     * @param inputTokens  Tokens in the prompt.
     * @param outputTokens Tokens in the response.
     * @return The estimated cost in dollars.
     */
    private double estimateCost(ModelProvider provider, int inputTokens, int outputTokens) {
        return switch (provider) {
            case OLLAMA -> ((inputTokens / 1000.0d) * properties.getPricing().getOllamaInputPerThousand())
                + ((outputTokens / 1000.0d) * properties.getPricing().getOllamaOutputPerThousand());
            case OPENAI -> ((inputTokens / 1000.0d) * properties.getPricing().getOpenAiInputPerThousand())
                + ((outputTokens / 1000.0d) * properties.getPricing().getOpenAiOutputPerThousand());
        };
    }
}
