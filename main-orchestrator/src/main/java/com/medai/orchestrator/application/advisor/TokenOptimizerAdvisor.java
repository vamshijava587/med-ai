package com.medai.orchestrator.application.advisor;

import com.medai.orchestrator.config.AppProperties;
import com.medai.orchestrator.domain.ResponseDetailLevel;
import org.springframework.stereotype.Component;

@Component
public class TokenOptimizerAdvisor {

    private final AppProperties properties;

    public TokenOptimizerAdvisor(AppProperties properties) {
        this.properties = properties;
    }

    public ResponseDetailLevel chooseDetailLevel(int estimatedPromptTokens) {
        if (estimatedPromptTokens >= properties.getTokens().getConciseThreshold()) {
            return ResponseDetailLevel.CONCISE;
        }
        if (estimatedPromptTokens >= properties.getTokens().getBalancedThreshold()) {
            return ResponseDetailLevel.BALANCED;
        }
        return ResponseDetailLevel.DETAILED;
    }
}
