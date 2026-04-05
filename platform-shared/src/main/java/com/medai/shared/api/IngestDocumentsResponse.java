package com.medai.shared.api;

import com.medai.shared.agent.AgentType;

public record IngestDocumentsResponse(
    AgentType agentType,
    int chunksCreated,
    int vectorsPersisted
) {
}
