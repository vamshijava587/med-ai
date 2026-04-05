package com.medai.orchestrator.domain;

public record LlmResponse(
    ModelProvider provider,
    String content,
    int inputTokens,
    int outputTokens,
    double estimatedCost
) {
}
