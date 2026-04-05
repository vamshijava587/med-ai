package com.medai.shared.api;

import com.medai.shared.agent.AgentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KnowledgeSearchRequest(
    @NotNull AgentType agentType,
    @NotBlank String query,
    int topK,
    boolean reRank
) {
}
