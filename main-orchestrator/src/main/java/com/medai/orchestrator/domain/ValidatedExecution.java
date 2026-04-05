package com.medai.orchestrator.domain;

import com.medai.shared.evaluation.EvaluationResult;

public record ValidatedExecution(
    AgentExecutionResult execution,
    EvaluationResult evaluation
) {
}
