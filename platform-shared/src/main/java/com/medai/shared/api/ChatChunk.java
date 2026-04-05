package com.medai.shared.api;

import com.medai.shared.agent.AgentType;
import java.time.Instant;

public record ChatChunk(
    AgentType agent,
    String content,
    boolean terminal,
    Double confidenceScore,
    Boolean hallucinationDetected,
    Instant createdAt
) {
}
