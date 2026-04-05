package com.medai.shared.api;

import com.medai.shared.agent.AgentType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ChatResponse(
    String sessionId,
    AgentType agent,
    String answer,
    double confidenceScore,
    boolean hallucinationDetected,
    int inputTokens,
    int outputTokens,
    List<String> toolsUsed,
    Duration latency,
    Instant createdAt
) {
}
