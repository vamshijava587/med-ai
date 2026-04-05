package com.medai.shared.api;

import com.medai.shared.agent.AgentType;
import java.util.List;

public record KnowledgeSearchResponse(
    AgentType agentType,
    List<RetrievedContext> matches
) {
}
