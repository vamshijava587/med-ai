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

@Service
public class LlmGateway {

    private final ApplicationContext applicationContext;
    private final AppProperties properties;

    public LlmGateway(ApplicationContext applicationContext, AppProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    public Mono<LlmResponse> complete(ModelProvider provider, String systemPrompt, String userPrompt) {
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

    public boolean isAvailable(ModelProvider provider) {
        return switch (provider) {
            case OLLAMA -> applicationContext.containsBean("ollamaChatModel");
            case OPENAI -> applicationContext.containsBean("openAiChatModel");
        };
    }

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

    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 4.0d));
    }

    private double estimateCost(ModelProvider provider, int inputTokens, int outputTokens) {
        return switch (provider) {
            case OLLAMA -> ((inputTokens / 1000.0d) * properties.getPricing().getOllamaInputPerThousand())
                + ((outputTokens / 1000.0d) * properties.getPricing().getOllamaOutputPerThousand());
            case OPENAI -> ((inputTokens / 1000.0d) * properties.getPricing().getOpenAiInputPerThousand())
                + ((outputTokens / 1000.0d) * properties.getPricing().getOpenAiOutputPerThousand());
        };
    }
}
