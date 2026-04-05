package com.medai.shared.evaluation;

import java.util.List;

public record EvaluationResult(
    boolean hallucinationDetected,
    double confidenceScore,
    double relevanceScore,
    String rationale,
    List<String> validationNotes
) {
    public boolean shouldFallback(double confidenceThreshold) {
        return hallucinationDetected || confidenceScore < confidenceThreshold;
    }
}
