package com.medai.orchestrator.domain;

import com.medai.shared.agent.AgentType;
import java.util.List;

public record AgentExecutionResult(
    AgentType agentType,
    ModelProvider provider,
    String answer,
    List<String> toolsUsed,
    List<ToolExecutionResult> toolExecutions,
    int inputTokens,
    int outputTokens,
    double estimatedCost,
    String trace
) {
}
