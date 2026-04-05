package com.medai.orchestrator.domain;

public record ToolExecutionResult(
    String tool,
    String input,
    String output
) {
}
