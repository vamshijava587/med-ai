package com.medai.orchestrator.domain;

import java.util.List;

public record ToolLoopResponse(
    String action,
    String answer,
    String rationale,
    List<ToolRequest> toolCalls
) {
}
