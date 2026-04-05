package com.medai.shared.api;

import com.medai.shared.agent.AgentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record IngestDocumentsRequest(
    @NotNull AgentType agentType,
    @NotEmpty List<@Valid DocumentChunkRequest> documents
) {
}
