package com.medai.shared.agent;

import java.util.Arrays;

public enum AgentType {
    SYMPTOM("symptom_kb"),
    ALLERGY("allergy_kb"),
    MEDICATION("medication_kb"),
    TRIAGE("triage_kb"),
    FINANCIAL("financial_kb"),
    FALLBACK("fallback_kb");

    private final String collectionName;

    AgentType(String collectionName) {
        this.collectionName = collectionName;
    }

    public String collectionName() {
        return collectionName;
    }

    public static AgentType fromString(String candidate) {
        return Arrays.stream(values())
            .filter(value -> value.name().equalsIgnoreCase(candidate))
            .findFirst()
            .orElse(FALLBACK);
    }
}
