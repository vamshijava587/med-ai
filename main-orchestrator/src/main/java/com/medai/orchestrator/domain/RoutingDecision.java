package com.medai.orchestrator.domain;

import com.medai.shared.agent.AgentType;

public record RoutingDecision(
    AgentType agentType,
    boolean fallbackPreferred,
    double confidence,
    String rationale
) {
}
